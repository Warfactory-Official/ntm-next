// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockDynamicSlag.BlockEntitySlag;
import com.hbm.inventory.material.Mats.MaterialStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockEntityFoundrySlagtap extends BlockEntityFoundryOutlet {

    public BlockEntityFoundrySlagtap(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_SLAGTAP.get(), pos, state);
    }

    @Override
    protected double dropRange() {
        return 15;
    }

    @Override
    public boolean canAcceptPartialFlow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (!passesGates(side, stack)) return false;
        return clipDown(level, pos) != null;
    }

    @Override
    public @Nullable MaterialStack flow(
            Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (stack == null || stack.material == null || stack.amount <= 0) return null;

        BlockHitResult hit = clipDown(level, pos);
        if (hit == null) return null;

        BlockPos hitPos = hit.getBlockPos();
        BlockPos abovePos = hitPos.above();
        BlockState hitState = level.getBlockState(hitPos);

        boolean didFlow = false;

        if (hitState.is(ModBlocks.SLAG.get())) {
            if (level.getBlockEntity(hitPos) instanceof BlockEntitySlag tile
                    && tile.mat == stack.material) {
                int transfer = Math.min(BlockEntitySlag.MAX_AMOUNT - tile.amount, stack.amount);
                tile.amount += transfer;
                stack.amount -= transfer;
                didFlow = transfer > 0;
                tile.setChanged();
                level.scheduleTick(hitPos, ModBlocks.SLAG.get(), 1);
            }
        } else if (hitState.canBeReplaced()) {
            level.setBlockAndUpdate(hitPos, ModBlocks.SLAG.get().defaultBlockState());
            if (level.getBlockEntity(hitPos) instanceof BlockEntitySlag tile) {
                tile.mat = stack.material;
                int transfer = Math.min(BlockEntitySlag.MAX_AMOUNT, stack.amount);
                tile.amount += transfer;
                stack.amount -= transfer;
                didFlow = transfer > 0;
                tile.setChanged();
                level.scheduleTick(hitPos, ModBlocks.SLAG.get(), 1);
            }
        }

        if (stack.amount > 0 && level.getBlockState(abovePos).canBeReplaced()) {
            level.setBlockAndUpdate(abovePos, ModBlocks.SLAG.get().defaultBlockState());
            if (level.getBlockEntity(abovePos) instanceof BlockEntitySlag tile) {
                tile.mat = stack.material;
                int transfer = Math.min(BlockEntitySlag.MAX_AMOUNT, stack.amount);
                tile.amount += transfer;
                stack.amount -= transfer;
                didFlow = didFlow || transfer > 0;
                tile.setChanged();
                level.scheduleTick(abovePos, ModBlocks.SLAG.get(), 1);
            }
        }

        if (didFlow) {
            recordStream(
                    stack.material.moltenColor,
                    Math.max(1F, worldPosition.getY() - (float) Math.ceil(hitPos.getY())));
        }

        if (stack.amount <= 0) return null;
        return stack;
    }

    private void recordStream(int color, float len) {

        this.setPourEvent(color, len);
    }
}
