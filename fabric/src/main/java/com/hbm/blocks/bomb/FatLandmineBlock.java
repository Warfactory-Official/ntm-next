/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.bomb;

import com.hbm.data.ExplosionData;
import com.hbm.explosion.MiniNukeAftermath;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FatLandmineBlock extends LandmineBlock {

    private static final VoxelShape SHAPE = Block.box(5, 0, 4, 11, 6, 12);

    public FatLandmineBlock(BlockBehaviour.Properties props) {
        super(props, 2.5D, 1D);
    }

    @Override
    protected VoxelShape shape() {
        return SHAPE;
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        var c = Vec3.atCenterOf(pos);
        float damage = ExplosionData.MINE_NUKE_DAMAGE.get().floatValue();
        new ComposableExplosion(level, c.x, c.y, c.z, 10F)
                .blockSelector(new AttenuatedRayBlockSelector(64))
                .blockProcessor(CellDestruction.decay())
                .entityProcessor(new WeaponExplosionEntityProcessor(2, damage).range(1.5F))
                .explode();

        MiniNukeAftermath.fallout(level, c, 1.5F);
        MiniNukeAftermath.rollCloud(level, c, 0D);
        MiniNukeAftermath.report(level, c);
        MiniNukeAftermath.soundWithoutVibration(level, c, 25F, 0.9F);
    }
}
