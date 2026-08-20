package com.drimoz.punchthemall.core.model.classes;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * Bundle of optional schema_version 2 extensions (conditions, player effects, interaction-level
 * sound/particles), kept together so {@link PtaInteraction} only grows by one field. All inert by
 * default.
 */
public record PtaExtras(
        PtaConditions conditions,
        List<PtaEffect> effects,
        SoundEvent sound,
        ParticleOptions particles
) {
    public static final PtaExtras EMPTY = new PtaExtras(PtaConditions.EMPTY, List.of(), null, null);

    public PtaExtras(PtaConditions conditions, List<PtaEffect> effects, SoundEvent sound, ParticleOptions particles) {
        this.conditions = conditions == null ? PtaConditions.EMPTY : conditions;
        this.effects = effects == null ? List.of() : effects;
        this.sound = sound;
        this.particles = particles;
    }

    public boolean hasEffects() {
        return !effects.isEmpty();
    }

    public boolean hasSound() {
        return sound != null;
    }

    public boolean hasParticles() {
        return particles != null;
    }
}
