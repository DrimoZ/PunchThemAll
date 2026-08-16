package com.drimoz.punchthemall;

import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every shipped example parses.
 *
 * <p>The example pack is the documentation people copy from, so a typo in it is a typo in the docs.
 * Nothing else checks these files short of loading a world with the pack installed, which is exactly
 * the loop this is meant to shorten.</p>
 */
class ExamplePackTest {

    private static final Path INTERACTIONS =
            Path.of("examples", "punchthemall-examples", "data", "pta_examples", "pta", "interaction");

    @TestFactory
    @DisplayName("every example interaction parses as a schema_version 2 spec")
    Stream<DynamicTest> examplesParse() throws IOException {
        assertTrue(Files.isDirectory(INTERACTIONS), "example pack not found at " + INTERACTIONS.toAbsolutePath());

        List<Path> files;
        try (Stream<Path> paths = Files.list(INTERACTIONS)) {
            files = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }

        assertTrue(files.size() > 1, "expected the example pack to hold several interactions");

        return files.stream().map(file -> DynamicTest.dynamicTest(file.getFileName().toString(), () -> {
            String json = read(file);
            DataResult<InteractionSpec> result = InteractionSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));

            InteractionSpec spec = result.resultOrPartial(message -> {}).orElse(null);
            if (result.error().isPresent()) {
                fail(file.getFileName() + " - " + result.error().get().message());
            }
            assertTrue(spec != null && spec.schemaVersion() >= 2, file.getFileName() + " - not a schema_version 2 file");
        }));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
