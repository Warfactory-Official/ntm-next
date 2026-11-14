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

package com.hbm.blockentity.network;

import com.hbm.blockentity.GhostFilters;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.CraneRouterMenu;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncUnitSchema;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class CraneRouterBlockEntity extends MachineBlockEntity
        implements GhostFilters, MenuProvider, SyncUnitSchema {

    public static final int SIDES = 6;
    public static final int FILTERS_PER_SIDE = 5;
    public static final int SLOT_COUNT = SIDES * FILTERS_PER_SIDE;
    public static final int MODE_NONE = 0;
    public static final int MODE_WHITELIST = 1;
    public static final int MODE_BLACKLIST = 2;
    public static final int MODE_WILDCARD = 3;

    private static final Window FILTER_WINDOW = new Window(0, SLOT_COUNT);
    private static final String[] SETTINGS_OPTIONS = {
        "copytool.pattern0", "copytool.pattern1", "copytool.pattern2",
        "copytool.pattern3", "copytool.pattern4", "copytool.pattern5"
    };

    public final ModulePatternMatcher[] patterns = new ModulePatternMatcher[SIDES];
    public final int[] modes = new int[SIDES];

    public CraneRouterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_ROUTER.get(), pos, state, SLOT_COUNT);
        for (int face = 0; face < SIDES; face++)
            hbm$syncArraySet$CraneRouterBlockEntity$patterns(
                    patterns, face, new ModulePatternMatcher(FILTERS_PER_SIDE));
    }

    @Override
    public Window filterWindow() {
        return FILTER_WINDOW;
    }

    @Override
    public void advanceFilterMode(int slot) {
        patterns[slot / FILTERS_PER_SIDE].nextMode(level, getItem(slot), slot % FILTERS_PER_SIDE);
        publishConfiguration();
    }

    public void initPattern(ItemStack stack, int slot) {
        patterns[slot / FILTERS_PER_SIDE].initPatternSmart(level, stack, slot % FILTERS_PER_SIDE);
        publishConfiguration();
    }

    public void cycleRouteMode(int face) {
        hbm$syncArraySet$CraneRouterBlockEntity$modes(
                modes, face, (modes[face] + 1) & MODE_WILDCARD);
        publishConfiguration();
    }

    private void publishConfiguration() {
        setChanged();
        networkPackNT(15);
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        var settings = GhostFilters.super.copySettings(level, pos);
        settings.putIntArray("modes", modes.clone());
        return settings;
    }

    @Override
    public void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        if (!settings.contains("items") || !settings.contains("modes")) {
            GhostFilters.super.pasteSettings(settings, option, level, player, pos);
            return;
        }
        var copiedModes = validateModes(settings.getIntArray("modes").orElseThrow());
        int first = option * FILTERS_PER_SIDE;
        if (!GhostFilters.load(
                this, settings.getListOrEmpty("items"), first, first + FILTERS_PER_SIDE)) return;
        if (!Arrays.equals(modes, copiedModes)) {
            applyModes(copiedModes);
            publishConfiguration();
        }
    }

    @Override
    public String[] getSettingsOptions(Level level, BlockPos pos) {
        return SETTINGS_OPTIONS;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        clearContent();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneRouter");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new CraneRouterMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int face = 0; face < SIDES; face++)
            patterns[face].save(output.child("pattern" + face));
        output.putIntArray("modes", modes);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int face = 0; face < SIDES; face++)
            patterns[face].load(input.childOrEmpty("pattern" + face));
        var savedModes = input.read("modes", Codec.PASSTHROUGH);
        if (savedModes.isPresent()) {
            var decoded = validateModes(savedModes.get().asIntStream().toArray());
            applyModes(decoded);
        } else {
            for (int face = 0; face < SIDES; face++)
                hbm$syncArraySet$CraneRouterBlockEntity$modes(modes, face, MODE_NONE);
        }
    }

    @Override
    public long syncUnitMask() {
        return 3;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                for (var pattern : patterns) pattern.serialize(output);
            }
            case 1 -> {
                for (int mode : modes) output.writeInt(mode);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                for (var pattern : patterns) pattern.deserialize(input);
            }
            case 1 -> {
                var decoded = new int[SIDES];
                for (int face = 0; face < SIDES; face++) decoded[face] = input.readInt();
                applyModes(validateModes(decoded));
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    private void applyModes(int[] decoded) {
        for (int face = 0; face < SIDES; face++)
            hbm$syncArraySet$CraneRouterBlockEntity$modes(modes, face, decoded[face]);
    }

    private static int[] validateModes(int[] decoded) {
        if (decoded.length != SIDES)
            throw new IllegalArgumentException("route mode count: " + decoded.length);
        for (int mode : decoded) {
            if (mode < MODE_NONE || mode > MODE_WILDCARD)
                throw new IllegalArgumentException("route mode: " + mode);
        }
        return decoded;
    }

    public final ModulePatternMatcher hbm$syncArraySet$CraneRouterBlockEntity$patterns(
            ModulePatternMatcher[] array, int index, ModulePatternMatcher next) {
        if (array[index] != next) {
            if (array == this.patterns && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.patterns) {
                syncArrayChanged(array, index, 3, 1);
            }
        }
        return next;
    }

    public final int hbm$syncArraySet$CraneRouterBlockEntity$modes(
            int[] array, int index, int next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.modes) {
                syncArrayChanged(array, index, 3, 2);
            }
        }
        return next;
    }

    static {
        com.hbm.packet.SyncBindings.field(CraneRouterBlockEntity.class, "patterns", 3, 1);
        com.hbm.packet.SyncBindings.field(CraneRouterBlockEntity.class, "modes", 3, 2);
    }
}
