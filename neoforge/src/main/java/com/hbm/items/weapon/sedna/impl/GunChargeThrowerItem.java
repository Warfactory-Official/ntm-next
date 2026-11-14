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

package com.hbm.items.weapon.sedna.impl;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.extprop.PlayerState;
import com.hbm.handler.KeybindType;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GunChargeThrowerItem extends GunBaseNTItem {

    public static final String KEY_LASTHOOK = "lasthook";

    public GunChargeThrowerItem(
            WeaponQuality quality, Properties properties, GunConfig.Builder... cfg) {
        super(quality, properties, cfg);
    }

    public static void clientGrappleTick(ItemStack stack) {
        Player player = ClientPlayerAccess.player();
        if (player == null) return;

        Entity e = player.level().getEntity(getLastHook(stack));
        if (e == null
                || !e.isAlive()
                || !(e instanceof BulletBaseMK4Entity hook)
                || hook.config != XFactoryTool.ct_hook
                || hook.velocity >= 0.01) return;

        Vec3 eye = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vec3 vec = e.position().subtract(eye);
        double line = vec.length();
        PlayerState props = PlayerState.getOrCreate(player);

        if (props.held(KeybindType.GUN_PRIMARY)) {
            Vec3 pull = vec.normalize().scale(0.1);
            player.setDeltaMovement(player.getDeltaMovement().add(pull.x, pull.y + 0.04, pull.z));
        } else if (!props.held(KeybindType.GUN_SECONDARY)) {
            Vec3 motion = player.getDeltaMovement();
            Vec3 nextPos =
                    new Vec3(
                            player.getX() + motion.x,
                            player.getY() + player.getEyeHeight() + motion.y,
                            player.getZ() + motion.z);
            Vec3 delta = e.position().subtract(nextPos);
            if (delta.length() > line) {
                delta = delta.normalize().scale(line);
                Vec3 newNext = e.position().subtract(delta);
                Vec3 vel = newNext.subtract(eye);
                if (vel.length() < 3) {
                    player.setDeltaMovement(vel);
                }
            }
        } else {
            player.setDeltaMovement(player.getDeltaMovement().scale(0.5));
        }

        if (player.getDeltaMovement().y > -0.1) player.fallDistance = 0;
    }

    public static boolean anchored(Player player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof GunChargeThrowerItem
                && anchoredHook(player.level(), stack) != null;
    }

    private static @Nullable Entity anchoredHook(Level level, ItemStack stack) {
        Entity e = level.getEntity(getLastHook(stack));
        return e != null
                        && e.isAlive()
                        && e instanceof BulletBaseMK4Entity hook
                        && hook.config == XFactoryTool.ct_hook
                        && hook.velocity < 0.01
                ? e
                : null;
    }

    public static int getLastHook(ItemStack stack) {
        return getValueInt(stack, KEY_LASTHOOK);
    }

    public static void setLastHook(ItemStack stack, int value) {
        setValueInt(stack, KEY_LASTHOOK, value);
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (getState(stack, 0) == GunState.RELOADING) {
            if (getLastHook(stack) != -1) setLastHook(stack, -1);
        }

        if (slot == EquipmentSlot.MAINHAND && entity instanceof Player player) {
            Entity e = anchoredHook(level, stack);
            if (e != null) {
                double line =
                        e.position()
                                .subtract(
                                        player.getX(),
                                        player.getY() + player.getEyeHeight(),
                                        player.getZ())
                                .length();
                if (PlayerState.getOrCreate(player).held(KeybindType.GUN_PRIMARY) && line < 2)
                    e.discard();
            }
        } else {
            if (getLastHook(stack) != -1) setLastHook(stack, -1);
        }
    }
}
