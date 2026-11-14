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
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HELandmineBlock extends LandmineBlock {
    private static final SmallExplosionEffect HE_MINE_EFFECT =
            new SmallExplosionEffect(15, 3.5F, 1.25F);

    private static final VoxelShape HE_MINE_SHAPE = Block.box(4, 0, 4, 12, 2, 12);

    public HELandmineBlock(BlockBehaviour.Properties properties) {
        super(properties, 2D, 5D);
    }

    @Override
    protected VoxelShape shape() {
        return HE_MINE_SHAPE;
    }

    @Override
    protected void explodeVariant(Level level, BlockPos pos) {
        float damage = ExplosionData.MINE_HE_DAMAGE.get().floatValue();
        var sweep = new WeaponExplosionEntityProcessor(1D, damage).piercing(15F, 0.2F);
        var blast =
                new ComposableExplosion(
                        level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 4F);
        blast.blockSelector(new AttenuatedRayBlockSelector());
        blast.blockProcessor(CellDestruction.decay());
        blast.entityProcessor(sweep);
        blast.effects(HE_MINE_EFFECT);
        blast.explode();
    }
}
