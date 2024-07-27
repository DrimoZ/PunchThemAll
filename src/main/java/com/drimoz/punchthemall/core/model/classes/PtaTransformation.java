package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.Set;

/**
 * @author drimoz
 */

public class PtaTransformation {

    // Private Properties

    private final double chance;
    private final Block block;
    private final Fluid fluid;
    private final Set<PtaStateRecord<?>> stateList;
    private final CompoundTag nbtList;
    private final SoundEvent sound;
    private final ParticleOptions particles;

    // Calculated Properties

    public boolean hasTransformation() {
        return chance > 0;
    }

    public boolean isAir() {
        return hasTransformation() && block == null && fluid == null;
    }

    public boolean isBlock() {
        return hasTransformation() && block != null && fluid == null;
    }

    public boolean isFluid() {
        return hasTransformation() && block == null && fluid != null;
    }

    public boolean hasStateList() {
        return !stateList.isEmpty();
    }

    public boolean hasNbtList() {
        return !nbtList.isEmpty();
    }

    public boolean hasSound() {
        return hasTransformation() && sound != null;
    }

    public boolean hasParticles() {
        return hasTransformation() && particles != null;
    }

    // Getters

    public double getChance() {
        return chance;
    }

    public Block getBlock() {
        return block;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public Set<PtaStateRecord<?>> getStateList() {
        return stateList;
    }

    public CompoundTag getNbtList() {
        return nbtList;
    }

    public SoundEvent getSound() {
        return sound;
    }

    public ParticleOptions getParticles() {
        return particles;
    }

    // Life cycle

    public static PtaTransformation createBlock (double chance, Block block, Set<PtaStateRecord<?>> stateList, CompoundTag nbtList, SoundEvent sound, ParticleOptions particles) {
        return new PtaTransformation(chance, block, null, stateList, nbtList, sound, particles);
    }

    public static PtaTransformation createFluid (double chance, Fluid fluid, Set<PtaStateRecord<?>> stateList, CompoundTag nbtList, SoundEvent sound, ParticleOptions particles) {
        return new PtaTransformation(chance, null, fluid, stateList, nbtList, sound, particles);
    }

    public static PtaTransformation createAir (double chance, SoundEvent sound, ParticleOptions particles) {
        return new PtaTransformation(chance, null,null,null,null, sound, particles);
    }

    protected PtaTransformation (
            double chance,
            Block block, Fluid fluid,
            Set<PtaStateRecord<?>> stateList, CompoundTag nbtList,
            SoundEvent sound, ParticleOptions particles
    ) {
        if (block != null && fluid != null)
            throw new IllegalArgumentException("Transformation must be either a Fluid or a Block.");

        this.chance = chance < 0 ? 0 : chance > 1 ? 1 : chance;
        this.sound = sound;
        this.particles = particles;

        if (block == null && fluid == null) {
            this.block = null;
            this.fluid = null;
            this.stateList = new HashSet<>();
            this.nbtList = new CompoundTag();

        }
        else {
            this.block = block;
            this.fluid = fluid;
            this.stateList = stateList == null ? new HashSet<>() : stateList;
            this.nbtList = nbtList == null ? new CompoundTag() : nbtList;
        }
    }

    // Interface



    // Interface ( Util )

    @Override
    public String toString() {
        return "PtaTransformation{" +
                "chance=" + chance +
                ", block=" + block +
                ", fluid=" + fluid +
                ", stateList=" + stateList +
                ", nbtList=" + nbtList +
                ", sound=" + sound +
                ", particles=" + particles +
                '}';
    }
}
