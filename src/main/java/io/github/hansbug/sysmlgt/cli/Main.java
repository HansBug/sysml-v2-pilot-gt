package io.github.hansbug.sysmlgt.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.hansbug.sysmlgt.api.BuildInfo;
import io.github.hansbug.sysmlgt.api.NodeTree;
import io.github.hansbug.sysmlgt.api.PilotParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "sysml-gt", mixinStandardHelpOptions = true, versionProvider = Main.Version.class,
        description = "Parse SysML v2 with the official Pilot parser.", subcommands = {Main.Parse.class, Main.ListEntries.class})
public final class Main implements Callable<Integer> {
    public static void main(String[] args) {
        CommandLine command = new CommandLine(new Main());
        command.setExecutionExceptionHandler((error, line, parseResult) -> {
            line.getErr().println("error: " + (error.getMessage() == null ? error : error.getMessage()));
            return error instanceof CommandLine.ParameterException ? 2 : 3;
        });
        int exit = command.execute(args);
        System.exit(exit);
    }

    @Option(names = "-v", description = "Show version and upstream provenance")
    private boolean shortVersion;

    @Override public Integer call() {
        if (shortVersion) {
            for (String line : new Version().getVersion()) System.out.println(line);
            return 0;
        }
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static final class Version implements CommandLine.IVersionProvider {
        @Override public String[] getVersion() {
            return BuildInfo.load().values().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
        }
    }

    @Command(name = "parse", description = "Parse text, a file, or stdin.", mixinStandardHelpOptions = true)
    static final class Parse implements Callable<Integer> {
        @Option(names = {"-t", "--text"}, description = "SysML source text") String text;
        @Option(names = {"-f", "--file"}, description = "Read SysML source from a file") Path file;
        @Option(names = {"-n", "--node"}, defaultValue = PilotParser.DEFAULT_ENTRY,
                description = "Official grammar node (default: ${DEFAULT-VALUE})") String node;
        @Option(names = {"--format"}, defaultValue = "pretty",
                description = "Output: pretty, json, semantic-json (default: ${DEFAULT-VALUE})") String format;
        @Option(names = "--tree", defaultValue = "minimal",
                description = "NodeModel tree: minimal or full (default: ${DEFAULT-VALUE})") String tree;

        @Override public Integer call() throws Exception {
            String source = readSource();
            NodeTree.Mode treeMode;
            try {
                treeMode = NodeTree.Mode.parse(tree);
            } catch (IllegalArgumentException error) {
                throw new CommandLine.ParameterException(new CommandLine(this), error.getMessage());
            }
            PilotParser parser = PilotParser.create();
            PilotParser.ParseResult result;
            try {
                result = parser.parse(source, node);
            } catch (IllegalArgumentException error) {
                throw new CommandLine.ParameterException(new CommandLine(this), error.getMessage());
            }
            if ("json".equals(format) || "semantic-json".equals(format)) {
                Map<String, Object> output = new LinkedHashMap<>();
                output.put("schemaVersion", 1);
                output.put("entryRule", result.entryRule());
                output.put("ok", result.success());
                output.put("diagnostics", result.diagnostics());
                output.put("treeMode", treeMode.name().toLowerCase(java.util.Locale.ROOT));
                output.put("tree", NodeTree.from(result, treeMode));
                if ("semantic-json".equals(format)) {
                    output.put("semantic", io.github.hansbug.sysmlgt.api.SemanticTree.from(result));
                    output.put("semanticType", io.github.hansbug.sysmlgt.api.SemanticTree.typeName(result.semanticRoot()));
                }
                System.out.println(json(output));
            } else if ("pretty".equals(format)) {
                printPretty(result, treeMode);
            } else {
                throw new CommandLine.ParameterException(new CommandLine(this), "unknown format: " + format);
            }
            return result.success() ? 0 : 1;
        }

        private String readSource() throws IOException {
            if (text != null && file != null) throw new CommandLine.ParameterException(new CommandLine(this), "use only one of --text or --file");
            if (text != null) return text;
            if (file != null) {
                try {
                    return Files.readString(file);
                } catch (IOException error) {
                    throw new CommandLine.ParameterException(new CommandLine(this), "cannot read --file '" + file + "'");
                }
            }
            return new String(System.in.readAllBytes());
        }
    }

    @Command(name = "list", description = "List official parser grammar nodes.", mixinStandardHelpOptions = true)
    static final class ListEntries implements Callable<Integer> {
        @Option(names = "--format", defaultValue = "pretty", description = "Output: pretty or json") String format;

        @Override public Integer call() throws Exception {
            PilotParser parser = PilotParser.create();
            if ("json".equals(format)) {
                System.out.println(json(parser.entries()));
            } else if ("pretty".equals(format)) {
                System.out.println("Available official parser nodes:");
                for (PilotParser.EntryInfo entry : parser.entries()) {
                    System.out.printf("  %-42s %s%n", entry.name(), entry.type() == null ? "" : entry.type());
                }
                System.out.println("\nExample: sysml-gt parse --node stateDefinition --file state.sysml");
            } else {
                throw new CommandLine.ParameterException(new CommandLine(this), "unknown format: " + format);
            }
            return 0;
        }
    }

    private static String json(Object value) throws IOException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        return mapper.writeValueAsString(value);
    }

    private static void printPretty(PilotParser.ParseResult result, NodeTree.Mode treeMode) {
        System.out.printf("entry=%s ok=%s tree=%s%n", result.entryRule(), result.success(),
                treeMode.name().toLowerCase(java.util.Locale.ROOT));
        printNode(NodeTree.from(result, treeMode), 0);
        for (PilotParser.Diagnostic diagnostic : result.diagnostics()) {
            System.err.printf("error[%d:%d]: %s%n", diagnostic.offset(), diagnostic.endOffset(), diagnostic.message());
        }
    }

    private static void printNode(NodeTree.TreeNode node, int depth) {
        String text = node.text() == null ? "" : node.text().replace("\n", "\\n").replace("\r", "\\r");
        if (text.length() > 100) text = text.substring(0, 97) + "...";
        System.out.printf("%s%s [%d,%d] %s text=\"%s\"%n", "  ".repeat(depth), node.kind(), node.offset(), node.endOffset(),
                node.grammarElement() == null ? "" : node.grammarElement(), text);
        if (node.children() != null) for (NodeTree.TreeNode child : node.children()) printNode(child, depth + 1);
    }
}
