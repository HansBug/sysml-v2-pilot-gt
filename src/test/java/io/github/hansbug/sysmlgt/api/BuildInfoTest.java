package io.github.hansbug.sysmlgt.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BuildInfoTest {
    @Test void exposesWrapperAndPilotKeys() {
        var values = BuildInfo.load().values();
        assertTrue(values.containsKey("wrapperVersion"));
        assertTrue(values.containsKey("pilotCommit"));
    }
}
