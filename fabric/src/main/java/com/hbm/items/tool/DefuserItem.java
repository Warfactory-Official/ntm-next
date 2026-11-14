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

package com.hbm.items.tool;

import com.hbm.entity.mob.glyphid.GlyphidNuclearEntity;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.SmallExplosionEffect;
import com.hbm.explosion.vanillant.standard.WeaponExplosionEntityProcessor;
import com.hbm.interfaces.ToolType;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ModDefuserItem;
import com.hbm.items.tool.ToolingItem;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DefuserItem extends ToolingItem {

    public DefuserItem(Item.Properties properties, ToolType type) {
        super(properties, type);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {

        if (entity instanceof Creeper creeper) {
            return ModDefuserItem.castrateCreeper(creeper, player, true)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        if (entity instanceof GlyphidNuclearEntity john) {

            if (!player.level().isClientSide()
                    && john.deathTicks > 0
                    && john.level() instanceof ServerLevel server) {
                john.discard();

                ComposableExplosion vnt =
                        new ComposableExplosion(
                                server, john.getX(), john.getY(), john.getZ(), 5F, john);
                vnt.entityProcessor(new WeaponExplosionEntityProcessor(1, 20).piercing(10F, 0.2F));
                vnt.effects(new SmallExplosionEffect(10, 2.5F, 1F));
                vnt.explode();

                ConfettiUtil.gib(john);

                if (ModItems.AMMO_STANDARD != null) {
                    john.spawnAtLocation(
                            server, ModItems.AMMO_STANDARD.stack(EnumAmmo.NUKE_DEMO), 1.5F);
                }
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
