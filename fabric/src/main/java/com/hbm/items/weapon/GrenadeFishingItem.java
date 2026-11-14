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

package com.hbm.items.weapon;

import com.hbm.entity.item.ItemBuoyantEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class GrenadeFishingItem extends GenericGrenadeItem {

    public GrenadeFishingItem(Properties properties, int fuse) {
        super(properties, fuse);
    }

    public static ItemStack getRandomLoot(ServerLevel level, BlockPos pos) {
        LootTable table =
                level.getServer()
                        .reloadableRegistries()
                        .getLootTable(BuiltInLootTables.FISHING_FISH);
        List<ItemStack> loot =
                table.getRandomItems(
                        new LootParams.Builder(level)
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                                .create(LootContextParamSets.FISHING));
        return loot.isEmpty() ? ItemStack.EMPTY : loot.getFirst();
    }

    @Override
    public void explode(
            Entity grenade, LivingEntity thrower, Level level, double x, double y, double z) {
        level.explode(null, x, y + 0.25D, z, 3F, Level.ExplosionInteraction.NONE);

        if (!(level instanceof ServerLevel server)) return;

        int iX = Mth.floor(x);
        int iY = Mth.floor(y);
        int iZ = Mth.floor(z);

        for (int i = 0; i < 15; i++) {

            int rX = iX + level.getRandom().nextInt(15) - 7;
            int rY = iY + level.getRandom().nextInt(15) - 7;
            int rZ = iZ + level.getRandom().nextInt(15) - 7;
            BlockPos pos = new BlockPos(rX, rY, rZ);
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof LiquidBlock
                    && state.getFluidState().is(FluidTags.WATER)) {
                ItemStack loot = getRandomLoot(server, pos);
                if (!loot.isEmpty()) {
                    ItemBuoyantEntity item =
                            new ItemBuoyantEntity(level, rX + 0.5, rY + 0.5, rZ + 0.5, loot.copy());
                    item.setDeltaMovement(item.getDeltaMovement().x, 1, item.getDeltaMovement().z);
                    level.addFreshEntity(item);
                }
            }
        }
    }

    @Override
    public int getMaxTimer() {
        return 60;
    }

    @Override
    public double getBounceMod() {
        return 0.5D;
    }
}
