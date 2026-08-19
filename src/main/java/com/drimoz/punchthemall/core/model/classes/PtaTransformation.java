package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.enums.PtaDropMode;
import com.drimoz.punchthemall.core.model.enums.PtaTransformOp;
import com.drimoz.punchthemall.core.model.records.PtaOffset;
import com.drimoz.punchthemall.core.model.records.PtaStateRecord;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.HashSet;
import java.util.Set;

public class PtaTransformation {

    /**
     * A transformation that does nothing, for callers that need an object rather than a null. Safe
     * to share: every field is immutable and {@code chance} of zero makes each {@code has*} query
     * answer false.
     */
    public static final PtaTransformation NONE = createAir(0, null, null);

    private final double chance;
    private final Block block;
    private final Fluid fluid;
    private final Set<PtaStateRecord<?>> stateList;
    private final CompoundTag nbtList;
    private final SoundEvent sound;
    private final ParticleOptions particles;

    /** What to do at the destination. Defaults to the pre-offset behaviour, {@link PtaTransformOp#REPLACE}. */
    private final PtaTransformOp op;

    /** Where the destination is, relative to the interacted block. {@link PtaOffset#NONE} is the block itself. */
    private final PtaOffset offset;

    /** Optional gate on what is already at the destination; {@code null} means "anything goes". */
    private final PtaBlock require;

    /** For {@link PtaTransformOp#BREAK}: what the destroyed block leaves behind. */
    private final PtaDropMode dropMode;

    /**
     * When set, this writes whatever block is at that offset instead of a fixed one — the half of a
     * "move this block over there" that puts it down. Read while planning, so it sees the world as it
     * was before any of this click's transformations ran.
     */
    private final PtaOffset copyFrom;

    // Calculated Properties

    public boolean hasTransformation() {
        return chance > 0;
    }

    /**
     * Whether this writes air. Note that a {@link PtaTransformOp#BREAK} also has no block or fluid
     * to write, so callers deciding what to put down must check {@link #getOp()} first — breaking a
     * block and overwriting it with air look identical here but are different in the world.
     */
    public boolean isAir() {
        return hasTransformation() && block == null && fluid == null && copyFrom == null;
    }

    public boolean isBlock() {
        return hasTransformation() && block != null && fluid == null;
    }

    public boolean isFluid() {
        return hasTransformation() && block == null && fluid != null;
    }

    public boolean isBreak() {
        return hasTransformation() && op == PtaTransformOp.BREAK;
    }

    public boolean hasOffset() {
        return !offset.isZero();
    }

    public boolean hasRequirement() {
        return require != null;
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

    public PtaTransformOp getOp() {
        return op;
    }

    public PtaOffset getOffset() {
        return offset;
    }

    public PtaBlock getRequire() {
        return require;
    }

    public boolean shouldDropItems() {
        return dropMode.drops();
    }


    public PtaDropMode getDropMode() {
        return dropMode;
    }

    /** Whether this writes the block found at {@link #getCopyFrom()} rather than a fixed one. */
    public boolean isCopy() {
        return hasTransformation() && copyFrom != null;
    }

    public PtaOffset getCopyFrom() {
        return copyFrom;
    }

    // Life cycle

    public static PtaTransformation createBlock(double chance, Block block, Set<PtaStateRecord<?>> stateList, CompoundTag nbtList, SoundEvent sound, ParticleOptions particles) {
        return new PtaTransformation(chance, block, null, stateList, nbtList, sound, particles,
                PtaTransformOp.REPLACE, PtaOffset.NONE, null, PtaDropMode.VANILLA, null);
    }

    public static PtaTransformation createFluid(double chance, Fluid fluid, Set<PtaStateRecord<?>> stateList, CompoundTag nbtList, SoundEvent sound, ParticleOptions particles) {
        return new PtaTransformation(chance, null, fluid, stateList, nbtList, sound, particles,
                PtaTransformOp.REPLACE, PtaOffset.NONE, null, PtaDropMode.VANILLA, null);
    }

    public static PtaTransformation createAir(double chance, SoundEvent sound, ParticleOptions particles) {
        return new PtaTransformation(chance, null, null, null, null, sound, particles,
                PtaTransformOp.REPLACE, PtaOffset.NONE, null, PtaDropMode.VANILLA, null);
    }

    /** Writes whatever block stands at {@code copyFrom} when the click happens. */
    public static PtaTransformation createCopy(double chance, PtaOffset copyFrom, CompoundTag nbtList, SoundEvent sound, ParticleOptions particles) {
        return new PtaTransformation(chance, null, null, null, nbtList, sound, particles,
                PtaTransformOp.REPLACE, PtaOffset.NONE, null, PtaDropMode.VANILLA,
                copyFrom == null ? PtaOffset.NONE : copyFrom);
    }

    /**
     * A copy of this transformation with the placement fields set. Kept separate from the factories
     * so the common case — what to write — stays readable, and callers that do not care about
     * placement carry on unchanged.
     */
    public PtaTransformation withPlacement(PtaTransformOp op, PtaOffset offset, PtaBlock require, PtaDropMode dropMode) {
        return new PtaTransformation(chance, block, fluid, stateList, nbtList, sound, particles,
                op, offset, require, dropMode, copyFrom);
    }

    protected PtaTransformation(
            double chance,
            Block block, Fluid fluid,
            Set<PtaStateRecord<?>> stateList, CompoundTag nbtList,
            SoundEvent sound, ParticleOptions particles,
            PtaTransformOp op, PtaOffset offset, PtaBlock require, PtaDropMode dropMode,
            PtaOffset copyFrom
    ) {
        if (block != null && fluid != null)
            throw new IllegalArgumentException("Transformation must be either a Fluid or a Block.");
        if (copyFrom != null && (block != null || fluid != null))
            throw new IllegalArgumentException("Transformation cannot both copy a block and name one.");

        this.chance = chance < 0 ? 0 : chance > 1 ? 1 : chance;
        this.sound = sound;
        this.particles = particles;
        this.op = op == null ? PtaTransformOp.REPLACE : op;
        this.offset = offset == null ? PtaOffset.NONE : offset;
        this.require = require;
        this.dropMode = dropMode == null ? PtaDropMode.VANILLA : dropMode;
        this.copyFrom = copyFrom;

        if (block == null && fluid == null) {
            this.block = null;
            this.fluid = null;
            this.stateList = new HashSet<>();
            this.nbtList = nbtList == null ? new CompoundTag() : nbtList;
        } else {
            this.block = block;
            this.fluid = fluid;
            this.stateList = stateList == null ? new HashSet<>() : stateList;
            this.nbtList = nbtList == null ? new CompoundTag() : nbtList;
        }
    }

    @Override
    public String toString() {
        return "PtaTransformation{chance=" + chance + ", op=" + op.serialized()
                + ", block=" + block + ", fluid=" + fluid
                + (copyFrom == null ? "" : ", copyFrom=" + copyFrom)
                + ", offset=" + offset + '}';
    }
}
