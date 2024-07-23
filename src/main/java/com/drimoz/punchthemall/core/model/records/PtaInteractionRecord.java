package com.drimoz.punchthemall.core.model.records;

import java.util.concurrent.ThreadLocalRandom;

public record PtaInteractionRecord (double chance, int min, int max) {

    // Calculated Properties

    public boolean shouldExecute() {
        return ThreadLocalRandom.current().nextDouble() <= chance;
    }

    public int getValue() {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    // Life cycle

    public PtaInteractionRecord(double chance, int min, int max) {
        this.chance = chance < 0 ? 0 : chance > 1 ? 1 : chance;
        this.min = Math.max(min, 1);
        this.max = Math.max(min, max);
    }

    // Interface

    @Override
    public String toString() {
        return "PtaInteractionRecord{" +
                "chance=" + chance +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}
