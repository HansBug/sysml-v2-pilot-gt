package io.github.hansbug.sysmlgt.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PilotParserTest {
    @Test void parsesDefaultRootNamespace() {
        PilotParser.ParseResult result = PilotParser.create().parse("package Demo { }");
        assertTrue(result.success(), result.diagnostics()::toString);
        assertNotNull(result.rootNode());
        assertNotNull(result.semanticRoot());
        assertTrue(result.semanticRoot().eClass().getName().equals("Namespace"));
    }

    @Test void parsesAFragmentByOfficialEntryName() {
        PilotParser.ParseResult result = PilotParser.create().parse("state def S;", "stateDefinition");
        assertTrue(result.success(), result.diagnostics()::toString);
        assertTrue(result.entryRule().equals("stateDefinition"));
        assertTrue(result.semanticRoot().eClass().getName().equals("StateDefinition"));
    }

    @Test void parsesOperatorExpressionsWithOfficialDelegates() {
        PilotParser.ParseResult result = PilotParser.create().parse("a + b * (c - d)", "ownedExpression");
        assertTrue(result.success(), result.diagnostics()::toString);
        assertNotNull(result.semanticRoot());
        assertTrue(result.semanticRoot().eClass().getName().equals("OperatorExpression"));
    }

    @Test void reportsInvalidSyntax() {
        PilotParser.ParseResult result = PilotParser.create().parse("package {");
        assertFalse(result.success());
        assertFalse(result.diagnostics().isEmpty());
    }

    @Test void listsOfficialParserRules() {
        var parser = PilotParser.create();
        assertTrue(parser.availableEntries().contains("rootNamespace"));
        assertTrue(parser.availableEntries().contains("stateDefinition"));
        assertTrue(parser.entries().stream().anyMatch(entry -> entry.name().equals("stateDefinition")
                && "StateDefinition".equals(entry.type())));
    }
}
