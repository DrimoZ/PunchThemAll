package com.drimoz.punchthemall.gametest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * One of PunchThemAll's in-world tests, addressed by name.
 *
 * <p>26.1 replaced the annotated-method model with registry entries: a test is a
 * {@code Consumer<GameTestHelper>} in {@code Registries.TEST_FUNCTION} and an instance describing
 * where to run it. That function registry is built during {@code BuiltInRegistries} class
 * initialisation and frozen long before a mod is constructed, so a mod cannot put anything in it.</p>
 *
 * <p>So the test body is not held in a registry at all. This instance carries the name of one, and
 * looks it up in {@link PtaTestFunctions} when it runs. The name is also all the codec needs to
 * carry, which keeps the instance serialisable without pretending a lambda can be written to
 * disk.</p>
 */
public class PtaTestInstance extends GameTestInstance {

    public static final MapCodec<PtaTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("test").forGetter(PtaTestInstance::name),
            TestData.CODEC.forGetter(PtaTestInstance::info)
    ).apply(instance, PtaTestInstance::new));

    private final Identifier name;

    public PtaTestInstance(Identifier name, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.name = name;
    }

    public Identifier name() {
        return name;
    }

    @Override
    public void run(GameTestHelper helper) {
        PtaTestFunctions.get(name).accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("PunchThemAll transformation test");
    }
}
