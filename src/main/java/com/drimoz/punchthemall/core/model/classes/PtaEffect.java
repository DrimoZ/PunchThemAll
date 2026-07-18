package com.drimoz.punchthemall.core.model.classes;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A potion effect optionally applied to the player on a successful interaction (§5.8).
 */
public record PtaEffect(MobEffect effect, int duration, int amplifier, double chance) {

    public PtaEffect(MobEffect effect, int duration, int amplifier, double chance) {
        this.effect = effect;
        this.duration = Math.max(1, duration);
        this.amplifier = Math.max(0, amplifier);
        this.chance = chance < 0 ? 0 : Math.min(chance, 1);
    }

    public boolean shouldApply() {
        return effect != null && ThreadLocalRandom.current().nextDouble() <= chance;
    }

    public MobEffectInstance toInstance() {
        return new MobEffectInstance(effect, duration, amplifier);
    }
}
