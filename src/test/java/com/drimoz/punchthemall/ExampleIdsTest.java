package com.drimoz.punchthemall;

import com.drimoz.punchthemall.core.checker.BlockChecker;
import com.drimoz.punchthemall.core.checker.FluidChecker;
import com.drimoz.punchthemall.core.checker.ItemChecker;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.drimoz.punchthemall.core.codec.InteractionSpecResolver;
import com.drimoz.punchthemall.core.model.classes.PtaInteraction;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every registry id in the example pack actually exists.
 *
 * <p>{@code ExamplePackTest} proves the files parse, which says nothing about whether
 * {@code minecraft:block.sweet_berry_bush_pick_berries} is a real sound — it is a valid string, and
 * the codec is happy. The mod reports it at load and carries on without the sound, so the example
 * quietly demonstrates the feature slightly wrong.</p>
 *
 * <p>This was checked by hand once, by loading the pack on a server and grepping the log. The grep
 * pattern did not include "Unknown sound", so a bad sound id passed a check that was reported as
 * clean. Hence a test: a pattern I have to keep remembering is not a check.</p>
 *
 * <p>Tags are skipped. They resolve against a running server and are empty here, which is the same
 * reason {@code InteractionSpecResolverTest} matches everything by id.</p>
 */
class ExampleIdsTest {

    private static final Path INTERACTIONS =
            Path.of("examples", "punchthemall-examples", "data", "pta_examples", "pta", "interaction");

    @BeforeAll
    static void bootstrap() {
        McBootstrap.ensure();
    }

    /** One id found in one file, and which registry it has to be in. */
    private record Reference(Path file, String field, String id, Kind kind) {
        enum Kind { BLOCK, ITEM, FLUID, SOUND, EFFECT, ENCHANTMENT }

        boolean exists() {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) return false;
            return switch (kind) {
                case BLOCK -> BlockChecker.doesBlockExist(id);
                case ITEM -> ItemChecker.doesItemExist(id);
                case FLUID -> FluidChecker.doesFluidExist(id);
                case SOUND -> BuiltInRegistries.SOUND_EVENT.containsKey(parsed);
                case EFFECT -> BuiltInRegistries.MOB_EFFECT.containsKey(parsed);
                // A dynamic registry: not reachable outside a running server, so not checked here.
                case ENCHANTMENT -> true;
            };
        }

    }

    @TestFactory
    @DisplayName("every id in the example pack resolves against the game registries")
    Stream<DynamicTest> everyIdResolves() throws IOException {
        assertTrue(Files.isDirectory(INTERACTIONS), "example pack not found at " + INTERACTIONS.toAbsolutePath());

        List<Path> files;
        try (Stream<Path> paths = Files.list(INTERACTIONS)) {
            files = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }

        List<Reference> references = new ArrayList<>();
        for (Path file : files) {
            collect(JsonParser.parseString(read(file)).getAsJsonObject(), file, references);
        }

        assertTrue(references.size() > 100, "expected the pack to reference plenty of ids, found " + references.size());

        return files.stream().map(file -> DynamicTest.dynamicTest(file.getFileName().toString(), () -> {
            List<String> broken = references.stream()
                    .filter(reference -> reference.file().equals(file))
                    .filter(reference -> !reference.exists())
                    .map(reference -> reference.field() + " -> " + reference.id() + " (" + reference.kind() + ")")
                    .toList();

            if (!broken.isEmpty()) {
                fail(file.getFileName() + " references ids that do not exist: " + String.join(", ", broken));
            }
        }));
    }

    // Walking the JSON

    private static void collect(JsonObject root, Path file, List<Reference> into) {
        addId(root, "sound", Reference.Kind.SOUND, file, "sound", into);
        // `particles` takes a block id, not a particle id — the mistake this catches most often.
        addId(root, "particles", Reference.Kind.BLOCK, file, "particles", into);

        if (root.has("hand")) {
            addMatch(root.getAsJsonObject("hand"), Reference.Kind.ITEM, file, "hand.match", into);
        }
        if (root.has("target")) {
            addTarget(root.getAsJsonObject("target"), file, "target", into);
        }
        if (root.has("rewards")) {
            JsonObject rewards = root.getAsJsonObject("rewards");
            addEntries(rewards, "weighted", file, into);
            addEntries(rewards, "guaranteed", file, into);
        }
        if (root.has("effects")) {
            for (JsonElement element : root.getAsJsonArray("effects")) {
                addId(element.getAsJsonObject(), "id", Reference.Kind.EFFECT, file, "effects.id", into);
            }
        }
        if (root.has("conditions")) {
            JsonObject conditions = root.getAsJsonObject("conditions");
            if (conditions.has("neighbours")) {
                for (JsonElement element : conditions.getAsJsonArray("neighbours")) {
                    JsonObject neighbour = element.getAsJsonObject();
                    if (neighbour.has("block")) {
                        addTarget(neighbour.getAsJsonObject("block"), file, "conditions.neighbours.block", into);
                    }
                }
            }
        }
        if (root.has("transformation")) {
            for (JsonObject transformation : transformations(root.get("transformation"))) {
                addTransformation(transformation, file, into);
            }
        }
    }

    /** The field takes one object, a list of them, or a group carrying them under `all`. */
    private static List<JsonObject> transformations(JsonElement element) {
        List<JsonObject> found = new ArrayList<>();
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) found.add(entry.getAsJsonObject());
            return found;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("all")) {
            JsonElement all = object.get("all");
            if (all.isJsonArray()) {
                for (JsonElement entry : all.getAsJsonArray()) found.add(entry.getAsJsonObject());
            } else {
                found.add(all.getAsJsonObject());
            }
            return found;
        }
        found.add(object);
        return found;
    }

    private static void addTransformation(JsonObject transformation, Path file, List<Reference> into) {
        addId(transformation, "sound", Reference.Kind.SOUND, file, "transformation.sound", into);
        addId(transformation, "particles", Reference.Kind.BLOCK, file, "transformation.particles", into);

        if (transformation.has("require")) {
            addTarget(transformation.getAsJsonObject("require"), file, "transformation.require", into);
        }
        if (transformation.has("into")) {
            JsonObject target = transformation.getAsJsonObject("into");
            String kind = target.has("kind") ? target.get("kind").getAsString() : "block";
            // `copy` and `air` name nothing to look up.
            if (!kind.equals("copy") && !kind.equals("air") && target.has("id")) {
                addId(target, "id", kind.equals("fluid") ? Reference.Kind.FLUID : Reference.Kind.BLOCK,
                        file, "transformation.into.id", into);
            }
        }
    }

    private static void addTarget(JsonObject target, Path file, String field, List<Reference> into) {
        String kind = target.has("kind") ? target.get("kind").getAsString() : "block";
        if (kind.equals("air")) return;
        // `any` is deliberately either, so neither registry missing it is a failure.
        if (kind.equals("any")) return;
        addMatch(target, kind.equals("fluid") ? Reference.Kind.FLUID : Reference.Kind.BLOCK, file, field + ".match", into);
    }

    private static void addEntries(JsonObject rewards, String key, Path file, List<Reference> into) {
        if (!rewards.has(key)) return;
        for (JsonElement element : rewards.getAsJsonArray(key)) {
            addMatch(element.getAsJsonObject(), Reference.Kind.ITEM, file, "rewards." + key + ".match", into);
        }
    }

    private static void addMatch(JsonObject owner, Reference.Kind kind, Path file, String field, List<Reference> into) {
        if (!owner.has("match")) return;
        JsonElement match = owner.get("match");
        if (match.isJsonArray()) {
            JsonArray array = match.getAsJsonArray();
            for (JsonElement element : array) addSingle(element.getAsString(), kind, file, field, into);
        } else {
            addSingle(match.getAsString(), kind, file, field, into);
        }
    }

    private static void addId(JsonObject owner, String key, Reference.Kind kind, Path file, String field, List<Reference> into) {
        if (owner.has(key)) addSingle(owner.get(key).getAsString(), kind, file, field, into);
    }

    private static void addSingle(String id, Reference.Kind kind, Path file, String field, List<Reference> into) {
        if (id.startsWith("#")) return; // A tag, which needs a server to resolve.
        into.add(new Reference(file, field, id, kind));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * An example that writes a second corner really gets a region.
     *
     * <p>Anchored on the shipped files rather than on a hand-built spec, because that is where the
     * bug showed: {@code combo_excavator_3x3} advertised nine blocks and broke one, and every layer
     * on its own was correct. The codec parsed the corner, the offset maths handled boxes, the game
     * tests built one directly — only the resolver between them dropped it, and no test crossed that
     * seam.</p>
     */
    @TestFactory
    @DisplayName("every example that asks for a region gets one")
    Stream<DynamicTest> regionsSurviveResolution() throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.list(INTERACTIONS)) {
            files = paths.filter(path -> path.toString().endsWith(".json")).sorted().toList();
        }

        List<Path> withRegions = files.stream()
                .filter(file -> read(file).replaceAll("\s+", "").contains("\"to\":{"))
                .toList();

        assertTrue(!withRegions.isEmpty(), "no example uses a region, so this check proves nothing");

        return withRegions.stream().map(file -> DynamicTest.dynamicTest(file.getFileName().toString(), () -> {
            InteractionSpec spec = InteractionSpec.CODEC
                    .parse(JsonOps.INSTANCE, JsonParser.parseString(read(file)))
                    .getOrThrow(message -> new AssertionError(message));
            PtaInteraction interaction = InteractionSpecResolver.resolve(
                    ResourceLocation.fromNamespaceAndPath("pta_examples", "region_check"), spec, null);

            assertTrue(interaction != null, file.getFileName() + " did not resolve at all");

            boolean anyRegion = interaction.getTransformations().stream()
                    .anyMatch(transformation -> transformation.getOffset().isRegion());
            if (!anyRegion) {
                fail(file.getFileName() + " writes a `to` corner but resolves to a single block."
                        + " A region that collapses silently is worse than one that errors.");
            }
        }));
    }
}
