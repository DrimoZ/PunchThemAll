package com.drimoz.punchthemall.core.model.classes;

import com.drimoz.punchthemall.core.model.enums.PtaHandEnum;
import com.drimoz.punchthemall.core.model.enums.PtaTypeEnum;
import com.drimoz.punchthemall.core.model.records.PtaInteractionRecord;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class PtaInteraction {

    // Public Properties

    private final ResourceLocation id;
    private final PtaTypeEnum type;

    private final PtaInteractionRecord hurtPlayer;
    private final PtaInteractionRecord consumeFood;

    private final PtaHand hand;
    private final PtaBlock block;
    private final PtaTransformation transformation;
    private final PtaPool pool;

    private final Set<String> biomeWhitelist;
    private final Set<String> biomeBlackList;

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

    public PtaTransformation getTransformation() {
        return transformation;
    }

    public PtaPool getPool() {
        return pool;
    }

    public Set<String> getBiomeWhitelist() {
        return biomeWhitelist;
    }

    public Set<String> getBiomeBlackList() {
        return biomeBlackList;
    }

    // Life Cycle

    public PtaInteraction(
            ResourceLocation id, PtaTypeEnum type,
            PtaInteractionRecord hurtPlayer, PtaInteractionRecord consumeFood,
            PtaHand hand, PtaBlock block, PtaTransformation transformation, PtaPool pool,
            Set<String> biomeWhitelist, Set<String> biomeBlackList
    ) {
        if (id == null) throw new IllegalArgumentException("Missing id for Interaction");
        if (type == null) throw new IllegalArgumentException("Missing type for Interaction");
        if (pool == null) throw new IllegalArgumentException("Missing pool for Interaction");

        this.id = id;
        this.type = type;
        this.hurtPlayer = hurtPlayer;
        this.consumeFood = consumeFood;
        this.hand = hand == null ? PtaHand.createEmpty(PtaHandEnum.ANY_HAND) : hand;
        this.block = block == null ? PtaBlock.createAir() : block;
        this.transformation = transformation == null || this.block.isAir() ? PtaTransformation.createAir(0, null, null) : transformation;
        this.pool = pool;
        this.biomeWhitelist = biomeWhitelist == null ? new HashSet<>() : biomeWhitelist;
        this.biomeBlackList = biomeBlackList == null ? new HashSet<>() : biomeBlackList;
    }

    // Interface



    // Interface ( Util )


    @Override
    public String toString() {
        return "PtaInteraction{" +
                "\n\tid=" + id +
                ", \n\ttype=" + type +
                ", \n\thand=" + hand +
                ", \n\tblock=" + block +
                ", \n\ttransformation=" + transformation +
                ", \n\tpool=" + pool +
                ", \n\tbiomeWhitelist=" + biomeWhitelist +
                ", \n\thunger=" + consumeFood +
                ", \n\tdamage=" + hurtPlayer +
                "\n}";
    }
}
