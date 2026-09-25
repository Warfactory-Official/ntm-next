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

public class BlockEntityTowerSmall extends BlockEntityCondenser {

    public static final int PLUME_HEIGHT = 18;

    public BlockEntityTowerSmall(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.TOWER_SMALL.get(),
                pos,
                state,
                MachineData.TOWER_SMALL_INPUT_TANK_SIZE.get(),
                MachineData.TOWER_SMALL_OUTPUT_TANK_SIZE.get());
    }

    @Override
    public void tickClient() {
        if (waterTimer <= 0 || level.getGameTime() % 2 != 0) return;
        if (!RenderConfig.coolingTowerParticles) return;

        RandomSource rand = level.getRandom();
        CoolingTowerParticleOptions opts =
                new CoolingTowerParticleOptions.Builder()
                        .setLift(1F)
                        .setBaseScale(0.5F)
                        .setMaxScale(4F)
                        .setLife(250 + rand.nextInt(250))
                        .build();
        level.addParticle(
                opts,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + PLUME_HEIGHT,
                worldPosition.getZ() + 0.5,
                0,
                0,
                0);
    }
}
