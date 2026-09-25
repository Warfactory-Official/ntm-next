// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.IFluidContainerItem;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.util.ExperienceUtil;
import com.hbm.util.InventoryUtil;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

public final class MedicalIVItem extends ItemCustomLore implements IFluidContainerItem {

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

        if ((type == Type.EMPTY || type == Type.XP_EMPTY)
                && Services.PLATFORM.isFakePlayer(player)) {
            return InteractionResult.PASS;
        }

        if (type == Type.XP_EMPTY && ExperienceUtil.total(player) < CAPACITY)
            return InteractionResult.PASS;

        ItemStack held =
                InventoryUtil.exchangeHeld(
                        player, player.getItemInHand(hand), new ItemStack(counterpart.get()));

        switch (type) {
            case EMPTY -> {
                float health = Math.max(player.getHealth() - 5F, 0F);
                player.setHealth(health);
                if (health <= 0F) player.die(player.damageSources().magic());
                play(server, player, ModSounds.ITEM_SYRINGE.get());
            }
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

    private static void play(
            ServerLevel level, Player player, net.minecraft.sounds.SoundEvent sound) {
        level.playSound(
                null,
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
        if (held() == null) return IFluidContainerItem.super.emptyContainer(stack);
        return new ItemStack(counterpart.get());
    }

    @Override
    public ItemStack filledContainer(ItemStack stack, FluidStackNTM content) {
        if (held() != null) return IFluidContainerItem.super.filledContainer(stack, content);
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
