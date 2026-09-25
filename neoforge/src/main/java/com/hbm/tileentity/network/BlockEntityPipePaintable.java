// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.FluidDuctPaintableBlock;
import com.hbm.interfaces.ICopiable;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityPipePaintable extends BlockEntity
        implements Synced, GraphResident, FoldedCoreResident, ICopiable, SyncUnitSchema {

    @SyncField(units = 1L)
    private @Nullable BlockState camo;

    public BlockEntityPipePaintable(BlockPos pos, BlockState state) {
        this(ModBlockEntities.PIPE_PAINTABLE.get(), pos, state);
    }

    protected BlockEntityPipePaintable(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public @Nullable BlockState getCamo() {
        return camo;
    }

    public void setCamo(@Nullable BlockState camo) {
        if (this.camo == camo) return;
        this.camo = camo;
        setChanged();
        syncToTracking();
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
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
        if (state.getBlock() instanceof FluidDuctPaintableBlock
                && !state.getValue(FluidDuctPaintableBlock.PAINTED)) {
            level.setBlock(
                    pos, state.setValue(FluidDuctPaintableBlock.PAINTED, true), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        BlockState old = camo;
        super.loadAdditional(input);
        this.camo = input.read("camo", BlockState.CODEC).orElse(null);
        refreshCamoMesh(old);
    }

    private void refreshCamoMesh(@Nullable BlockState old) {
        if (old == camo || level == null || !level.isClientSide()) return;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (camo != null) output.store("camo", BlockState.CODEC, camo);
    }

    private void writeCamo(ByteBuf output) {
        output.writeInt(camo == null ? -1 : Block.getId(camo));
    }

    private void readCamo(ByteBuf input) {
        BlockState old = camo;
        int id = input.readInt();
        camo = id < 0 ? null : Block.stateById(id);
        refreshCamoMesh(old);
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeCamo(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readCamo(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
