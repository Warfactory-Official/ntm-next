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

package com.hbm.items.weapon;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.client.HeldItemAnimations;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.AnimatedItem;
import com.hbm.items.EquipmentChangeListener;
import com.hbm.items.ItemSwingListener;
import com.hbm.items.tool.SwordAbilityItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.BlockDustBurstLayout;
import com.hbm.render.anim.AnimationEnums.ToolAnimation;
import com.hbm.render.anim.AnimationSequence;
import com.hbm.render.anim.BusAnimation;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public final class CrucibleItem extends SwordAbilityItem
        implements AnimatedItem, EquipmentChangeListener, ItemSwingListener {

    public static final int CHARGES = 3;
    public static Consumer<Float> PLAY_SWING;

    public CrucibleItem(Properties properties) {
        super(properties, AvailableAbilities.EMPTY);
    }

    @Override
    public boolean canOperate(ItemStack stack) {
        return stack.getDamageValue() < CHARGES;
    }

    public ItemStack depleted() {
        ItemStack stack = new ItemStack(this);
        stack.setDamageValue(CHARGES);
        return stack;
    }

    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer server) || !canOperate(stack)) return;
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.CRUCIBLE_DEPLOY.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
        AnimatedItem.cue(server, ToolAnimation.EQUIP);
    }

    @Override
    public void onEntitySwing(ServerPlayer player, ItemStack stack) {
        if (canOperate(stack)) AnimatedItem.cue(player, ToolAnimation.SWING);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!canOperate(stack)) {
            if (!attacker.level().isClientSide() && attacker instanceof Player player) {
                player.sendSystemMessage(
                        Component.translatable("desc.item.crucible.noEnergy")
                                .withStyle(ChatFormatting.RED));
            }
            return;
        }
        EntitySounds.play(
                victim,
                victim.getX(),
                victim.getY(),
                victim.getZ(),
                SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                SoundSource.PLAYERS,
                1.0F,
                0.75F + victim.getRandom().nextFloat() * 0.2F);
        if (victim.level() instanceof ServerLevel level && !victim.isAlive()) {
            int count = Math.min((int) Math.ceil(victim.getMaxHealth() / 3.0D), 250) * 4;
            double y = victim.getY() + victim.getBbHeight() * 0.5D;
            PacketWire.sendToAllAround(
                    BlockDustBurstLayout.encode(
                            Block.BLOCK_STATE_REGISTRY.getId(
                                    Blocks.REDSTONE_BLOCK.defaultBlockState()),
                            victim.getX(),
                            y,
                            victim.getZ(),
                            count,
                            0.1D),
                    new TargetPoint(level, victim.getX(), y, victim.getZ(), 50));
        }
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (!canOperate(stack)) return;
        if (!(attacker.level() instanceof ServerLevel level)) return;
        ServerPlayer player = attacker instanceof ServerPlayer server ? server : null;
        int wear = stack.processDurabilityChange(1, level, player);
        if (wear == 0) return;
        int damage = Math.min(stack.getDamageValue() + wear, CHARGES);
        if (player != null) CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, damage);
        stack.setDamageValue(damage);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - stack.getDamageValue() * 13.0F / CHARGES);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(
                Math.max(0.0F, 1.0F - stack.getDamageValue() / (float) CHARGES) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        StringBuilder charge = new StringBuilder();
        for (int i = 2; i >= 0; i--) charge.append(stack.getDamageValue() <= i ? "||||||" : "   ");
        adder.accept(
                Component.translatable("desc.item.crucible.charge", charge.toString())
                        .withStyle(ChatFormatting.RED));
    }

    @Override
    public @Nullable BusAnimation animation(ToolAnimation type, ItemStack stack) {
        if (type == ToolAnimation.EQUIP) {
            return new BusAnimation()
                    .addBus(
                            "GUARD_ROT",
                            new AnimationSequence()
                                    .to(90, 0, 1, 0)
                                    .to(90, 0, 1, 800)
                                    .to(0, 0, 1, 50));
        }
        if (type != ToolAnimation.SWING
                || HeldItemAnimations.getChannelValue("SWING_ROT", 0) != 0) {
            return null;
        }
        var random = ClientPlayerAccess.player().getRandom();
        int offset = random.nextInt(80) - 20;
        PLAY_SWING.accept(0.8F + random.nextFloat() * 0.2F);
        return new BusAnimation()
                .addBus(
                        "SWING_ROT",
                        new AnimationSequence()
                                .to(90 - offset, 90 - offset, 35, 75)
                                .to(90 + offset, 90 - offset, -45, 150)
                                .to(0, 0, 0, 500))
                .addBus(
                        "SWING_TRANS",
                        new AnimationSequence().to(-3, 0, 0, 75).to(8, 0, 0, 150).to(0, 0, 0, 500));
    }
}
