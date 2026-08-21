package io.github.hansbug.sysmlgt.api;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.parser.IParser;
import org.omg.sysml.lang.sysml.SysMLPackage;
import org.omg.sysml.logic.SysMLLogicStandaloneSetup;
import org.omg.sysml.xtext.SysMLStandaloneSetup;
import org.omg.sysml.xtext.services.SysMLGrammarAccess;
import com.google.inject.Injector;

/** Thin, official-Pilot-backed parser API. */
public final class PilotParser {
    public static final String DEFAULT_ENTRY = "rootNamespace";
    private final SysMLGrammarAccess grammar;
    private final IParser parser;

    private PilotParser(Injector injector) {
        this.grammar = injector.getInstance(SysMLGrammarAccess.class);
        this.parser = injector.getInstance(IParser.class);
    }

    public static PilotParser create() {
        // The generated SysML model uses the upstream SysML setting and
        // invocation delegates for derived expression features. The official
        // KerML/interactive entry points install these delegates before
        // creating model instances; do the same for standalone parsing.
        SysMLLogicStandaloneSetup.doSetup();
        // The official standalone setup registers Xtext services but does not
        // eagerly initialize the generated SysML EPackage. Initialize it first
        // so parse results contain the official generated semantic objects.
        SysMLPackage.eINSTANCE.eClass();
        return new PilotParser(new SysMLStandaloneSetup().createInjectorAndDoEMFRegistration());
    }

    public ParseResult parse(String source) {
        return parse(source, DEFAULT_ENTRY);
    }

    public ParseResult parse(String source, String entryRule) {
        Objects.requireNonNull(source, "source");
        String entry = normalizeEntry(entryRule);
        ParserRule rule = parserRule(entry);
        try {
            IParseResult result = parser.parse(rule, new StringReader(source));
            List<Diagnostic> diagnostics = new ArrayList<>();
            for (var node : result.getSyntaxErrors()) {
                diagnostics.add(new Diagnostic(
                        node.getSyntaxErrorMessage() == null
                                ? "syntax error" : node.getSyntaxErrorMessage().getMessage(),
                        node.getTotalOffset(), node.getTotalEndOffset()));
            }
            return new ParseResult(source, entry, result, diagnostics);
        } catch (RuntimeException error) {
            throw new IllegalStateException("Official Pilot parser failed for entry '" + entry + "'", error);
        }
    }

    /** Return every callable, non-fragment parser rule from the official grammar. */
    public List<EntryInfo> entries() {
        return GrammarUtil.allParserRules(grammar.getGrammar()).stream()
                .filter(rule -> !rule.isFragment())
                .map(rule -> new EntryInfo(normalizeEntry(rule.getName()), ruleType(rule)))
                .distinct()
                .sorted(Comparator.comparing(EntryInfo::name))
                .collect(Collectors.toUnmodifiableList());
    }

    private ParserRule parserRule(String entry) {
        return GrammarUtil.allParserRules(grammar.getGrammar()).stream()
                .filter(rule -> !rule.isFragment())
                .filter(rule -> normalizeEntry(rule.getName()).equals(entry))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown grammar node '" + entry
                        + "'. Use 'list' to see parser entries."));
    }

    private static String ruleType(ParserRule rule) {
        return rule.getType() == null ? null : GrammarUtil.getTypeRefName(rule.getType());
    }

    public List<String> availableEntries() {
        return entries().stream().map(EntryInfo::name).collect(Collectors.toUnmodifiableList());
    }

    public static String normalizeEntry(String entryRule) {
        if (entryRule == null || entryRule.isBlank()) return DEFAULT_ENTRY;
        String value = entryRule.trim();
        if (value.startsWith("entryRule")) value = value.substring("entryRule".length());
        if (value.startsWith("rule")) value = value.substring("rule".length());
        if (value.isEmpty()) return DEFAULT_ENTRY;
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    public record EntryInfo(String name, String type) {}

    public record Diagnostic(String message, int offset, int endOffset) {}

    public static final class ParseResult {
        private final String source;
        private final String entryRule;
        private final IParseResult officialResult;
        private final List<Diagnostic> diagnostics;

        private ParseResult(String source, String entryRule, IParseResult officialResult,
                            List<Diagnostic> diagnostics) {
            this.source = source;
            this.entryRule = entryRule;
            this.officialResult = officialResult;
            this.diagnostics = List.copyOf(diagnostics);
        }

        public String source() { return source; }
        public String entryRule() { return entryRule; }
        public IParseResult officialResult() { return officialResult; }
        public ICompositeNode rootNode() { return officialResult.getRootNode(); }
        public EObject semanticRoot() { return officialResult.getRootASTElement(); }
        public List<Diagnostic> diagnostics() { return diagnostics; }
        public boolean success() { return !officialResult.hasSyntaxErrors(); }
    }
}
