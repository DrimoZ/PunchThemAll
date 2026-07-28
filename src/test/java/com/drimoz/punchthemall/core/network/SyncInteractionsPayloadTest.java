package com.drimoz.punchthemall.core.network;

import com.drimoz.punchthemall.core.codec.InteractionSpec;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Batching of the server → client sync.
 *
 * <p>The specs travel as NBT and the vanilla reader caps a single tag at 2 MiB, so a large pack in
 * one payload would drop the client at join with a decode error. The {@code first}/{@code last}
 * flags are what let the receiver rebuild only on a whole, consistent series.</p>
 */
class SyncInteractionsPayloadTest {

    private static InteractionSpec spec() {
        return InteractionSpec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"type\": \"left_click\"}"))
                .getOrThrow(message -> new AssertionError(message));
    }

    private static Map<ResourceLocation, InteractionSpec> specs(int count) {
        Map<ResourceLocation, InteractionSpec> specs = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            specs.put(ResourceLocation.fromNamespaceAndPath("pta_test", "interaction_" + i), spec());
        }
        return specs;
    }

    private static int totalSize(List<SyncInteractionsPayload> payloads) {
        return payloads.stream().mapToInt(payload -> payload.specs().size()).sum();
    }

    @Test
    @DisplayName("an empty set still sends one payload, so a client can be told it now has none")
    void emptySetStillSends() {
        List<SyncInteractionsPayload> payloads = SyncInteractionsPayload.split(Map.of());

        assertEquals(1, payloads.size());
        assertTrue(payloads.get(0).first());
        assertTrue(payloads.get(0).last());
        assertTrue(payloads.get(0).specs().isEmpty());
    }

    @Test
    @DisplayName("a small set fits in a single payload flagged both first and last")
    void singleBatch() {
        List<SyncInteractionsPayload> payloads = SyncInteractionsPayload.split(specs(5));

        assertEquals(1, payloads.size());
        assertTrue(payloads.get(0).first());
        assertTrue(payloads.get(0).last());
        assertEquals(5, payloads.get(0).specs().size());
    }

    @Test
    @DisplayName("exactly one batch worth is still one payload")
    void exactlyOneBatch() {
        List<SyncInteractionsPayload> payloads = SyncInteractionsPayload.split(specs(SyncInteractionsPayload.BATCH_SIZE));

        assertEquals(1, payloads.size());
        assertTrue(payloads.get(0).first());
        assertTrue(payloads.get(0).last());
    }

    @Test
    @DisplayName("a large set splits, with first on the head and last on the tail only")
    void multipleBatches() {
        int count = SyncInteractionsPayload.BATCH_SIZE * 2 + 7;
        List<SyncInteractionsPayload> payloads = SyncInteractionsPayload.split(specs(count));

        assertEquals(3, payloads.size());

        assertTrue(payloads.get(0).first());
        assertFalse(payloads.get(0).last());
        assertFalse(payloads.get(1).first());
        assertFalse(payloads.get(1).last());
        assertFalse(payloads.get(2).first());
        assertTrue(payloads.get(2).last());
    }

    @Test
    @DisplayName("no batch exceeds the batch size")
    void batchesRespectTheirSize() {
        List<SyncInteractionsPayload> payloads =
                SyncInteractionsPayload.split(specs(SyncInteractionsPayload.BATCH_SIZE * 3 + 1));

        for (SyncInteractionsPayload payload : payloads) {
            assertTrue(payload.specs().size() <= SyncInteractionsPayload.BATCH_SIZE);
            assertFalse(payload.specs().isEmpty(), "an empty middle batch would waste a packet");
        }
    }

    @Test
    @DisplayName("splitting loses nothing: every id arrives exactly once")
    void splitIsLossless() {
        Map<ResourceLocation, InteractionSpec> original = specs(SyncInteractionsPayload.BATCH_SIZE * 2 + 13);
        List<SyncInteractionsPayload> payloads = SyncInteractionsPayload.split(original);

        assertEquals(original.size(), totalSize(payloads));

        Map<ResourceLocation, InteractionSpec> reassembled = new LinkedHashMap<>();
        payloads.forEach(payload -> reassembled.putAll(payload.specs()));

        assertEquals(original.keySet(), reassembled.keySet());
        assertEquals(original, reassembled);
    }

    @Test
    @DisplayName("exactly one payload claims first and exactly one claims last")
    void flagsAreUnique() {
        List<SyncInteractionsPayload> payloads =
                SyncInteractionsPayload.split(specs(SyncInteractionsPayload.BATCH_SIZE * 4));

        assertEquals(1, payloads.stream().filter(SyncInteractionsPayload::first).count());
        assertEquals(1, payloads.stream().filter(SyncInteractionsPayload::last).count());
    }
}
