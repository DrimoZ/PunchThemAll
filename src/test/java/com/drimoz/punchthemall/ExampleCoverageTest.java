package com.drimoz.punchthemall;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every feature of the format has a runnable example.
 *
 * <p>The catalogue is the thing people learn from, so a field with no example is a field that only
 * exists in the reference — documented, and undemonstrated. That gap is invisible by inspection once
 * there are sixty files, and it opens every single time a feature is added, because writing the
 * example is the step that feels optional.</p>
 *
 * <p>This checks the pack, not the parser: {@code ExamplePackTest} already proves the files are
 * valid. What is asserted here is only that somebody could find each feature by reading them.</p>
 */
class ExampleCoverageTest {

    private static final Path INTERACTIONS =
            Path.of("examples", "punchthemall-examples", "data", "pta_examples", "pta", "interaction");

    /**
     * Feature -> a snippet that only appears in a file using it.
     *
     * <p>Deliberately matched as text rather than parsed. A structural check would follow the codec
     * and so would silently agree with it; the point here is to look at the files the way a person
     * reading them would.</p>
     */
    private static Map<String, String> features() {
        Map<String, String> features = new LinkedHashMap<>();

        features.put("left_click", "\"left_click\"");
        features.put("right_click", "\"right_click\"");
        features.put("shift_left_click", "\"shift_left_click\"");
        features.put("shift_right_click", "\"shift_right_click\"");
        features.put("enabled: false", "\"enabled\": false");
        features.put("hidden", "\"hidden\": true");

        features.put("hand.hand off", "\"hand\": \"off\"");
        features.put("hand.match tag", "\"match\": \"#minecraft:");
        features.put("consume durability", "\"mode\": \"durability\"");
        features.put("consume shrink", "\"mode\": \"shrink\"");
        features.put("consume chance", "\"chance\": 0.25");
        features.put("consume count range", "\"count\": { \"min\":");
        features.put("hand nbt whitelist", "\"whitelist\": \"{");
        features.put("hand nbt_predicates", "\"nbt_predicates\"");
        features.put("nbt predicate where", "\"where\"");

        features.put("target fluid", "\"kind\": \"fluid\"");
        features.put("target air", "\"kind\": \"air\"");
        features.put("target any", "\"kind\": \"any\"");
        features.put("target state whitelist", "\"state\": { \"whitelist\"");
        features.put("target state blacklist", "\"blacklist\": {");

        features.put("rewards weighted", "\"weighted\"");
        features.put("rewards guaranteed", "\"guaranteed\"");
        features.put("rewards rolls", "\"rolls\"");
        features.put("rewards fortune", "\"fortune\"");
        features.put("rewards at", "\"at\": { \"y\": 1 },\n    \"guaranteed\"");
        features.put("drop nbt", "\"nbt\": \"{Damage");

        features.put("transformation into block", "\"into\": { \"kind\": \"block\"");
        features.put("transformation into fluid", "\"into\": { \"kind\": \"fluid\"");
        features.put("transformation copy_state_value", "copy_state_value");
        features.put("transformation block-entity nbt", "\"nbt\": \"{CustomName");
        features.put("op break", "\"op\": \"break\"");
        features.put("op place", "\"op\": \"place\"");
        features.put("op replace", "\"op\": \"replace\"");
        features.put("offset world frame", "\"at\": { \"x\": 1, \"y\": 0, \"z\": -2 }");
        features.put("offset player frame", "\"relative_to\": \"player\"");
        features.put("offset face frame", "\"relative_to\": \"face\"");
        features.put("region (at.to)", "\"to\": {");
        features.put("transformation require", "\"require\"");
        features.put("transformation list", "\"transformation\": [");
        features.put("transformation group chance", "\"all\": [");
        features.put("into copy", "\"kind\": \"copy\"");
        features.put("drops tool", "\"drops\": \"tool\"");
        features.put("drops false", "\"drops\": false");

        features.put("costs damage", "\"damage\"");
        features.put("costs hunger", "\"hunger\"");
        features.put("effects", "\"effects\"");
        features.put("sound", "\"sound\"");
        features.put("particles", "\"particles\"");

        features.put("conditions biomes", "\"biomes\"");
        features.put("conditions time", "\"time\"");
        features.put("conditions weather", "\"weather\"");
        features.put("conditions y_range", "\"y_range\"");
        features.put("conditions light", "\"light\"");
        features.put("conditions player_state", "\"player_state\"");
        features.put("conditions neighbours", "\"neighbours\"");
        features.put("neighbour invert", "\"invert\": true");
        features.put("neoforge:conditions", "neoforge:mod_loaded");

        return features;
    }

    @TestFactory
    @DisplayName("every feature of the format is demonstrated by at least one example")
    Stream<DynamicTest> everyFeatureHasAnExample() throws IOException {
        assertTrue(Files.isDirectory(INTERACTIONS), "example pack not found at " + INTERACTIONS.toAbsolutePath());

        List<Path> files;
        try (Stream<Path> paths = Files.list(INTERACTIONS)) {
            files = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }

        Map<Path, String> contents = new LinkedHashMap<>();
        for (Path file : files) {
            contents.put(file, normalise(read(file)));
        }

        return features().entrySet().stream().map(feature -> DynamicTest.dynamicTest(feature.getKey(), () -> {
            String needle = normalise(feature.getValue());
            boolean found = contents.values().stream().anyMatch(body -> body.contains(needle));
            if (!found) {
                fail("no example demonstrates " + feature.getKey()
                        + " (looked for " + feature.getValue().replace("\n", "\\n") + ")."
                        + " Add one to the example pack and catalogue it in its README.");
            }
        }));
    }

    /** Whitespace between JSON tokens is the author's taste, not part of the feature. */
    private static String normalise(String json) {
        return json.replaceAll("\\s+", " ");
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
