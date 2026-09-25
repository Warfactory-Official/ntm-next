// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network.pneumatic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.network.pneumatic.PneumoTubePaintableBlock;
import com.hbm.interfaces.ICopiable;
import com.hbm.packet.SyncField;
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

public class BlockEntityPneumoTubePaintable extends BlockEntityPneumoTube implements ICopiable {

    @SyncField(units = 1L << 7)
    private @Nullable BlockState camo;

    public BlockEntityPneumoTubePaintable(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMATIC_TUBE_PAINTABLE.get(), pos, state);
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

    private void writeCamo(ByteBuf output) {
        output.writeInt(camo == null ? -1 : Block.getId(camo));
    }

    private void readCamo(ByteBuf input) {
        BlockState old = camo;
        int id = input.readInt();
        this.camo = id < 0 ? null : Block.stateById(id);
        refreshCamoMesh(old);
    }

    private void refreshCamoMesh(@Nullable BlockState old) {
        if (old == camo || level == null || !level.isClientSide()) return;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, 0);
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
        this.camo = input.read("camo", BlockState.CODEC).orElse(null);
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
}
