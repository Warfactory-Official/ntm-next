// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class AvailableAbilities {

    public static final AvailableAbilities EMPTY = new AvailableAbilities(Map.of());

    private final Map<BaseAbility, Integer> declared;

    private AvailableAbilities(Map<BaseAbility, Integer> declared) {
        this.declared = declared;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int maxLevel(BaseAbility ability) {
        if (!ability.allowed()) return -1;
        return declared.getOrDefault(ability, -1);
    }

    public boolean supports(BaseAbility ability) {
        return maxLevel(ability) != -1;
    }

    public List<Map.Entry<BaseAbility, Integer>> weapon() {
        return of(WeaponAbility.class);
    }

    public List<Map.Entry<BaseAbility, Integer>> area() {
        return of(ToolAreaAbility.class);
    }

    public List<Map.Entry<BaseAbility, Integer>> harvest() {
        return of(ToolHarvestAbility.class);
    }

    private List<Map.Entry<BaseAbility, Integer>> of(Class<? extends BaseAbility> family) {
        List<Map.Entry<BaseAbility, Integer>> entries = new ArrayList<>();
        for (Map.Entry<BaseAbility, Integer> entry : declared.entrySet()) {
            if (family.isInstance(entry.getKey()) && entry.getKey().allowed()) entries.add(entry);
        }
        entries.sort(
                Comparator.<Map.Entry<BaseAbility, Integer>>comparingInt(
                                e -> e.getKey().sortOrder())
                        .thenComparingInt(Map.Entry::getValue));
        return entries;
    }

    public List<ToolPreset> defaultPresets() {
        List<ToolPreset> presets = new ArrayList<>();
        presets.add(ToolPreset.NONE);

        for (Map.Entry<BaseAbility, Integer> entry : area()) {
            if (entry.getKey() == ToolAreaAbility.NONE) continue;
            presets.add(ToolPreset.ofArea((ToolAreaAbility) entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<BaseAbility, Integer> entry : harvest()) {
            if (entry.getKey() == ToolHarvestAbility.NONE) continue;
            presets.add(
                    ToolPreset.ofHarvest((ToolHarvestAbility) entry.getKey(), entry.getValue()));
        }

        presets.sort(
                Comparator.comparingInt((ToolPreset p) -> p.harvest().sortOrder())
                        .thenComparingInt(ToolPreset::harvestLevel)
                        .thenComparingInt(p -> p.area().sortOrder())
                        .thenComparingInt(ToolPreset::areaLevel));
        return presets;
    }

    public boolean hasToolAbilities() {
        return !area().isEmpty() || !harvest().isEmpty();
    }

    public void appendTooltip(Consumer<Component> adder) {
        List<Map.Entry<BaseAbility, Integer>> tools = new ArrayList<>(area());
        tools.addAll(harvest());
        tools.removeIf(
                entry ->
                        entry.getKey() == ToolAreaAbility.NONE
                                || entry.getKey() == ToolHarvestAbility.NONE);
        tools.sort(
                Comparator.<Map.Entry<BaseAbility, Integer>>comparingInt(
                                e -> e.getKey().sortOrder())
                        .thenComparingInt(Map.Entry::getValue));

        if (!tools.isEmpty()) {
            adder.accept(Component.translatable("desc.item.toolAbility.abilities"));
            for (Map.Entry<BaseAbility, Integer> entry : tools) {
                adder.accept(
                        Component.literal("  ")
                                .append(
                                        ToolPreset.name(entry.getKey(), entry.getValue())
                                                .withStyle(ChatFormatting.GOLD)));
            }
            adder.accept(Component.translatable("desc.item.toolAbility.rightClickTo"));
            adder.accept(Component.translatable("desc.item.toolAbility.sneakClickTo"));
            adder.accept(Component.translatable("desc.item.toolAbility.altClickTo"));
        }

        List<Map.Entry<BaseAbility, Integer>> weapons = weapon();
        if (weapons.isEmpty()) return;

        adder.accept(Component.translatable("desc.item.swordAbility.weaponModifiers"));
        for (Map.Entry<BaseAbility, Integer> entry : weapons) {
            adder.accept(
                    Component.literal("  ")
                            .append(
                                    ToolPreset.name(entry.getKey(), entry.getValue())
                                            .withStyle(ChatFormatting.RED)));
        }
    }

    public static final class Builder {

        private final Map<BaseAbility, Integer> declared = new LinkedHashMap<>();

        private Builder() {}

        public Builder add(BaseAbility ability, int level) {
            assert level >= 0 && level < ability.levels();
            assert !declared.containsKey(ability);
            declared.put(ability, level);
            return this;
        }

        public AvailableAbilities build() {
            return declared.isEmpty() ? EMPTY : new AvailableAbilities(Map.copyOf(declared));
        }
    }
}
