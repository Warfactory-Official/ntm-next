// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class NukeMk5 extends Block {

    private final int radius;

    public NukeMk5(Properties props) {
        this(props, ExplosionData.MAN_RADIUS.get());
    }

    public NukeMk5(Properties props, int radius) {
        super(props);
        this.radius = radius;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) detonate(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) detonate(level, pos, null);
    }

    private void detonate(Level level, BlockPos pos, @Nullable Player detonator) {
        level.removeBlock(pos, false);
        level.playSound(
                null,
                pos,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                1.0F,
                0.9F + level.getRandom().nextFloat() * 0.1F);

        EntityNukeExplosionMK5 mk5 =
                EntityNukeExplosionMK5.statFac(
                        level, radius, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        mk5.setDetonator(detonator);
        level.addFreshEntity(mk5);

        EntityNukeTorex.statFac(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, radius);
    }
}
