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

package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.machine.ControlCurve;
import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.interfaces.SettingsHandler;
import com.hbm.inventory.container.RBMKControlAutoMenu;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class RBMKControlAutoBlockEntity extends RBMKControlBlockEntity
        implements MenuProvider, SettingsHandler {
    public ControlCurve function;

    {
        hbm$syncSet$RBMKControlAutoBlockEntity$function(ControlCurve.LINEAR);
    }

    public double levelLower;
    public double levelUpper;
    public double heatLower;
    public double heatUpper;

    public RBMKControlAutoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONTROL_AUTO.get(), pos, state);
    }

    @Override
    public void tickServer() {
        double before = this.target;
        this.hbm$syncSet$RBMKControlBlockEntity$target(
                function.target(heat, heatLower, heatUpper, levelLower, levelUpper));
        if (this.target != before) this.markChanged();

        super.tickServer();
    }

    public void setFunction(ControlCurve function) {
        this.hbm$syncSet$RBMKControlAutoBlockEntity$function(function);
        markChanged();
    }

    public void setCurve(double levelLower, double levelUpper, double heatLower, double heatUpper) {
        if (!Double.isFinite(levelLower)
                || !Double.isFinite(levelUpper)
                || !Double.isFinite(heatLower)
                || !Double.isFinite(heatUpper)) return;
        this.hbm$syncSet$RBMKControlAutoBlockEntity$levelLower(levelLower);
        this.hbm$syncSet$RBMKControlAutoBlockEntity$levelUpper(levelUpper);
        this.hbm$syncSet$RBMKControlAutoBlockEntity$heatLower(heatLower);
        this.hbm$syncSet$RBMKControlAutoBlockEntity$heatUpper(heatUpper);
        markChanged();
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.CONTROL_AUTO;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$RBMKControlAutoBlockEntity$levelLower(
                input.getDoubleOr("levelLower", levelLower));
        hbm$syncSet$RBMKControlAutoBlockEntity$levelUpper(
                input.getDoubleOr("levelUpper", levelUpper));
        hbm$syncSet$RBMKControlAutoBlockEntity$heatLower(input.getDoubleOr("heatLower", heatLower));
        hbm$syncSet$RBMKControlAutoBlockEntity$heatUpper(input.getDoubleOr("heatUpper", heatUpper));
        hbm$syncSet$RBMKControlAutoBlockEntity$function(
                ControlCurve.VALUES[input.getIntOr("function", function.ordinal())]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("levelLower", levelLower);
        output.putDouble("levelUpper", levelUpper);
        output.putDouble("heatLower", heatLower);
        output.putDouble("heatUpper", heatUpper);
        output.putInt("function", function.ordinal());
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        tag.putDouble("levelLower", levelLower);
        tag.putDouble("levelUpper", levelUpper);
        tag.putDouble("heatLower", heatLower);
        tag.putDouble("heatUpper", heatUpper);
        tag.putInt("function", function.ordinal());
    }

    private void writeFunction(ByteBuf output) {
        output.writeByte(function.ordinal());
    }

    private void readFunction(ByteBuf input) {
        int ordinal = input.readUnsignedByte();
        if (ordinal >= ControlCurve.VALUES.length)
            throw new DecoderException("Invalid RBMK function");
        hbm$syncSet$RBMKControlAutoBlockEntity$function(ControlCurve.VALUES[ordinal]);
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putDouble("levelLower", levelLower);
        data.putDouble("levelUpper", levelUpper);
        data.putDouble("heatLower", heatLower);
        data.putDouble("heatUpper", heatUpper);
        data.putInt("function", function.ordinal());
        return data;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        nbt.getDouble("levelLower")
                .ifPresent(v -> hbm$syncSet$RBMKControlAutoBlockEntity$levelLower(v));
        nbt.getDouble("levelUpper")
                .ifPresent(v -> hbm$syncSet$RBMKControlAutoBlockEntity$levelUpper(v));
        nbt.getDouble("heatLower")
                .ifPresent(v -> hbm$syncSet$RBMKControlAutoBlockEntity$heatLower(v));
        nbt.getDouble("heatUpper")
                .ifPresent(v -> hbm$syncSet$RBMKControlAutoBlockEntity$heatUpper(v));
        nbt.getInt("function")
                .ifPresent(
                        value ->
                                hbm$syncSet$RBMKControlAutoBlockEntity$function(
                                        ControlCurve.VALUES[value]));
        markChanged();
        networkPackNT(50);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkControlAuto");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RBMKControlAutoMenu(id, inv, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x1f00L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 8 -> output.writeDouble(this.levelLower);
            case 9 -> output.writeDouble(this.levelUpper);
            case 10 -> output.writeDouble(this.heatLower);
            case 11 -> output.writeDouble(this.heatUpper);
            case 12 -> writeFunction(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 8 -> this.hbm$syncSet$RBMKControlAutoBlockEntity$levelLower(input.readDouble());
            case 9 -> this.hbm$syncSet$RBMKControlAutoBlockEntity$levelUpper(input.readDouble());
            case 10 -> this.hbm$syncSet$RBMKControlAutoBlockEntity$heatLower(input.readDouble());
            case 11 -> this.hbm$syncSet$RBMKControlAutoBlockEntity$heatUpper(input.readDouble());
            case 12 -> readFunction(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$RBMKControlAutoBlockEntity$function(ControlCurve next) {
        if (this.function != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.function);
            this.function = next;
            if (bound) bindSyncValue(next, 3, 1L << 12);
            if ((1L << 12) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 12);
        }
    }

    public final void hbm$syncSet$RBMKControlAutoBlockEntity$levelLower(double next) {
        if (Double.doubleToRawLongBits(this.levelLower) != Double.doubleToRawLongBits(next)) {
            this.levelLower = next;
            if ((1L << 8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 8);
        }
    }

    public final void hbm$syncSet$RBMKControlAutoBlockEntity$levelUpper(double next) {
        if (Double.doubleToRawLongBits(this.levelUpper) != Double.doubleToRawLongBits(next)) {
            this.levelUpper = next;
            if ((1L << 9) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 9);
        }
    }

    public final void hbm$syncSet$RBMKControlAutoBlockEntity$heatLower(double next) {
        if (Double.doubleToRawLongBits(this.heatLower) != Double.doubleToRawLongBits(next)) {
            this.heatLower = next;
            if ((1L << 10) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 10);
        }
    }

    public final void hbm$syncSet$RBMKControlAutoBlockEntity$heatUpper(double next) {
        if (Double.doubleToRawLongBits(this.heatUpper) != Double.doubleToRawLongBits(next)) {
            this.heatUpper = next;
            if ((1L << 11) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 11);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(
                RBMKControlAutoBlockEntity.class, "function", 3, 1L << 12);
    }
}
