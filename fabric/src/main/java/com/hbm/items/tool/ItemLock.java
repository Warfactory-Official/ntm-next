// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityLockableBase;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ItemLock extends ItemKeyPin {

    private final double lockMod;

    public ItemLock(Properties properties, double lockMod) {
        super(properties);
        this.lockMod = lockMod;
    }

    private static BlockEntity lockableAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (MultiblockSurface.isFoldedCell(state)) {
            BlockPos core = MultiblockSurface.coreOfAny(level, pos);
            return core == null ? null : level.getBlockEntity(core);
        }
        return level.getBlockEntity(pos);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (getPins(stack) == 0) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockEntity blockEntity = lockableAt(level, context.getClickedPos());
        if (!(blockEntity instanceof BlockEntityLockableBase lock)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (lock.isLocked()) return InteractionResult.FAIL;

        lock.setPins(getPins(stack));
        lock.lock();
        lock.setMod(lockMod);
        level.playSound(
                null,
                context.getPlayer().getX(),
                context.getPlayer().getY(),
                context.getPlayer().getZ(),
                ModSounds.LOCK_HANG.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
