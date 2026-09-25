// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
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

public class BlockEntityPlushie extends BlockEntity implements Synced, SyncUnitSchema {

    @SyncField(units = 1L)
    public PlushieType type = PlushieType.NONE;

    private static final int SQUISH_TICKS = 11;

    private long squishUntil;

    public BlockEntityPlushie(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_PLUSHIE.get(), pos, state);
    }

    public static ItemStack stackOf(PlushieType type) {
        return type == PlushieType.NONE
                ? new ItemStack(ModBlocks.PLUSHIE)
                : ModBlocks.PLUSHIES.stack(type);
    }

    public static PlushieType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemBlockTrinket<?> trinket
                        && trinket.type instanceof PlushieType type
                ? type
                : PlushieType.NONE;
    }

    public void squish() {
        squishUntil = level.getGameTime() + SQUISH_TICKS;
    }

    public long squishTimer() {
        return Math.max(0L, squishUntil - level.getGameTime());
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        type = PlushieType.byOrdinal(in.getByteOr("type", (byte) 0));
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
        type = PlushieType.byOrdinal(input.readByte());
    }
}
