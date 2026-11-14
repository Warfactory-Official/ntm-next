/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
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
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShrapnelLandmineBlock extends LandmineBlock {
    private static final SmallExplosionEffect SMALL_MINE_EFFECT =
            new SmallExplosionEffect(5, 1F, 0.5F);

    private static final VoxelShape SMALL_MINE_SHAPE = Block.box(5, 0, 5, 11, 1, 11);

    public ShrapnelLandmineBlock(BlockBehaviour.Properties properties) {
        super(properties, 1.5D, 1D);
    }

    @Override
    protected VoxelShape shape() {
        return SMALL_MINE_SHAPE;
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        float damage = ExplosionData.MINE_SHRAP_DAMAGE.get().floatValue();
        var sweep = new WeaponExplosionEntityProcessor(0.5D, damage);
        var blast = new ComposableExplosion(level, x, y, z, 3F);
        blast.entityProcessor(sweep);
        blast.effects(SMALL_MINE_EFFECT);
        blast.explode();

        BlastDebris.shower(level, x, y, z, 0D, 1D, 0D, 45, 0.2D);
        BlastDebris.shrapnel(level, x, y, z, 5);
    }
}
