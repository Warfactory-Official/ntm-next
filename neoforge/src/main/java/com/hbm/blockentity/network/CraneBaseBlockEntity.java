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
import com.hbm.blocks.network.CraneBlock;
import com.hbm.interfaces.SettingsHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class CraneBaseBlockEntity extends MachineBlockEntity implements SettingsHandler {

    private static final String[] SETTINGS = {"copytool.filter", "copytool.orientation"};

    protected CraneBaseBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    public final Direction getInputSide() {
        return CraneBlock.inputSide(getBlockState());
    }

    public final Direction getOutputSide() {
        return CraneBlock.outputSide(getBlockState());
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (level != null && !level.isClientSide()) directionStateChanged();
    }

    protected void directionStateChanged() {}

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        spillInventory(pos);
        clearContent();
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        var settings = new CompoundTag();
        settings.putInt("inputSide", getInputSide().get3DDataValue());
        settings.putInt("outputSide", getOutputSide().get3DDataValue());
        if (this instanceof GhostFilters filters) settings.put("items", GhostFilters.save(filters));
        return settings;
    }

    @Override
    public void pasteSettings(
            CompoundTag settings, int option, Level level, Player player, BlockPos pos) {
        if (option == 1) {
            var state = getBlockState();
            var input = settings.getInt("inputSide");
            var output = settings.getInt("outputSide");
            if (input.isPresent())
                state = state.setValue(CraneBlock.INPUT, Direction.VALUES[input.get()]);
            if (output.isPresent())
                state = state.setValue(CraneBlock.OUTPUT, Direction.VALUES[output.get()]);
            level.setBlockAndUpdate(pos, state);
        } else if (this instanceof GhostFilters filters) {
            GhostFilters.load(filters, settings.getListOrEmpty("items"));
        }
    }

    @Override
    public String[] getSettingsOptions(Level level, BlockPos pos) {
        return SETTINGS;
    }
}
