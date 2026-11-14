/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blocks.bomb;

import com.hbm.data.ExplosionData;
import com.hbm.explosion.BlastDebris;
import com.hbm.explosion.BlastSmoke;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NavalLandmineBlock extends LandmineBlock {
    private static final SmallExplosionEffect NAVAL_MINE_EFFECT =
            new SmallExplosionEffect(10, 1F, 0.5F);

    public NavalLandmineBlock(BlockBehaviour.Properties properties) {
        super(properties, 2.5D, 1D);
    }

    @Override
    protected VoxelShape shape() {
        return Shapes.block();
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        float damage = ExplosionData.MINE_NAVAL_DAMAGE.get().floatValue();
        var sweep = new WeaponExplosionEntityProcessor(0.5D, damage).piercing(5F, 0.2F);
        var blast =
                new ComposableExplosion(
                        level, pos.getX() + 5D, pos.getY() + 5D, pos.getZ() + 5D, 25F);
        blast.blockSelector(new AttenuatedRayBlockSelector(32, true));
        blast.blockProcessor(CellDestruction.decay());
        blast.entityProcessor(sweep);
        blast.effects(NAVAL_MINE_EFFECT);
        blast.explode();

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        BlastSmoke.radial(level, x, pos.getY() + 2D, z, 30);
        BlastDebris.rubble(level, x, y, z, 5);

        var waterPos = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                waterPos.set(pos.getX() + dx, pos.getY() + 1, pos.getZ() + dz);
                if (level.getBlockState(waterPos).getBlock() == Blocks.WATER) {
                    BlastSmoke.foam(level, x, pos.getY() + 0.5D, z, 60);
                    return;
                }
            }
        }
    }
}
