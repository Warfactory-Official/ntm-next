// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.interfaces.RigidPistonStructure;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBaseBlock.class)
public abstract class MixinPistonBaseBlock {

    @ModifyExpressionValue(
            method = "isPushable",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/state/BlockState;hasBlockEntity()Z"))
    private static boolean hbm$multiblocksArePushable(boolean hasBlockEntity, BlockState state) {
        return hasBlockEntity && !(state.getBlock() instanceof RigidPistonStructure);
    }

    @ModifyExpressionValue(
            method = "moveBlocks",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/piston/MovingPistonBlock;newMovingBlockEntity(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;ZZ)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity hbm$carryMovedBlockEntity(
            BlockEntity moving,
            Level level,
            BlockPos pistonPos,
            Direction direction,
            boolean extending) {

        if (level.isClientSide()) return moving;
        if (!(moving instanceof PistonMovingBlockEntity piston) || piston.isSourcePiston())
            return moving;
        if (!(piston.getMovedState().getBlock() instanceof RigidPistonStructure)) return moving;

        Direction push = extending ? direction : direction.getOpposite();
        BlockEntity source =
                level.getBlockEntity(piston.getBlockPos().relative(push.getOpposite()));
        if (source != null) piston.hbm$captureFrom(source);
        return moving;
    }

    @WrapOperation(
            method = "moveBlocks",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean hbm$relocateRatherThanDestroy(
            Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        if (state.isAir()
                && (flags & Block.UPDATE_MOVE_BY_PISTON) != 0
                && (flags & Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS) == 0
                && level.getBlockState(pos).getBlock() instanceof RigidPistonStructure) {
            flags |= Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
        }
        return original.call(level, pos, state, flags);
    }
}
