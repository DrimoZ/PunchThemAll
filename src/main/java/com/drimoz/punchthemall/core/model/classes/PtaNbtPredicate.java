package com.drimoz.punchthemall.core.model.classes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A typed NBT predicate (schema_version 2, §5.3). Matches against a {@link CompoundTag}; for items
 * this is PTA's stable {@code ItemView} (so the authoring format is identical across mod versions).
 *
 * <p>Shape: a dotted {@code path}, where a segment ending in {@code []} iterates a list; an optional
 * {@code where} compound filters which list elements qualify; and an optional integer range checks
 * the numeric leaf. Satisfied when at least one reachable leaf is in range (or exists, if no range).</p>
 */
public record PtaNbtPredicate(String path, Optional<Integer> intMin, Optional<Integer> intMax, CompoundTag where) {

    public PtaNbtPredicate(String path, Optional<Integer> intMin, Optional<Integer> intMax, CompoundTag where) {
        this.path = path == null ? "" : path;
        this.intMin = intMin;
        this.intMax = intMax;
        this.where = where == null ? new CompoundTag() : where;
    }

    public boolean matches(CompoundTag root) {
        if (root == null) return false;

        String[] segments = path.isEmpty() ? new String[0] : path.split("\\.");
        List<Tag> leaves = new ArrayList<>();
        collect(root, segments, 0, leaves);
        if (leaves.isEmpty()) return false;

        boolean rangeGiven = intMin.isPresent() || intMax.isPresent();
        for (Tag leaf : leaves) {
            if (!(leaf instanceof NumericTag numeric)) {
                if (!rangeGiven) return true; // presence-only predicate
                continue;
            }
            long value = numeric.getAsLong();
            if (intMin.isPresent() && value < intMin.get()) continue;
            if (intMax.isPresent() && value > intMax.get()) continue;
            return true;
        }
        return false;
    }

    private void collect(Tag current, String[] segments, int index, List<Tag> out) {
        if (index >= segments.length) {
            out.add(current);
            return;
        }

        String segment = segments[index];
        boolean isList = segment.endsWith("[]");
        String name = isList ? segment.substring(0, segment.length() - 2) : segment;

        if (!(current instanceof CompoundTag compound)) return;
        Tag child = compound.get(name);
        if (child == null) return;

        if (isList) {
            if (!(child instanceof ListTag list)) return;
            for (Tag element : list) {
                if (matchesWhere(element)) {
                    collect(element, segments, index + 1, out);
                }
            }
        } else {
            collect(child, segments, index + 1, out);
        }
    }

    private boolean matchesWhere(Tag element) {
        if (where.isEmpty()) return true;
        if (!(element instanceof CompoundTag compound)) return false;

        for (String key : where.getAllKeys()) {
            Tag expected = where.get(key);
            Tag actual = compound.get(key);
            if (actual == null) return false;

            if (expected instanceof NumericTag expectedNumber && actual instanceof NumericTag actualNumber) {
                if (expectedNumber.getAsLong() != actualNumber.getAsLong()) return false;
            } else if (!expected.getAsString().equals(actual.getAsString())) {
                return false;
            }
        }
        return true;
    }
}
