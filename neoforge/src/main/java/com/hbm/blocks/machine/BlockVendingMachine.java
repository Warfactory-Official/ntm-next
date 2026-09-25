// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockVendingMachine extends BlockMultiblockCore {

    public final String pool;

    public BlockVendingMachine(Properties properties, String pool) {
        super(properties);
        this.pool = pool;
    }

    @Override
    public int[] getDimensions() {
        return new int[] {1, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!held.is(ModItems.COIN_TOKEN.get())) return InteractionResult.PASS;

        held.shrink(1);
        if (held.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack reward = ItemPool.getStack(ItemPool.POOLS.get(pool).pool, level.getRandom());
        BlockPos clicked = hit.getBlockPos();
        Direction facing = state.getValue(FACING);
        if (!reward.isEmpty()) {
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            clicked.getX() + 0.5 + facing.getStepX() * 0.75,
                            core.getY() + 0.25,
                            clicked.getZ() + 0.5 + facing.getStepZ() * 0.75,
                            reward));
        }
        level.playSound(
                null,
                clicked.getX() + 0.5,
                clicked.getY() + 0.5,
                clicked.getZ() + 0.5,
                ModSounds.GUN_BOLT_OPEN.get(),
                SoundSource.BLOCKS,
                1F,
                0.75F);
        return InteractionResult.SUCCESS;
    }
}
