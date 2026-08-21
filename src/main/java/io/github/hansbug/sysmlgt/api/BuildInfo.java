package io.github.hansbug.sysmlgt.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Build and upstream provenance exposed by the CLI and Java API. */
public final class BuildInfo {
    private final Map<String, String> values;

    private BuildInfo(Map<String, String> values) {
        this.values = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static BuildInfo load() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("wrapperVersion", value("project.version", "0.1.0-SNAPSHOT"));
        values.put("wrapperCommit", value("wrapper.commit", "unknown"));
        values.put("wrapperCommitTime", value("wrapper.commit.time", "unknown"));
        values.put("pilotCommit", value("pilot.commit", "unknown"));
        values.put("pilotCommitTime", value("pilot.commit.time", "unknown"));
        values.put("pilotRevision", value("pilot.revision", "unknown"));
        values.put("pilotMetamodelTag", value("pilot.metamodel.tag", "unknown"));
        values.put("pilotBuildTag", value("pilot.build.tag", "unknown"));
        values.put("javaRuntime", System.getProperty("java.version"));
        return new BuildInfo(values);
    }

    private static String value(String key, String fallback) {
        try (InputStream input = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            Properties properties = new Properties();
            if (input != null) properties.load(input);
            return properties.getProperty(key, fallback);
        } catch (IOException ignored) {
            return fallback;
        }
    }

    public Map<String, String> values() { return values; }
}
