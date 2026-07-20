package com.drimoz.punchthemall.core.model.classes;

import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * A potion effect optionally applied to the player on a successful interaction (§5.8). In 1.21 a
 * {@link MobEffectInstance} is built from a {@link Holder} of the effect.
 */
public record PtaEffect(Holder<MobEffect> effect, int duration, int amplifier, double chance) {

    public PtaEffect(Holder<MobEffect> effect, int duration, int amplifier, double chance) {
        this.effect = effect;
        this.duration = Math.max(1, duration);
        this.amplifier = Math.max(0, amplifier);
        this.chance = chance < 0 ? 0 : Math.min(chance, 1);
    }

    public boolean shouldApply(RandomSource random) {
        return effect != null && random.nextDouble() <= chance;
    }

    public MobEffectInstance toInstance() {
        return new MobEffectInstance(effect, duration, amplifier);
    }
}
