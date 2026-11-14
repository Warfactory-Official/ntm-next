/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.redstoneoverradio.RadioCommandParser;
import com.hbm.api.redstoneoverradio.RadioControlHandler;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.rbmk.RBMKControlBlock;
import com.hbm.data.MachineData;
import com.hbm.interfaces.SettingsHandler;
import com.hbm.inventory.container.RBMKControlMenu;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class RBMKManualControlBlockEntity extends RBMKControlBlockEntity
        implements RadioControlHandler, MenuProvider, SettingsHandler {

    private static final String SET = RadioCommandParser.FUN + "setrods";
    private static final String EXTEND = RadioCommandParser.FUN + "extendrods";
    private static final String[] DESCRIPTORS = {
        EXTRACTION,
        SET + RadioCommandParser.NAME_SEP + "percent",
        EXTEND + RadioCommandParser.NAME_SEP + "percent"
    };
    private static final int UNDYED = -1;

    public @Nullable RodGroup group;
    public double startLevel;

    public RBMKManualControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONTROL.get(), pos, state);
    }

    @Override
    public boolean isModerated() {
        return ((RBMKControlBlock) getBlockState().getBlock()).moderated;
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.CONTROL;
    }

    @Override
    public void setTarget(double level) {
        if (startLevel != extraction) {
            startLevel = extraction;
            markChanged();
        }
        super.setTarget(level);
    }

    @Override
    public double fluxMultiplier() {
        if (target < startLevel && Math.abs(extraction - target) > 0.01D) {
            return extraction
                    + Math.sin(Math.PI * Math.pow(1D - extraction, 15))
                            * (startLevel - target)
                            * MachineData.RBMK_SURGE_MOD.get();
        }
        return extraction;
    }

    public void setGroup(@Nullable RodGroup group) {
        if (this.group == group) return;
        this.hbm$syncSet$RBMKManualControlBlockEntity$group(group);
        markChanged();
    }

    public void toggleGroup(int index) {
        RodGroup picked = RodGroup.VALUES[index];
        setGroup(picked == group ? null : picked);
    }

    @Override
    protected short consoleDye() {
        return (short) (group == null ? UNDYED : group.ordinal());
    }

    @Override
    public String[] getFunctionInfo() {
        return DESCRIPTORS;
    }

    @Override
    public void runRadioFunction(String key, String[] params) {
        if (params.length == 0) return;
        if (SET.equals(key)) {
            setTarget(RadioCommandParser.intParam(params[0], 0, 100) / 100D);
        } else if (EXTEND.equals(key)) {
            setTarget(
                    Math.clamp(
                            target + RadioCommandParser.intParam(params[0], -100, 100) / 100D,
                            0D,
                            1D));
        }
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        var data = new CompoundTag();
        if (group != null) data.putInt("color", group.ordinal());
        return data;
    }

    @Override
    public void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        hbm$syncSet$RBMKManualControlBlockEntity$group(
                settings.getInt("color").map(color -> RodGroup.VALUES[color]).orElse(null));
        markChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkControl");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RBMKControlMenu(id, inventory, this);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int color = input.getIntOr("color", UNDYED);
        if (color < UNDYED || color >= RodGroup.VALUES.length) {
            throw new IllegalArgumentException("Invalid RBMK rod group " + color);
        }
        hbm$syncSet$RBMKManualControlBlockEntity$group(
                color == UNDYED ? null : RodGroup.VALUES[color]);
        startLevel = input.getDoubleOr("startingLevel", extraction);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("color", group == null ? UNDYED : group.ordinal());
        output.putDouble("startingLevel", startLevel);
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("startingLevel", startLevel);
        tag.putDouble("mult", fluxMultiplier());
        if (group != null) tag.putInt("color", group.ordinal());
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 8;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 8) output.writeByte(group == null ? UNDYED : group.ordinal());
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 8) {
            super.readSyncUnit(unit, input);
            return;
        }
        int color = input.readByte();
        if (color < UNDYED || color >= RodGroup.VALUES.length) {
            throw new DecoderException("Invalid RBMK rod group " + color);
        }
        hbm$syncSet$RBMKManualControlBlockEntity$group(
                color == UNDYED ? null : RodGroup.VALUES[color]);
    }

    public final void hbm$syncSet$RBMKManualControlBlockEntity$group(RodGroup next) {
        if (this.group != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.group);
            this.group = next;
            if (bound) bindSyncValue(next, 3, 1L << 8);
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(RBMKManualControlBlockEntity.class, "group", 3, 1L << 8);
    }
}
