// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.items.block.ItemBlockTrinket;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntitySnowglobe extends BlockEntity implements Synced, SyncUnitSchema {

    @SyncField(units = 1L)
    public SnowglobeType type = SnowglobeType.NONE;

    public BlockEntitySnowglobe(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_SNOWGLOBE.get(), pos, state);
    }

    public static ItemStack stackOf(SnowglobeType type) {
        return type == SnowglobeType.NONE
                ? new ItemStack(ModBlocks.SNOWGLOBE)
                : ModBlocks.SNOWGLOBES.stack(type);
    }

    public static SnowglobeType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemBlockTrinket<?> trinket
                        && trinket.type instanceof SnowglobeType type
                ? type
                : SnowglobeType.NONE;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        type = SnowglobeType.byOrdinal(in.getByteOr("type", (byte) 0));
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        output.writeByte(type.ordinal());
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        type = SnowglobeType.byOrdinal(input.readByte());
    }
}
