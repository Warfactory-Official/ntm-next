// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.RenderConfig;
import com.hbm.data.MachineData;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityTowerLarge extends BlockEntityCondenser {

    private static final double PLUME_SPREAD = 3D;

    public BlockEntityTowerLarge(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.TOWER_LARGE.get(),
                pos,
                state,
                MachineData.TOWER_LARGE_INPUT_TANK_SIZE.get(),
                MachineData.TOWER_LARGE_OUTPUT_TANK_SIZE.get());
    }

    @Override
    public void tickClient() {
        if (waterTimer <= 0 || level.getGameTime() % 4 != 0) return;
        if (!RenderConfig.coolingTowerParticles) return;

        RandomSource rand = level.getRandom();
        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(0.5F)
                        .setBaseScale(1F)
                        .setMaxScale(10F)
                        .setLife(750 + rand.nextInt(250))
                        .build();
        level.addParticle(
                opts,
                worldPosition.getX() + 0.5 + rand.nextDouble() * PLUME_SPREAD - PLUME_SPREAD / 2D,
                worldPosition.getY() + 1,
                worldPosition.getZ() + 0.5 + rand.nextDouble() * PLUME_SPREAD - PLUME_SPREAD / 2D,
                0,
                0,
                0);
    }
}
