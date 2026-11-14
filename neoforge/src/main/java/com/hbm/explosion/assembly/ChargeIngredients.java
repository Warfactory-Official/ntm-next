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

package com.hbm.explosion.assembly;

import com.hbm.registration.OverridePoint;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.explosion.assembly.ChargeIngredients.Channel.*;
import static com.hbm.explosion.assembly.ChargeIngredients.Mode.*;

public final class ChargeIngredients {
    public static final OverridePoint<Consumer<Editor>> EDITS =
            OverridePoint.of(
                    Identifier.fromNamespaceAndPath("hbm", "custom_nuke_entries"),
                    _ -> {});

    public enum Channel {
        CONVENTIONAL("TNT"),
        NUCLEAR("Nuclear"),
        THERMONUCLEAR("Hydrogen"),
        ANTIMATTER("Antimatter"),
        FALLOUT("Salted"),
        SCHRABIDIUM("Schrabidium"),
        ANTI_MASS("Anti Mass");

        private final String displayLabel;

        Channel(String displayLabel) {
            this.displayLabel = displayLabel;
        }

        public String displayLabel() {
            return displayLabel;
        }
    }

    public enum Mode {
        ADD,
        MULTIPLY
    }

    public record Contribution(Channel channel, float value, Mode mode) {
        public Contribution {
            Objects.requireNonNull(channel);
            Objects.requireNonNull(mode);
            if (!Float.isFinite(value))
                throw new IllegalArgumentException("Non-finite ingredient contribution");
        }
    }

    public static final class Editor {
        private final Contribution[] rows = new Contribution[BuiltInRegistries.ITEM.size()];
        private float antimatterNuclearThreshold = 50F;

        private Editor() {}

        public void put(Item item, Contribution contribution) {
            rows[BuiltInRegistries.ITEM.getId(item)] = Objects.requireNonNull(contribution);
        }

        public void remove(Item item) {
            rows[BuiltInRegistries.ITEM.getId(item)] = null;
        }

        public void antimatterNuclearThreshold(float threshold) {
            if (!Float.isFinite(threshold) || threshold < 0F) {
                throw new IllegalArgumentException("Invalid antimatter nuclear prerequisite");
            }
            antimatterNuclearThreshold = threshold;
        }
    }

    private static final class Ready {
        private static final Contribution[] ROWS;
        private static final float ANTIMATTER_NUCLEAR_THRESHOLD;
        private static final Item FALLING_ACCESSORY;

        static {
            var editor = new Editor();
            declareBase(editor);
            EDITS.get().accept(editor);
            ROWS = editor.rows.clone();
            ANTIMATTER_NUCLEAR_THRESHOLD = editor.antimatterNuclearThreshold;
            FALLING_ACCESSORY = requiredItem(Identifier.fromNamespaceAndPath("hbm", "custom_fall"));
        }
    }

    private ChargeIngredients() {}

    public static @Nullable Contribution lookup(ItemStack stack) {
        if (stack.isEmpty()) return null;
        return Ready.ROWS[BuiltInRegistries.ITEM.getId(stack.getItem())];
    }

    public static Item fallingAccessory() {
        return Ready.FALLING_ACCESSORY;
    }

    public static ChargeYield evaluate(Iterable<ItemStack> contents) {
        var rows = Ready.ROWS;
        List<ItemStack> indexedContents;
        Iterator<ItemStack> remainingContents;
        if (contents instanceof List<ItemStack> list) {
            indexedContents = list;
            remainingContents = null;
        } else {
            indexedContents = null;
            remainingContents = contents.iterator();
        }
        float conventionalAmount = 0F;
        float nuclearAmount = 0F;
        float thermonuclearAmount = 0F;
        float antimatterAmount = 0F;
        float falloutAmount = 0F;
        float schrabidiumAmount = 0F;
        float antiMassAmount = 0F;
        float conventionalFactor = 1F;
        float nuclearFactor = 1F;
        float thermonuclearFactor = 1F;
        float antimatterFactor = 1F;
        float falloutFactor = 1F;
        float schrabidiumFactor = 1F;

        int slot = 0;
        while (true) {
            ItemStack stack;
            if (indexedContents != null) {
                if (slot == indexedContents.size()) break;
                stack = indexedContents.get(slot);
                slot++;
            } else {
                if (!remainingContents.hasNext()) break;
                stack = remainingContents.next();
            }
            if (stack.isEmpty()) continue;
            var contribution = rows[BuiltInRegistries.ITEM.getId(stack.getItem())];
            if (contribution == null) continue;
            float weightedValue = contribution.value() * stack.getCount();
            if (contribution.mode() == ADD) {
                switch (contribution.channel()) {
                    case CONVENTIONAL -> conventionalAmount += weightedValue;
                    case NUCLEAR -> nuclearAmount += weightedValue;
                    case THERMONUCLEAR -> thermonuclearAmount += weightedValue;
                    case ANTIMATTER -> antimatterAmount += weightedValue;
                    case FALLOUT -> falloutAmount += weightedValue;
                    case SCHRABIDIUM -> schrabidiumAmount += weightedValue;
                    case ANTI_MASS -> antiMassAmount += weightedValue;
                }
            } else {
                switch (contribution.channel()) {
                    case CONVENTIONAL -> conventionalFactor *= weightedValue;
                    case NUCLEAR -> nuclearFactor *= weightedValue;
                    case THERMONUCLEAR -> thermonuclearFactor *= weightedValue;
                    case ANTIMATTER -> antimatterFactor *= weightedValue;
                    case FALLOUT -> falloutFactor *= weightedValue;
                    case SCHRABIDIUM -> schrabidiumFactor *= weightedValue;
                    case ANTI_MASS -> {}
                }
            }
        }

        float conventional = conventionalAmount * conventionalFactor;
        float nuclear = nuclearAmount * nuclearFactor;
        float thermonuclear = thermonuclearAmount * thermonuclearFactor;
        float antimatter = antimatterAmount * antimatterFactor;
        float fallout = falloutAmount * falloutFactor;
        float schrabidium = schrabidiumAmount * schrabidiumFactor;
        if (conventional < 16F) nuclear = 0F;
        if (nuclear < 100F) thermonuclear = 0F;
        if (nuclear < Ready.ANTIMATTER_NUCLEAR_THRESHOLD) antimatter = 0F;
        if (nuclear < 50F) schrabidium = 0F;
        if (schrabidium == 0F) antiMassAmount = 0F;
        return new ChargeYield(
                conventional,
                nuclear,
                thermonuclear,
                antimatter,
                fallout,
                schrabidium,
                antiMassAmount);
    }

    private static Item requiredItem(Identifier itemId) {
        if (!BuiltInRegistries.ITEM.containsKey(itemId)) {
            throw new IllegalArgumentException("Missing charge ingredient item " + itemId);
        }
        return BuiltInRegistries.ITEM.getValue(itemId);
    }

    private static void row(Editor editor, String itemId, Channel channel, Mode mode, float value) {
        editor.put(requiredItem(Identifier.parse(itemId)), new Contribution(channel, value, mode));
    }

    private static void declareBase(Editor editor) {
        row(editor, "minecraft:gunpowder", CONVENTIONAL, ADD, 0.8F);
        row(editor, "minecraft:tnt", CONVENTIONAL, ADD, 4F);
        row(editor, "hbm:det_cord", CONVENTIONAL, ADD, 1.5F);
        row(editor, "hbm:ingot_semtex", CONVENTIONAL, ADD, 8F);
        row(editor, "hbm:det_charge", CONVENTIONAL, ADD, 15F);
        row(editor, "hbm:red_barrel", CONVENTIONAL, ADD, 2.5F);
        row(editor, "hbm:pink_barrel", CONVENTIONAL, ADD, 4F);
        row(editor, "hbm:custom_tnt", CONVENTIONAL, ADD, 10F);
        row(editor, "hbm:ingot_u233", NUCLEAR, ADD, 15F);
        row(editor, "hbm:ingot_u235", NUCLEAR, ADD, 15F);
        row(editor, "hbm:ingot_pu239", NUCLEAR, ADD, 25F);
        row(editor, "hbm:ingot_pu241", NUCLEAR, ADD, 25F);
        row(editor, "hbm:ingot_neptunium", NUCLEAR, ADD, 30F);
        row(editor, "hbm:nugget_u233", NUCLEAR, ADD, 1.5F);
        row(editor, "hbm:nugget_u235", NUCLEAR, ADD, 1.5F);
        row(editor, "hbm:nugget_pu239", NUCLEAR, ADD, 2.5F);
        row(editor, "hbm:nugget_pu241", NUCLEAR, ADD, 2.5F);
        row(editor, "hbm:nugget_neptunium", NUCLEAR, ADD, 3F);
        row(editor, "hbm:powder_neptunium", NUCLEAR, ADD, 30F);
        row(editor, "hbm:custom_nuke", NUCLEAR, ADD, 30F);
        row(editor, "hbm:cell_deuterium", THERMONUCLEAR, ADD, 20F);
        row(editor, "hbm:cell_tritium", THERMONUCLEAR, ADD, 30F);
        row(editor, "hbm:lithium", THERMONUCLEAR, ADD, 20F);
        row(editor, "hbm:custom_hydro", THERMONUCLEAR, ADD, 30F);
        row(editor, "hbm:cell_antimatter", ANTIMATTER, ADD, 5F);
        row(editor, "hbm:custom_amat", ANTIMATTER, ADD, 15F);
        row(editor, "hbm:egg_balefire_shard", ANTIMATTER, ADD, 15F);
        row(editor, "hbm:egg_balefire", ANTIMATTER, ADD, 150F);
        row(editor, "hbm:ingot_tungsten", FALLOUT, ADD, 1F);
        row(editor, "hbm:custom_dirty", FALLOUT, ADD, 10F);
        row(editor, "hbm:ingot_schrabidium", SCHRABIDIUM, ADD, 5F);
        row(editor, "hbm:block_schrabidium", SCHRABIDIUM, ADD, 50F);
        row(editor, "hbm:nugget_schrabidium", SCHRABIDIUM, ADD, 0.5F);
        row(editor, "hbm:powder_schrabidium", SCHRABIDIUM, ADD, 5F);
        row(editor, "hbm:cell_sas3", SCHRABIDIUM, ADD, 7.5F);
        row(editor, "hbm:cell_anti_schrabidium", SCHRABIDIUM, ADD, 15F);
        row(editor, "hbm:custom_schrab", SCHRABIDIUM, ADD, 15F);
        row(editor, "hbm:nugget_euphemium", ANTI_MASS, ADD, 1F);
        row(editor, "hbm:ingot_euphemium", ANTI_MASS, ADD, 1F);
        row(editor, "minecraft:redstone", CONVENTIONAL, MULTIPLY, 1.05F);
        row(editor, "minecraft:redstone_block", CONVENTIONAL, MULTIPLY, 1.5F);
        row(editor, "hbm:ingot_uranium", NUCLEAR, MULTIPLY, 1.05F);
        row(editor, "hbm:ingot_plutonium", NUCLEAR, MULTIPLY, 1.15F);
        row(editor, "hbm:ingot_u238", NUCLEAR, MULTIPLY, 1.1F);
        row(editor, "hbm:ingot_pu238", NUCLEAR, MULTIPLY, 1.15F);
        row(editor, "hbm:nugget_uranium", NUCLEAR, MULTIPLY, 1.005F);
        row(editor, "hbm:nugget_plutonium", NUCLEAR, MULTIPLY, 1.15F);
        row(editor, "hbm:nugget_u238", NUCLEAR, MULTIPLY, 1.01F);
        row(editor, "hbm:nugget_pu238", NUCLEAR, MULTIPLY, 1.015F);
        row(editor, "hbm:powder_uranium", NUCLEAR, MULTIPLY, 1.05F);
        row(editor, "hbm:powder_plutonium", NUCLEAR, MULTIPLY, 1.15F);
        row(editor, "hbm:ingot_pu240", FALLOUT, MULTIPLY, 1.05F);
        row(editor, "hbm:nuclear_waste", FALLOUT, MULTIPLY, 1.025F);
        row(editor, "hbm:block_waste", FALLOUT, MULTIPLY, 1.25F);
        row(editor, "hbm:yellow_barrel", FALLOUT, MULTIPLY, 1.2F);
    }
}
