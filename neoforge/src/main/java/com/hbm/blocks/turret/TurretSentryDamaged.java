// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.turret;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.MachineCaps;
import com.hbm.tileentity.turret.BlockEntityTurretSentry;
import com.hbm.tileentity.turret.BlockEntityTurretSentryDamaged;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class TurretSentryDamaged extends TurretSentry {

    public static final MapCodec<TurretSentryDamaged> CODEC = simpleCodec(TurretSentryDamaged::new);

    public TurretSentryDamaged(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityTurretSentryDamaged(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.TURRET_SENTRY_DAMAGED)
                .powerIn()
                .items()
                .faces(BlockEntityTurretSentry.class, (be, side) -> side == Direction.DOWN);
    }
}
