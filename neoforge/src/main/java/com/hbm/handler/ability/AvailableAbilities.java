/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.handler.ability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class AvailableAbilities {

    public static final AvailableAbilities EMPTY =
            new AvailableAbilities(List.of(), List.of(), List.of());

    public record Declared<A extends BaseAbility>(A ability, int maxLevel) {}

    private final List<Declared<ToolAreaAbility>> area;
    private final List<Declared<ToolHarvestAbility>> harvest;
    private final List<Declared<WeaponAbility>> hits;
    private final byte[] areaMax;
    private final byte[] harvestMax;
    private final byte[] hitMax;

    private AvailableAbilities(
            List<Declared<ToolAreaAbility>> area,
            List<Declared<ToolHarvestAbility>> harvest,
            List<Declared<WeaponAbility>> hits) {
        this.area = area;
        this.harvest = harvest;
        this.hits = hits;
        this.areaMax = maxima(area);
        this.harvestMax = maxima(harvest);
        this.hitMax = maxima(hits);
    }

    private static byte[] maxima(List<? extends Declared<?>> declared) {
        int span = 0;
        for (var entry : declared) span = Math.max(span, entry.ability().index() + 1);
        var maxima = new byte[span];
        Arrays.fill(maxima, (byte) -1);
        for (var entry : declared) maxima[entry.ability().index()] = (byte) entry.maxLevel();
        return maxima;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int maxLevel(BaseAbility ability) {
        if (!ability.allowed()) return -1;
        byte[] maxima =
                switch (ability) {
                    case ToolAreaAbility _ -> areaMax;
                    case ToolHarvestAbility _ -> harvestMax;
                    case WeaponAbility _ -> hitMax;
                };
        int index = ability.index();
        return index < maxima.length ? maxima[index] : -1;
    }

    public boolean supports(BaseAbility ability) {
        return maxLevel(ability) != -1;
    }

    public List<Declared<WeaponAbility>> hits() {
        return hits;
    }

    public List<ToolPreset> defaultPresets() {
        List<ToolPreset> presets = new ArrayList<>(1 + area.size() + harvest.size());
        presets.add(ToolPreset.NONE);
        for (int i = 0, n = area.size(); i < n; i++) {
            var entry = area.get(i);
            if (entry.ability() != ToolAreaAbility.NONE && entry.ability().allowed()) {
                presets.add(ToolPreset.ofArea(entry.ability(), entry.maxLevel()));
            }
        }
        for (int i = 0, n = harvest.size(); i < n; i++) {
            var entry = harvest.get(i);
            if (entry.ability() != ToolHarvestAbility.NONE && entry.ability().allowed()) {
                presets.add(ToolPreset.ofHarvest(entry.ability(), entry.maxLevel()));
            }
        }
        return presets;
    }

    public void appendTooltip(Consumer<Component> adder) {
        appendTooltip(adder, _ -> true);
    }

    public void appendTooltip(Consumer<Component> adder, Predicate<BaseAbility> selectable) {
        boolean tools = appendToolLines(adder, selectable, area, false);
        tools = appendToolLines(adder, selectable, harvest, tools);
        if (tools) {
            adder.accept(Component.translatable("desc.item.toolAbility.rightClickTo"));
            adder.accept(Component.translatable("desc.item.toolAbility.sneakClickTo"));
            adder.accept(Component.translatable("desc.item.toolAbility.altClickTo"));
        }

        if (hits.isEmpty()) return;
        adder.accept(Component.translatable("desc.item.swordAbility.weaponModifiers"));
        for (int i = 0, n = hits.size(); i < n; i++) {
            var entry = hits.get(i);
            adder.accept(
                    Component.literal("  ")
                            .append(
                                    ToolPreset.name(entry.ability(), entry.maxLevel())
                                            .withStyle(ChatFormatting.RED)));
        }
    }

    private static boolean appendToolLines(
            Consumer<Component> adder,
            Predicate<BaseAbility> selectable,
            List<? extends Declared<?>> declared,
            boolean headed) {
        for (int i = 0, n = declared.size(); i < n; i++) {
            var entry = declared.get(i);
            BaseAbility ability = entry.ability();
            if (ability == ToolAreaAbility.NONE
                    || ability == ToolHarvestAbility.NONE
                    || !ability.allowed()) continue;
            if (!headed) {
                adder.accept(Component.translatable("desc.item.toolAbility.abilities"));
                headed = true;
            }
            adder.accept(
                    Component.literal("  ")
                            .append(
                                    ToolPreset.name(ability, entry.maxLevel())
                                            .withStyle(
                                                    selectable.test(ability)
                                                            ? ChatFormatting.GOLD
                                                            : ChatFormatting.GRAY)));
        }
        return headed;
    }

    public static final class Builder {

        private final List<Declared<ToolAreaAbility>> area = new ArrayList<>();
        private final List<Declared<ToolHarvestAbility>> harvest = new ArrayList<>();
        private final List<Declared<WeaponAbility>> hits = new ArrayList<>();

        private Builder() {}

        public Builder add(BaseAbility ability, int level) {
            if (level < 0 || level >= ability.levels()) {
                throw new IllegalArgumentException(
                        ability.translationKey() + " has no level " + level);
            }
            switch (ability) {
                case ToolAreaAbility member -> add(area, member, level);
                case ToolHarvestAbility member -> add(harvest, member, level);
                case WeaponAbility member -> add(hits, member, level);
            }
            return this;
        }

        private static <A extends BaseAbility> void add(
                List<Declared<A>> family, A ability, int level) {
            for (var entry : family) {
                if (entry.ability() == ability) {
                    throw new IllegalArgumentException(
                            ability.translationKey() + " declared twice");
                }
            }
            family.add(new Declared<>(ability, level));
        }

        public AvailableAbilities build() {
            if (area.isEmpty() && harvest.isEmpty() && hits.isEmpty()) return EMPTY;
            return new AvailableAbilities(sorted(area), sorted(harvest), sorted(hits));
        }

        private static <A extends BaseAbility> List<Declared<A>> sorted(List<Declared<A>> family) {
            var copy = new ArrayList<>(family);
            copy.sort(Comparator.comparingInt(entry -> entry.ability().sortOrder()));
            return List.copyOf(copy);
        }
    }
}
