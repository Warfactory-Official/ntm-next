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

package com.hbm.world.feature;

import com.hbm.blockentity.machine.storage.CrateBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.storage.SafeBlock;
import com.hbm.itempool.ComponentLoot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class LockedSafeFeature extends Feature<NoneFeatureConfiguration> {
    public LockedSafeFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos pos = context.origin();
        if (!Block.canSupportCenter(level, pos.below(), Direction.UP)) return false;

        BlockState state =
                ModBlocks.SAFE
                        .get()
                        .defaultBlockState()
                        .setValue(
                                SafeBlock.FACING, Direction.from3DDataValue(random.nextInt(4) + 2));
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity safe)) return false;

        ComponentLoot loot;
        switch (random.nextInt(10)) {
            case 0, 1, 2, 3 -> {
                safe.setPickModifier(1);
                loot = ComponentLoot.VAULT_RUSTY_3_6_UNSCALED;
            }
            case 4, 5, 6 -> {
                safe.setPickModifier(0.1);
                loot = ComponentLoot.VAULT_STANDARD_2_4_UNSCALED;
            }
            case 7, 8 -> {
                safe.setPickModifier(0.02);
                loot = ComponentLoot.VAULT_REINFORCED_1_3_UNSCALED;
            }
            default -> {
                safe.setPickModifier(0.0);
                loot = ComponentLoot.VAULT_UNBREAKABLE_1_2_UNSCALED;
            }
        }
        safe.setLootTable(loot.table(), random.nextLong());
        safe.setPinCode(random.nextInt(999) + 1);
        safe.lock();
        if (random.nextInt(10) < 3) safe.fillWithSpiders();
        return true;
    }
}
