// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntitySkeletonHolder extends BlockEntity implements Synced, SyncUnitSchema {

    @SyncField(units = 1L)
    public ItemStack item = ItemStack.EMPTY;

    public BlockEntitySkeletonHolder(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NTM_SKELETON.get(), pos, state);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (item.isEmpty() || level == null) return;
        level.addFreshEntity(
                new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, item.copy()));
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!item.isEmpty()) out.store("item", ItemStack.CODEC, item);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        item = in.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    private RegistryFriendlyByteBuf registryBuf(ByteBuf buffer) {
        return new RegistryFriendlyByteBuf(buffer, level.registryAccess());
    }

    @Override
    public long syncUnitMask() {
        return 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit != 0) throw new IllegalArgumentException();
        ItemStack.OPTIONAL_STREAM_CODEC.encode(registryBuf(output), item);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit != 0) throw new IllegalArgumentException();
        item = ItemStack.OPTIONAL_STREAM_CODEC.decode(registryBuf(input));
    }
}
