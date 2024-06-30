package com.drimoz.punchthemall.core.model.records;

import net.minecraft.world.level.block.state.properties.Property;

public record PtaStateRecord<T extends Comparable<T>>(Property<T> property, T value) {

    // Life cycle

    public PtaStateRecord {
        if (!property.getPossibleValues().contains(value)) {
            throw new IllegalArgumentException("The given value is not part of the given property.");
        }
    }

    // Interface

    // Interface ( Util )

    @Override
    public String toString() {
        return "PtaStateRecord{" +
                "property=" + property +
                ", value=" + value +
                '}';
    }
}
