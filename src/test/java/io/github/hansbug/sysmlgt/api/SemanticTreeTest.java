package io.github.hansbug.sysmlgt.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class SemanticTreeTest {
    @Test void exposesOfficialSemanticFeaturesAndContainment() {
        var result = PilotParser.create().parse("package Demo { state def S; }");
        var tree = SemanticTree.from(result);
        assertEquals("Namespace", tree.type());
        assertFalse(tree.children().isEmpty());
        assertEquals("ownedRelationship", tree.children().get(0).feature());
    }
}
