/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.extprop.PlayerState;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.KeybindType;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ArmorDash {

    public static final int PER_DASH = 30;

    private ArmorDash() {}

    public static void tick(Player player, float forward, float strafe) {
        PlayerState props = PlayerState.getOrCreate(player);
        int dashes = dashCount(player);
        props.dashCount = dashes;

        if (dashes * PER_DASH < props.stamina) props.stamina = dashes * PER_DASH;
        if (dashes <= 0) return;

        int stamina = props.stamina;

        if (props.dashCooldown > 0) {
            props.dashCooldown--;
            props.setHeld(KeybindType.DASH, false);
        } else if (props.held(KeybindType.DASH) && stamina >= PER_DASH) {
            dash(player, forward, strafe);
            props.dashCooldown = PlayerState.DASH_COOLDOWN;
            stamina -= PER_DASH;
        }

        if (stamina < dashes * PER_DASH) {
            stamina++;
            if (stamina % PER_DASH == PER_DASH - 1) {
                if (!player.level().isClientSide()) {
                    EntitySounds.play(
                            player,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.TECH_BOOP.get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F + (1F / 12F) * (stamina / PER_DASH));
                }
                stamina++;
            }
        }

        props.stamina = stamina;
    }

    private static void dash(Player player, float forward, float strafe) {
        if (player.level().isClientSide()) {
            int f = (int) Math.signum(forward);
            int s = (int) Math.signum(strafe);
            if (f == 0 && s == 0) f = 1;

            Vec3 look = player.getLookAngle();
            Vec3 left = new Vec3(look.z, 0D, -look.x);
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(
                    motion.x + look.x * f + left.x * s, 0D, motion.z + look.z * f + left.z * s);
            player.fallDistance = 0F;
            return;
        }
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.WEAPON_ROCKET_FLAME.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }

    private static int dashCount(Player player) {
        int count = 0;
        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate) {
            ArmorFullSetBonus bonus = ArmorFullSetBonus.get(plate.suit());
            if (bonus != null
                    && bonus.dashCount() > 0
                    && ArmorSuitEffects.hasFullSet(player, plate.suit()))
                count += bonus.dashCount();
        }
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!ArmorModHandler.isArmor(armor)) continue;
            var mods = ArmorModHandler.mods(armor);
            for (int socket = 0; socket < ArmorModHandler.MOD_SLOTS; socket++) {
                if (mods.item(socket) instanceof ModCloudItem cloud) count += cloud.dashes();
            }
        }
        return count;
    }
}
