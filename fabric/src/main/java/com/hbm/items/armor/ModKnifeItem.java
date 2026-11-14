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

import com.hbm.advancement.HbmCriteria;
import com.hbm.handler.ArmorModHandler;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ProperJoltLayout;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class ModKnifeItem extends ArmorModItem {

    public static final Identifier TRIGAMMA_MODIFIER_ID = RegistryUtil.id("trigamma_health");

    public ModKnifeItem(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, true, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.modKnife.pain").withStyle(ChatFormatting.RED));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable("desc.item.modKnife.hurtsDoesntIt")
                        .withStyle(ChatFormatting.RED));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ").append(stack.getHoverName()).withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean ticksServer() {
        return true;
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide()
                || entity.tickCount % 50 != 0
                || entity.getMaxHealth() <= 2F) return;

        EntitySounds.play(
                entity,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.SLICER.get(),
                entity.getSoundSource(),
                1.0F,
                1.0F);

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        float health = entity.getMaxHealth();
        maxHealth.removeModifier(TRIGAMMA_MODIFIER_ID);
        maxHealth.addPermanentModifier(
                new AttributeModifier(
                        TRIGAMMA_MODIFIER_ID,
                        -(entity.getMaxHealth() - health + 2),
                        AttributeModifier.Operation.ADD_VALUE));

        if (entity instanceof ServerPlayer player) {
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
                player.connection.send(
                        new ClientboundSetHealthPacket(
                                player.getHealth(),
                                player.getFoodData().getFoodLevel(),
                                player.getFoodData().getSaturationLevel()));
            }
            if (player.getMaxHealth() > 2F) {
                PacketWire.sendTo(
                        ProperJoltLayout.encode(10000 + player.getRandom().nextInt(10000), 10000),
                        player);
            } else {
                HbmCriteria.bledOut(player);
                PacketWire.sendTo(ProperJoltLayout.encode(0, 0), player);
            }
        }
    }
}
