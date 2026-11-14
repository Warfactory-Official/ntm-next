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

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import com.hbm.interfaces.TriggerHandler;
import com.hbm.interfaces.TriggerResult;
import com.hbm.particle.helper.ExplosionCreator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class BombFlameWarBlock extends Block implements TriggerHandler {

    public BombFlameWarBlock(Properties props) {
        super(props);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) trigger(level, pos, null);
    }

    @Override
    public TriggerResult trigger(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return TriggerResult.DETONATED;

        level.destroyBlock(pos, false);
        for (int i = 0; i < 150; i++) {
            ComposableExplosion blast =
                    new ComposableExplosion(
                            level,
                            pos.getX() + level.getRandom().nextInt(51) - 25,
                            pos.getY() + level.getRandom().nextInt(11) - 5,
                            pos.getZ() + level.getRandom().nextInt(51) - 25,
                            4F,
                            null);
            blast.entityProcessor(new WeaponExplosionEntityProcessor(1D, 25F));
            blast.effects(new ExplosionEffectTiny());
            blast.explode();
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        ComposableExplosion blast = new ComposableExplosion(level, x, y, z, 15F);
        blast.blockSelector(new AttenuatedRayBlockSelector(32));
        blast.blockProcessor(CellDestruction.noDrops());
        blast.entityProcessor(new WeaponExplosionEntityProcessor(2D, 200F));
        blast.explode();
        ExplosionCreator.composeEffectSmall(level, x, y, z);

        return TriggerResult.DETONATED;
    }
}
