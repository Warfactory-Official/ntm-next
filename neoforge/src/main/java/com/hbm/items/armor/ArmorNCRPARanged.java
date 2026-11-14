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

package com.hbm.items.armor;

import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunBaseNTItem.GunState;
import com.hbm.items.weapon.sedna.GunBaseNTItem.LambdaContext;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ArmorNCRPARanged implements PowerArmorWeapon {

    public static final MagazineBelt rocketSteerMag = new MagazineBelt();
    public static final MagazineBelt rocketMag = new MagazineBelt();

    private static final float DAMAGE = 25F;
    private static final float SIDE_OFFSET = 0.25F;
    private static final int COOLDOWN = 10;

    @Override
    public void primary(ItemStack stack, LambdaContext ctx) {
        fireRocket(stack, ctx, true);
    }

    @Override
    public void secondary(ItemStack stack, LambdaContext ctx) {
        fireRocket(stack, ctx, false);
    }

    public static void fireRocket(ItemStack stack, LambdaContext ctx, boolean steer) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        if (GunBaseNTItem.getState(stack, 0) != GunState.IDLE) return;

        MagazineBelt mag = steer ? rocketSteerMag : rocketMag;
        if (mag.acceptedBullets.isEmpty()) {
            mag.addConfigs(steer ? XFactoryRocket.rocket_ncrpa_steer : XFactoryRocket.rocket_ncrpa);
        }

        BulletConfig cfg = mag.getType(stack, player.getInventory());
        int amount = mag.getAmount(stack, player.getInventory());

        GunBaseNTItem.setState(stack, 0, GunState.COOLDOWN);
        GunBaseNTItem.setTimer(stack, 0, COOLDOWN);

        if (amount <= 0) {
            EntitySounds.play(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.GUN_DRY_FIRE.get(),
                    SoundSource.PLAYERS,
                    1F,
                    1F);
            return;
        }

        mag.consumeAmmo(stack, player.getInventory(), 1);
        float side = SIDE_OFFSET * (player.getRandom().nextBoolean() ? -1F : 1F);
        BulletBaseMK4Entity rocket = new BulletBaseMK4Entity(player, cfg, DAMAGE, 0F, side, 0D, 0D);
        player.level().addFreshEntity(rocket);
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.GUN_ROCKET_FIRE.get(),
                SoundSource.PLAYERS,
                0.5F,
                0.9F + player.getRandom().nextFloat() * 0.2F);
        player.inventoryMenu.broadcastChanges();
    }
}
