/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.module;

import com.hbm.inventory.material.MaterialShape;
import com.hbm.items.ModItems;
import com.hbm.items.special.BedrockOreNewItem;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncSource;
import com.hbm.registration.ItemFamily;
import com.hbm.registration.ItemStates;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ModulePatternMatcher implements SyncSource {
    private final String[] modes;

    public String mode(int index) {
        return modes[index];
    }

    public void mode(int index, String mode) {
        hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, mode);
    }

    public static final String MODE_EXACT = "exact";
    public static final String MODE_WILDCARD = "wildcard";
    public static final String MODE_BEDROCK = "bedrock";
    private static final String[] SMART_SHAPE_PATHS = {
        MaterialShape.INGOT.plural() + "/",
        MaterialShape.BLOCK.plural() + "/",
        MaterialShape.DUST.plural() + "/",
        MaterialShape.NUGGET.plural() + "/",
        MaterialShape.PLATE.plural() + "/",
    };

    public ModulePatternMatcher(int count) {
        modes = new String[count];
    }

    public static boolean sameItemAndState(ItemStack a, ItemStack b) {
        return a.is(b.getItem()) && ItemStates.sameState(a, b);
    }

    public static Component getLabel(String mode) {
        String text =
                switch (mode) {
                    case MODE_EXACT -> "Item and variant match";
                    case MODE_WILDCARD -> "Item matches";
                    case MODE_BEDROCK -> "Item and bedrock grade match";
                    default -> "Item tag matches: " + mode;
                };
        return Component.literal(text).withStyle(ChatFormatting.YELLOW);
    }

    private static boolean matchesTag(ItemStack stack, String mode) {
        @Nullable Identifier id = Identifier.tryParse(mode);
        return id != null && stack.is(TagKey.create(Registries.ITEM, id));
    }

    public void initPatternSmart(Level level, ItemStack stack, int index) {
        if (level.isClientSide()) return;
        if (stack.isEmpty()) {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, null);
            return;
        }

        String selected = null;
        int priority = SMART_SHAPE_PATHS.length;
        var tags = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).tags().iterator();
        while (tags.hasNext()) {
            var identifier = tags.next().location();
            for (int candidate = 0; candidate < priority; candidate++) {
                if (identifier.getPath().startsWith(SMART_SHAPE_PATHS[candidate])) {
                    selected = identifier.toString();
                    priority = candidate;
                    break;
                }
            }
            if (priority == 0) break;
        }
        if (selected != null) {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, selected);
            return;
        }

        initPatternStandard(level, stack, index);
    }

    public void initPatternStandard(Level level, ItemStack stack, int index) {
        if (level.isClientSide()) return;
        if (stack.isEmpty()) {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, null);
        } else if (stack.is(ModItems.BEDROCK_ORE.get())) {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, MODE_BEDROCK);
        } else if (!ItemStates.of(stack.getItem()).isEmpty()
                || ItemFamily.of(stack.getItem()) != null) {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, MODE_EXACT);
        } else {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, MODE_WILDCARD);
        }
    }

    public void nextMode(Level level, ItemStack pattern, int index) {
        if (level.isClientSide()) return;
        if (pattern.isEmpty()) {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, null);
            return;
        }

        String mode = modes[index];
        if (mode == null) {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, MODE_EXACT);
        } else if (MODE_EXACT.equals(mode)) {
            hbm$syncArraySet$ModulePatternMatcher$modes(
                    modes,
                    index,
                    pattern.is(ModItems.BEDROCK_ORE.get()) ? MODE_BEDROCK : MODE_WILDCARD);
        } else if (MODE_BEDROCK.equals(mode)) {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, MODE_WILDCARD);
        } else {
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, nextTagMode(pattern, mode));
        }
    }

    private static String nextTagMode(ItemStack pattern, String mode) {
        boolean selectNext = MODE_WILDCARD.equals(mode);
        var tags = BuiltInRegistries.ITEM.wrapAsHolder(pattern.getItem()).tags().iterator();
        while (tags.hasNext()) {
            var tag = tags.next().location().toString();
            if (selectNext) return tag;
            selectNext = tag.equals(mode);
        }
        return MODE_EXACT;
    }

    public boolean isValidForFilter(ItemStack filter, int index, ItemStack input) {
        String mode = modes[index];
        if (mode == null)
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, index, mode = MODE_EXACT);
        return matches(mode, filter, input);
    }

    public static boolean matches(String mode, ItemStack filter, ItemStack input) {
        return switch (mode) {
            case MODE_EXACT -> sameItemAndState(input, filter);
            case MODE_WILDCARD ->
                    input.is(filter.getItem())
                            || ItemFamily.sameFamily(filter.getItem(), input.getItem());
            case MODE_BEDROCK ->
                    input.is(filter.getItem())
                            && input.is(ModItems.BEDROCK_ORE.get())
                            && BedrockOreNewItem.ore(input).grade()
                                    == BedrockOreNewItem.ore(filter).grade();
            default -> matchesTag(input, mode);
        };
    }

    public void load(ValueInput input) {
        for (int i = 0; i < modes.length; i++)
            hbm$syncArraySet$ModulePatternMatcher$modes(
                    modes, i, input.getString("mode" + i).orElse(null));
    }

    public void save(ValueOutput output) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] != null) output.putString("mode" + i, modes[i]);
        }
    }

    public void serialize(ByteBuf buffer) {
        for (String mode : modes)
            ByteBufCodecs.STRING_UTF8.encode(buffer, mode == null ? "" : mode);
    }

    public void deserialize(ByteBuf buffer) {
        for (int i = 0; i < modes.length; i++) {
            String mode = ByteBufCodecs.STRING_UTF8.decode(buffer);
            hbm$syncArraySet$ModulePatternMatcher$modes(modes, i, mode.isEmpty() ? null : mode);
        }
    }

    public final String hbm$syncArraySet$ModulePatternMatcher$modes(
            String[] array, int index, String next) {
        if (array[index] != next) {
            boolean changed = !java.util.Objects.equals(array[index], next);
            array[index] = next;
            if (array == this.modes && changed) {
                syncArrayChanged(array, index, 3, 0L);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(ModulePatternMatcher.class, "modes", 3, 0L);
    }

    private SyncSource hbm$syncOwner;

    private int hbm$syncMask;

    private int hbm$syncBindings;

    private long hbm$syncUnits;

    public final boolean syncBound() {
        return hbm$syncOwner != null;
    }

    public final void syncChanged(int mask) {
        if (hbm$syncOwner == null) return;
        if (hbm$syncUnits == 0) hbm$syncOwner.syncChanged(hbm$syncMask);
        else hbm$syncOwner.syncUnitsChanged(hbm$syncMask, hbm$syncUnits);
    }

    public final void syncUnitsChanged(int mask, long units) {
        syncChanged(mask);
    }

    public final void bindSync(SyncSource owner, int mask) {
        bindSyncUnits(owner, mask, 0);
    }

    public final void bindSyncUnits(SyncSource owner, int mask, long units) {
        if (hbm$syncOwner != null && hbm$syncOwner != owner) {
            throw new IllegalStateException("Mutable sync state has two owners");
        }
        hbm$syncOwner = owner;
        hbm$syncMask |= mask;
        hbm$syncUnits |= units;
        if (hbm$syncBindings++ == 0) SyncBindings.bindFields(this, owner);
    }

    public final void unbindSync(SyncSource owner) {
        assert hbm$syncOwner == owner;
        if (--hbm$syncBindings != 0) return;
        SyncBindings.unbindFields(this, owner);
        hbm$syncOwner = null;
        hbm$syncMask = 0;
        hbm$syncUnits = 0;
    }
}
