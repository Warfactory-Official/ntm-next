// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.items.ModItems;
import com.hbm.tileentity.bomb.BlockEntityNukePrototype;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class NukePrototype extends NukeAssemblyBase {

    public NukePrototype(Properties props) {
        super(props);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityNukePrototype(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isSecondaryUseActive() || !stack.is(ModItems.IGNITER.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) explode(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void ignite(Level level, BlockPos pos, int yield, @Nullable Entity detonator) {
        EntityNukeExplosionMK3 blast =
                EntityNukeExplosionMK3.statFacFleija(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, yield);
        if (blast.isRemoved()) return;

        playBoom(level, pos);
        level.addFreshEntity(blast);
        level.addFreshEntity(
                EntityCloudFleija.statFac(level, yield, pos.getX(), pos.getY(), pos.getZ()));
    }
}
