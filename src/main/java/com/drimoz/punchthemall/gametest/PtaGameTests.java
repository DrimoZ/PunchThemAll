package com.drimoz.punchthemall.gametest;

import com.drimoz.punchthemall.PunchThemAll;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers PunchThemAll's in-world tests.
 *
 * <p>26.1 replaced the annotated-method model with two registries: a test body, and an instance
 * saying where to run it. The body registry is frozen before a mod exists, so the bodies stay in
 * {@link PtaTestFunctions}; the instances are registered here, in code, through NeoForge's
 * {@code RegisterGameTestsEvent} — no datapack files, and nothing that has to be kept in step with
 * the Java by hand.</p>
 */
public final class PtaGameTests {

    /** One structure for all of them: a small flat platform with room around it. */
    private static final Identifier PLATFORM = Identifier.fromNamespaceAndPath(PunchThemAll.MOD_ID, "pta_platform");

    /** Generous: the applier finishes in a tick, and the rest is the framework settling. */
    private static final int MAX_TICKS = 100;

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> INSTANCE_TYPES =
            DeferredRegister.create(Registries.TEST_INSTANCE_TYPE, PunchThemAll.MOD_ID);

    static {
        INSTANCE_TYPES.register("transformation", () -> PtaTestInstance.CODEC);
    }

    private PtaGameTests() {}

    public static void register(IEventBus modEventBus) {
        INSTANCE_TYPES.register(modEventBus);
        modEventBus.addListener(PtaGameTests::onRegisterGameTests);
    }

    private static void onRegisterGameTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(PunchThemAll.MOD_ID, "transformations"));

        PtaTestFunctions.all().forEach((name, body) ->
                event.registerTest(name, new PtaTestInstance(name,
                        new TestData<>(environment, PLATFORM, MAX_TICKS, 0, true))));
    }
}
