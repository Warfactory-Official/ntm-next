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
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ModTwoKickItem extends ArmorModItem {

    public ModTwoKickItem(Properties properties) {
        super(properties, ArmorModHandler.SERVOS, false, true, false, false);
    }

    public static void onPunch(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !ArmorModHandler.hasMods(chest)) return;
        ItemStack held = player.getMainHandItem();
        if (carriesAttackDamage(held)) return;
        if (!(ArmorModHandler.modItem(chest, ArmorModHandler.SERVOS)
                == ModItems.BALLISTIC_GAUNTLET.get())) return;
        BulletConfig[] configs = {
            XFactory12ga.g12_bp,
            XFactory12ga.g12_bp_magnum,
            XFactory12ga.g12_bp_slug,
            XFactory12ga.g12,
            XFactory12ga.g12_slug,
            XFactory12ga.g12_flechette,
            XFactory12ga.g12_magnum,
            XFactory12ga.g12_explosive,
            XFactory12ga.g12_phosphorus
        };
        BulletConfig chosen = null;
        for (BulletConfig config : configs) {
            if (consumeShell(player.getInventory(), config)) {
                chosen = config;
                break;
            }
        }
        if (chosen == null) return;
        int projectiles = chosen.projectilesMin;
        if (chosen.projectilesMax > chosen.projectilesMin) {
            projectiles +=
                    player.getRandom().nextInt(chosen.projectilesMax - chosen.projectilesMin);
        }
        for (int index = 0; index < projectiles; index++) {
            BulletBaseMK4Entity bullet =
                    new BulletBaseMK4Entity(player, chosen, 15F, 0F, -0.1875, -0.0625, 0.5);
            level.addFreshEntity(bullet);
            if (index == 0 && chosen.blackPowder) {
                var motion = bullet.getFlightDirection();
                ParticleCreators.blackPowder(
                        level,
                        bullet.getX(),
                        bullet.getY(),
                        bullet.getZ(),
                        motion.x,
                        motion.y,
                        motion.z,
                        10,
                        0.25F,
                        0.5F,
                        10,
                        0.25F);
            }
        }
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.GUN_SPAS_FIRE.get(),
                SoundSource.PLAYERS,
                1F,
                1F);
    }

    private static boolean carriesAttackDamage(ItemStack held) {
        boolean[] found = {false};
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            held.forEachModifier(
                    slot,
                    (attribute, modifier) -> {
                        if (attribute.value() == Attributes.ATTACK_DAMAGE.value()) found[0] = true;
                    });
        }
        return found[0];
    }

    private static boolean consumeShell(Inventory inventory, BulletConfig config) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (config.matchesAmmo(stack)) {
                stack.shrink(1);
                if (stack.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.twoKick.1")
                        .withStyle(ChatFormatting.ITALIC));
        adder.accept(
                Component.translatable("desc.item.armorMod.twoKick.2")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.twoKick.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.YELLOW));
    }
}
