package io.github.hansbug.sysmlgt.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class NodeTreeTest {
    @Test void keepsSourceOffsetsAndChildren() {
        PilotParser.ParseResult result = PilotParser.create().parse("package Demo { }");
        NodeTree.TreeNode tree = NodeTree.from(result);
        assertEquals(0, tree.offset());
        assertEquals(result.source().length(), tree.endOffset());
        assertNotNull(tree.children());
    }
}
