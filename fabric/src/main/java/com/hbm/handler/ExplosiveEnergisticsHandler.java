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

package com.hbm.handler;

import com.hbm.config.BalanceConfig;
import com.hbm.data.WorldData;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

public final class ExplosiveEnergisticsHandler {

    private ExplosiveEnergisticsHandler() {}

    public static boolean detonate(Level level, BlockPos pos) {
        if (level.isClientSide()
                || !BalanceConfig.enable528
                || !WorldData.EXPLOSIVE_ENERGISTICS.get()) return false;
        var block = level.getBlockState(pos).getBlock();
        if (!"ae2".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) return false;
        level.destroyBlock(pos, false);
        new ComposableExplosion(level, pos, 5)
                .entityProcessor(new WeaponExplosionEntityProcessor(1, 20).piercing(5F, 0.2F))
                .effects(new SmallExplosionEffect(10, 2.5F, 1F))
                .explode();
        return true;
    }
}
