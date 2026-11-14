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

package com.hbm.items.food;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.FluidContainerItem;
import com.hbm.items.special.CustomLoreItem;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import com.hbm.util.ExperienceUtil;
import com.hbm.util.InventoryUtil;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

public final class MedicalIVItem extends CustomLoreItem implements FluidContainerItem {

    private static final int CAPACITY = 100;
    private final Type type;
    private final Supplier<? extends Item> counterpart;

    public MedicalIVItem(Properties properties, Type type, Supplier<? extends Item> counterpart) {
        super(properties);
        this.type = type;
        this.counterpart = counterpart;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        if ((type == Type.EMPTY || type == Type.XP_EMPTY) && (player instanceof FakePlayer)) {
            return InteractionResult.PASS;
        }

        if (type == Type.XP_EMPTY && ExperienceUtil.total(player) < CAPACITY)
            return InteractionResult.PASS;

        if (type == Type.EMPTY) {
            if (!player.hurtServer(
                    server, server.damageSources().source(ModDamageTypes.BLOOD_LOSS), 5F)) {
                return InteractionResult.PASS;
            }
            if (player.isDeadOrDying()) return InteractionResult.SUCCESS;
        }

        ItemStack held =
                InventoryUtil.exchangeHeld(
                        player, player.getItemInHand(hand), new ItemStack(counterpart.get()));

        switch (type) {
            case EMPTY -> play(server, player, ModSounds.ITEM_SYRINGE.get());
            case BLOOD -> {
                player.heal(5F);
                play(server, player, ModSounds.ITEM_RADAWAY.get());
            }
            case XP_EMPTY -> {
                ExperienceUtil.set(player, ExperienceUtil.total(player) - CAPACITY);
                play(server, player, ModSounds.ITEM_SYRINGE.get());
            }
            case XP -> {
                ExperienceUtil.add(player, CAPACITY);
                play(server, player, SoundEvents.EXPERIENCE_ORB_PICKUP);
            }
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(held);
    }

    private static void play(ServerLevel level, Player player, SoundEvent sound) {
        EntitySounds.play(
                player,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                1F,
                1F);
    }

    @Override
    public int capacity(ItemStack stack) {
        return CAPACITY;
    }

    private Fluid held() {
        return switch (type) {
            case BLOOD -> NTMFluids.BLOOD;
            case XP -> NTMFluids.XPJUICE;
            default -> null;
        };
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        Fluid fluid = held();
        return fluid == null ? EMPTY_CONTENT : new FluidStackNTM(fluid, CAPACITY);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        throw new UnsupportedOperationException(
                "Fixed IV-bag item ids must transform through filledContainer/emptyContainer");
    }

    @Override
    public int fill(ItemStack stack, Fluid fluid, int amount, int pressure) {
        return held() == null && fluid == pairFluid() && pressure == 0 && amount >= CAPACITY
                ? CAPACITY
                : 0;
    }

    @Override
    public int drain(ItemStack stack, Fluid fluid, int amount, int pressure) {
        return held() != null && held() == fluid && pressure == 0 && amount >= CAPACITY
                ? CAPACITY
                : 0;
    }

    private Fluid pairFluid() {
        return type == Type.XP || type == Type.XP_EMPTY ? NTMFluids.XPJUICE : NTMFluids.BLOOD;
    }

    @Override
    public boolean canStore(Fluid fluid) {
        return fluid == pairFluid();
    }

    @Override
    public ItemStack emptyContainer(ItemStack stack) {
        if (held() == null) return FluidContainerItem.super.emptyContainer(stack);
        return new ItemStack(counterpart.get());
    }

    @Override
    public ItemStack filledContainer(ItemStack stack, FluidStackNTM content) {
        if (held() != null) return FluidContainerItem.super.filledContainer(stack, content);
        if (content.type() != pairFluid()
                || content.amount() != CAPACITY
                || content.pressure() != 0) {
            throw new IllegalArgumentException(
                    "IV bags only convert to 100mB of their own fluid at zero pressure");
        }
        return new ItemStack(counterpart.get());
    }

    public enum Type {
        EMPTY,
        BLOOD,
        XP_EMPTY,
        XP
    }
}
