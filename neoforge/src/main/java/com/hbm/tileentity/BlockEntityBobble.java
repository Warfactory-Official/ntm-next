// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
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

public class BlockEntityBobble extends BlockEntity implements Synced, SyncUnitSchema {

    @SyncField(units = 1L)
    public BobbleType type = BobbleType.NONE;

    public BlockEntityBobble(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_BOBBLEHEAD.get(), pos, state);
    }

    public static ItemStack stackOf(BobbleType type) {
        return type == BobbleType.NONE
                ? new ItemStack(ModBlocks.BOBBLEHEAD)
                : ModBlocks.BOBBLEHEADS.stack(type);
    }

    public static BobbleType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemBlockTrinket<?> trinket
                        && trinket.type instanceof BobbleType type
                ? type
                : BobbleType.NONE;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putByte("type", (byte) type.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        type = BobbleType.byOrdinal(in.getByteOr("type", (byte) 0));
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
        type = BobbleType.byOrdinal(input.readByte());
    }
}
