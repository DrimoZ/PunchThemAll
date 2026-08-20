package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaInteractionRecord;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PtaInteraction {

    private final ResourceLocation id;
    private final PtaTypeEnum type;

    private final PtaInteractionRecord hurtPlayer;
    private final PtaInteractionRecord consumeFood;

    private final PtaHand hand;
    private final PtaBlock block;
    private final List<PtaTransformation> transformations;

    /**
     * One roll deciding whether the transformations are attempted at all, on top of each
     * entry's own chance. 1.0 — always attempted — unless the file grouped them.
     */
    private final double transformationChance;
    private final PtaRewards rewards;

    private final Set<String> biomeWhitelist;
    private final Set<String> biomeBlackList;

    private final PtaExtras extras;

    /** Gameplay is unaffected; only JEI/EMI skip this interaction. See {@link #isHidden()}. */
    private final boolean hidden;

    /**
     * Identity of the source JSON, carried so two resolutions of an unchanged file compare equal.
     * The runtime model is rebuilt from scratch on every reload and sync, so without this the
     * viewers would see an entirely new recipe set each time and churn their hidden-recipe state.
     */
    private final int contentHash;

    // Calculated Properties

    public boolean hasBiomeWhiteList() {
        return !biomeWhitelist.isEmpty() && biomeBlackList.isEmpty();
    }

    public boolean hasBiomeBlackList() {
        return biomeWhitelist.isEmpty() && !biomeBlackList.isEmpty();
    }

    public boolean hasHurtPlayer() {
        return hurtPlayer != null;
    }

    public boolean hasConsumeFood() {
        return consumeFood != null;
    }

    // Getters

    public ResourceLocation getId() {
        return id;
    }

    public PtaTypeEnum getType() {
        return type;
    }

    public PtaInteractionRecord getHurtPlayer() {
        return hurtPlayer;
    }

    public PtaInteractionRecord getConsumeFood() {
        return consumeFood;
    }

    public PtaHand getHand() {
        return hand;
    }

    public PtaBlock getBlock() {
        return block;
    }

    /**
     * The first transformation, or an inert one when there is none. Kept for the recipe viewers and
     * for callers that predate multi-step transformations; gameplay walks {@link #getTransformations()}.
     */
    public PtaTransformation getTransformation() {
        return transformations.isEmpty() ? PtaTransformation.NONE : transformations.get(0);
    }

    /** Every transformation this interaction applies, in declaration order. */
    public List<PtaTransformation> getTransformations() {
        return transformations;
    }

    /** The roll that gates the whole set. See {@link #getTransformations()}. */
    public double getTransformationChance() {
        return transformationChance;
    }

    public boolean hasTransformations() {
        return !transformations.isEmpty();
    }

    public PtaPool getPool() {
        return rewards.getPool();
    }

    public PtaRewards getRewards() {
        return rewards;
    }

    public Set<String> getBiomeWhitelist() {
        return biomeWhitelist;
    }

    public Set<String> getBiomeBlackList() {
        return biomeBlackList;
    }

    public PtaExtras getExtras() {
        return extras;
    }

    public PtaConditions getConditions() {
        return extras.conditions();
    }

    /**
     * Whether the recipe viewers should leave this interaction out. It still loads and still fires:
     * this is for interactions a pack does not want to advertise (a secret, or an implementation
     * detail of a multi-step recipe), not a way to disable one — use {@code enabled} for that.
     */
    public boolean isHidden() {
        return hidden;
    }

    // Life Cycle

    public PtaInteraction(
            ResourceLocation id, PtaTypeEnum type,
            PtaInteractionRecord hurtPlayer, PtaInteractionRecord consumeFood,
            PtaHand hand, PtaBlock block, PtaTransformation transformation, PtaRewards rewards,
            Set<String> biomeWhitelist, Set<String> biomeBlackList,
            PtaExtras extras
    ) {
        this(id, type, hurtPlayer, consumeFood, hand, block,
                transformation == null ? List.of() : List.of(transformation), 1.0D, rewards,
                biomeWhitelist, biomeBlackList, extras, false, 0);
    }

    public PtaInteraction(
            ResourceLocation id, PtaTypeEnum type,
            PtaInteractionRecord hurtPlayer, PtaInteractionRecord consumeFood,
            PtaHand hand, PtaBlock block, List<PtaTransformation> transformations, double transformationChance, PtaRewards rewards,
            Set<String> biomeWhitelist, Set<String> biomeBlackList,
            PtaExtras extras, boolean hidden, int contentHash
    ) {
        if (id == null) throw new IllegalArgumentException("Missing id for Interaction");
        if (type == null) throw new IllegalArgumentException("Missing type for Interaction");
        if (rewards == null) throw new IllegalArgumentException("Missing rewards for Interaction");

        this.hidden = hidden;
        this.contentHash = contentHash;
        this.id = id;
        this.type = type;
        this.hurtPlayer = hurtPlayer;
        this.consumeFood = consumeFood;
        this.hand = hand == null ? PtaHand.createEmpty(PtaHandEnum.ANY_HAND) : hand;
        this.block = block == null ? PtaBlock.createAir() : block;
        this.transformations = keepApplicable(transformations, this.block);
        this.transformationChance = transformationChance < 0 ? 0 : Math.min(transformationChance, 1);
        this.rewards = rewards;
        this.biomeWhitelist = biomeWhitelist == null ? new HashSet<>() : biomeWhitelist;
        this.biomeBlackList = biomeBlackList == null ? new HashSet<>() : biomeBlackList;
        this.extras = extras == null ? PtaExtras.EMPTY : extras;
    }

    /**
     * Drop the transformations that cannot do anything, so the runtime never has to ask again.
     *
     * <p>An air target has no block under the cursor to act on, so a transformation aimed at the
     * origin is meaningless and was discarded before offsets existed. One with an offset is not:
     * its origin is the player, so "place a block above me" is a perfectly good air interaction and
     * is kept.</p>
     */
    private static List<PtaTransformation> keepApplicable(List<PtaTransformation> transformations, PtaBlock block) {
        if (transformations == null || transformations.isEmpty()) return List.of();

        List<PtaTransformation> applicable = new ArrayList<>(transformations.size());
        for (PtaTransformation transformation : transformations) {
            if (transformation == null || !transformation.hasTransformation()) continue;
            if (block.isAir() && !transformation.hasOffset()) continue;
            applicable.add(transformation);
        }
        return List.copyOf(applicable);
    }

    /**
     * Equality is (id, source JSON), not object identity. The recipe viewers diff the interaction set
     * on every reload and sync; with identity equality every entry looked new, so JEI hid and re-added
     * the whole category each time and its permanent hidden-recipe set grew without bound.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PtaInteraction that)) return false;
        return contentHash == that.contentHash && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + contentHash;
    }

    @Override
    public String toString() {
        return "PtaInteraction{id=" + id + ", type=" + type + '}';
    }
}
