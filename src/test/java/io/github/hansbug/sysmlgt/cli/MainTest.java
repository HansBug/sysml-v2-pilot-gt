package io.github.hansbug.sysmlgt.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class MainTest {
    @Test void exposesVersionProvenance() {
        Invocation invocation = invoke("-v");

        assertEquals(0, invocation.exitCode());
        assertTrue(invocation.stdout().contains("wrapperVersion="));
        assertTrue(invocation.stdout().contains("pilotCommit="));
    }

    @Test void parsesJsonInMinimalAndFullModes() {
        Invocation minimal = invoke("parse", "--node", "ownedExpression", "--text", "1 + 2 * 3", "--format", "json");
        Invocation full = invoke("parse", "--node", "ownedExpression", "--text", "1 + 2 * 3", "--tree", "full", "--format", "json");

        assertEquals(0, minimal.exitCode());
        assertTrue(minimal.stdout().contains("\"treeMode\" : \"minimal\""));
        assertTrue(minimal.stdout().contains("Action(operand +=)"));
        assertEquals(0, full.exitCode());
        assertTrue(full.stdout().contains("\"treeMode\" : \"full\""));
        assertTrue(full.stdout().contains("RuleCall(ConditionalExpression)"));
    }

    @Test void listsOfficialEntriesAsJson() {
        Invocation invocation = invoke("list", "--format", "json");

        assertEquals(0, invocation.exitCode());
        assertTrue(invocation.stdout().contains("rootNamespace"));
        assertTrue(invocation.stdout().contains("stateDefinition"));
    }

    private static Invocation invoke(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));
            int exitCode = new CommandLine(new Main()).execute(args);
            return new Invocation(exitCode, text(stdout), text(stderr));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static String text(ByteArrayOutputStream output) {
        return output.toString(StandardCharsets.UTF_8);
    }

    private record Invocation(int exitCode, String stdout, String stderr) {}
}
