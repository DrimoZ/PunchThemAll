package com.drimoz.punchthemall.core.model.records;

import com.drimoz.punchthemall.core.registry.RegistryConstants;
import com.drimoz.punchthemall.core.util.PTALoggers;
import net.minecraft.world.level.block.state.properties.Property;

public record PtaStateRecord<T extends Comparable<T>>(Property<T> property, String value) {

    // Life cycle

    public PtaStateRecord {
        // Validate the value only if it's not intended to be a copy value
        if (!value.equalsIgnoreCase(RegistryConstants.SAME_STATE) && property.getPossibleValues().stream().noneMatch(v -> v.toString().equals(value))) {
            throw new IllegalArgumentException("The given value is not part of the given property.");
        }
    }

    // Interface

    public boolean isCopyValue() {
        return value != null && value.equalsIgnoreCase(RegistryConstants.SAME_STATE);
    }

    public T getValue() {
        if (isCopyValue()) {
            throw new UnsupportedOperationException("Cannot get value when it is set to copy the state value.");
        }
        return property.getValue(value).orElseThrow(() ->
                new IllegalArgumentException("The given value is not part of the given property."));
    }

    // Interface ( Util )

    @Override
    public String toString() {
        return "PtaStateRecord{" +
                "property=" + property +
                ", value=" + value +
                '}';
    }
}
