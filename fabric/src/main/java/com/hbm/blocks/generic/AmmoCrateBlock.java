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

package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.util.RegistryUtil;
import com.hbm.world.NtmWorldgenFields;
import com.hbm.world.WorldgenHash;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AmmoCrateBlock extends Block {
    private static final long CONTENT_DOMAIN =
            WorldgenHash.identifier(RegistryUtil.id("crate_ammo"));

    public AmmoCrateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(ModItems.CROWBAR.get())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        var random = NtmWorldgenFields.get((ServerLevel) level).random(CONTENT_DOMAIN, pos);
        popResource(level, pos, new ItemStack(ModItems.CAP_NUKA.get(), 12 + random.nextInt(21)));
        popResource(
                level,
                pos,
                new ItemStack(ModItems.SYRINGE_METAL_STIMPAK.get(), 1 + random.nextInt(3)));
        dropAmmunition(level, pos, random, CrateContents.SMALL_AMMO, 16, 17);
        dropAmmunition(level, pos, random, CrateContents.ORDNANCE_AMMO, 2, 3);
        if (random.nextInt(10) == 0) {
            popResource(level, pos, new ItemStack(ModItems.SYRINGE_METAL_SUPER.get(), 2));
        }
        LootCrateBlock.finishOpening(level, pos);
        return InteractionResult.SUCCESS;
    }

    private static void dropAmmunition(
            Level level,
            BlockPos pos,
            RandomSource random,
            EnumAmmo[] kinds,
            int minimum,
            int amountChoices) {
        for (var kind : kinds) {
            if (random.nextBoolean()) {
                int amount = minimum + random.nextInt(amountChoices);
                popResource(level, pos, ModItems.AMMO_STANDARD.stack(kind, amount));
            }
        }
    }
}
