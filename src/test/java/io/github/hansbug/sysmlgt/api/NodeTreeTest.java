package io.github.hansbug.sysmlgt.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NodeTreeTest {
    @Test void keepsSourceOffsetsAndChildren() {
        PilotParser.ParseResult result = PilotParser.create().parse("package Demo { }");
        NodeTree.TreeNode tree = NodeTree.from(result);
        assertEquals(0, tree.offset());
        assertEquals(result.source().length(), tree.endOffset());
        assertNotNull(tree.children());
    }

    @Test void defaultsToMinimalAndOffersTheFullOfficialTree() {
        PilotParser.ParseResult result = PilotParser.create().parse("1 + 2 * 3", "ownedExpression");
        NodeTree.TreeNode minimal = NodeTree.from(result);
        NodeTree.TreeNode full = NodeTree.fullFrom(result);
        assertTrue(count(minimal) < count(full));
        assertTrue(containsGrammar(minimal, "Action(operand +=)"));
        assertTrue(containsGrammar(full, "RuleCall(ConditionalExpression)"));
    }

    private static int count(NodeTree.TreeNode node) {
        return 1 + (node.children() == null ? 0 : node.children().stream().mapToInt(NodeTreeTest::count).sum());
    }

    private static boolean containsGrammar(NodeTree.TreeNode node, String grammar) {
        if (grammar.equals(node.grammarElement())) return true;
        return node.children() != null && node.children().stream().anyMatch(child -> containsGrammar(child, grammar));
    }
}
