package com.drimoz.punchthemall.core.model;

import net.minecraft.world.level.block.state.properties.Property;

public class StateEntry<T extends Comparable<T>> {

    // Private Properties

    private final Property<T> property;
    private final T value;

    // Life cycle

    public StateEntry(Property<T> property, T value) {
        if (!property.getPossibleValues().contains(value)) {
            throw new IllegalArgumentException("The given value is not part of the given property.");
        }
        this.property = property;
        this.value = value;
    }

    // Interface

    public Property<T> getProperty() {
        return property;
    }

    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "StateEntry = {" +
                "\n\tproperty = " + property +
                ", \n\tvalue = " + value +
                '}';
    }
}
