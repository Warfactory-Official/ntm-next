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

package com.hbm.blockentity.network.pneumatic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.pneumatic.PneumoTubePaintableBlock;
import com.hbm.interfaces.SettingsHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class PneumoTubePaintableBlockEntity extends PneumaticTubeBlockEntity
        implements SettingsHandler {
    private @Nullable BlockState camo;

    public PneumoTubePaintableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_TUBE_PAINTABLE.get(), pos, state);
    }

    public @Nullable BlockState getCamo() {
        return camo;
    }

    public void setCamo(@Nullable BlockState camo) {
        if (this.camo == camo) return;
        this.hbm$syncSet$PneumoTubePaintableBlockEntity$camo(camo);
        setChanged();
        syncToTracking();
    }

    private void writeCamo(ByteBuf output) {
        output.writeInt(camo == null ? -1 : Block.getId(camo));
    }

    private void readCamo(ByteBuf input) {
        BlockState old = camo;
        int id = input.readInt();
        this.hbm$syncSet$PneumoTubePaintableBlockEntity$camo(id < 0 ? null : Block.stateById(id));
        refreshCamoMesh(old);
    }

    private void refreshCamoMesh(@Nullable BlockState old) {
        if (old == camo || level == null || !level.isClientSide()) return;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 0);
    }

    @Override
    public CompoundTag copySettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        if (camo != null) nbt.putInt("paintblock", Block.getId(camo));
        return nbt;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (!nbt.contains("paintblock")) return;
        setCamo(Block.stateById(nbt.getIntOr("paintblock", 0)));
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PneumoTubePaintableBlock
                && !state.getValue(PneumoTubePaintableBlock.PAINTED)) {
            level.setBlock(
                    pos, state.setValue(PneumoTubePaintableBlock.PAINTED, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        BlockState old = camo;
        super.loadAdditional(input);
        this.hbm$syncSet$PneumoTubePaintableBlockEntity$camo(
                input.read("camo", BlockState.CODEC).orElse(null));
        refreshCamoMesh(old);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (camo != null) output.store("camo", BlockState.CODEC, camo);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 7;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 7 -> writeCamo(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 7 -> readCamo(input);
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$PneumoTubePaintableBlockEntity$camo(BlockState next) {
        if (this.camo != next) {
            boolean bound = syncBound();
            if (bound) com.hbm.packet.SyncBindings.unbind(this, this.camo);
            this.camo = next;
            if (bound) bindSyncValue(next, 3, 1L << 7);
            if ((1L << 7) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 7);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(PneumoTubePaintableBlockEntity.class, "camo", 3, 1L << 7);
    }
}
