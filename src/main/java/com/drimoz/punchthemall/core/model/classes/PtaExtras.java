package com.drimoz.punchthemall.core.model.classes;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

/**
 * Bundle of optional schema_version 2 extensions attached to an interaction, kept together so the
 * {@link PtaInteraction} constructor only grows by one parameter. All fields default to inert, so
 * interactions that do not use them behave exactly as before.
 *
 * <ul>
 *     <li>{@code conditions} — environmental/player gating (§5.7)</li>
 *     <li>{@code effects} — potion effects applied to the player on success (§5.8)</li>
 *     <li>{@code sound}/{@code particles} — feedback played on the interaction itself (not only on
 *     the transformation)</li>
 * </ul>
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
