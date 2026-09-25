// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.config.VersatileConfig;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import com.hbm.util.InventoryUtil;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class MedicalSyringeItem extends ItemCustomLore {

    private static final int MEDX_TICKS = 4 * 60 * SharedConstants.TICKS_PER_SECOND;
    private static final int PSYCHO_TICKS = 2 * 60 * SharedConstants.TICKS_PER_SECOND;
    private static final int TAINT_TICKS = 60 * SharedConstants.TICKS_PER_SECOND;
    private static final int AWESOME_TICKS = 50 * SharedConstants.TICKS_PER_SECOND;
    private static final float POISON_DAMAGE = 30F;
    private static final int BANG_TICKS = 30;

    private final Type type;
    private final Supplier<? extends Item> container;

    public MedicalSyringeItem(
            Properties properties, Type type, Supplier<? extends Item> container) {
        super(properties);
        this.type = type;
        this.container = container;
    }

    private static void clearNegativeEffects(Player player) {
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.NAUSEA);
        player.removeEffect(MobEffects.MINING_FATIGUE);
        player.removeEffect(MobEffects.HUNGER);
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(HbmPotion.radiation());
    }

    private static void give(Player player, ItemStack stack) {
        player.getInventory().placeItemBackInInventory(stack);
    }

    private static void play(ServerLevel level, LivingEntity entity) {
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.ITEM_SYRINGE.get(),
                SoundSource.PLAYERS,
                1F,
                1F);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (type.sicknessGated && VersatileConfig.hasPotionSickness(player))
            return InteractionResult.PASS;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(hand);
        ItemStack held = stack;
        switch (type) {
            case ANTIDOTE -> {
                player.removeAllEffects();
                held = exchange(player, stack);
                VersatileConfig.applyPotionSickness(player, 5);
                play(server, player);
            }
            case STIMPAK -> {
                player.heal(5F);
                held = exchange(player, stack);
                VersatileConfig.applyPotionSickness(player, 5);
                play(server, player);
            }
            case SUPER_STIMPAK -> {
                player.heal(25F);
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.SLOWNESS, 10 * SharedConstants.TICKS_PER_SECOND, 0));
                held = exchange(player, stack);
                VersatileConfig.applyPotionSickness(player, 15);
                play(server, player);
            }
            case MED_BAG -> {
                player.setHealth(player.getMaxHealth());
                clearNegativeEffects(player);
                VersatileConfig.applyPotionSickness(player, 15);
                stack.shrink(1);
            }
            case MEDX -> {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MEDX_TICKS, 2));
                held = exchange(player, stack);
                play(server, player);
                VersatileConfig.applyPotionSickness(player, 5);
            }
            case PSYCHO -> {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, PSYCHO_TICKS, 0));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, PSYCHO_TICKS, 0));
                held = exchange(player, stack);
                play(server, player);
                VersatileConfig.applyPotionSickness(player, 5);
            }
            case TAINT -> {
                taint(player);
                give(player, new ItemStack(ModItems.BOTTLE2_EMPTY.get()));
                held = exchange(player, stack);
                play(server, player);
            }
            case AWESOME -> {
                held = exchange(player, stack);
                play(server, player);
                awesome(player);
                VersatileConfig.applyPotionSickness(player, 5);
            }
            case CBT -> {
                player.addEffect(new MobEffectInstance(HbmPotion.bang(), BANG_TICKS, 0));
                stack.shrink(1);
                server.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.VICE.get(),
                        SoundSource.PLAYERS,
                        1F,
                        1F);
            }
            case POISON -> {
                player.hurtServer(
                        server,
                        server.damageSources()
                                .source(
                                        player.getRandom().nextBoolean()
                                                ? ModDamageTypes.EUTHANIZED_SELF
                                                : ModDamageTypes.EUTHANIZED_SELF_2),
                        POISON_DAMAGE);
                held = exchange(player, stack);
                play(server, player);
            }
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(held);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {

        if (type == Type.MED_BAG
                || type == Type.CBT
                || (type.sicknessGated && VersatileConfig.hasPotionSickness(target))
                || !(attacker instanceof Player player)
                || !(player.level() instanceof ServerLevel server)) return;

        switch (type) {
            case ANTIDOTE -> {
                target.removeAllEffects();
                consumeToInventory(player, stack);
                VersatileConfig.applyPotionSickness(target, 5);
                play(server, target);
            }
            case STIMPAK -> {
                target.heal(5F);
                consumeToInventory(player, stack);
                VersatileConfig.applyPotionSickness(target, 5);
                play(server, target);
            }
            case SUPER_STIMPAK -> {
                target.heal(25F);
                target.addEffect(
                        new MobEffectInstance(
                                MobEffects.SLOWNESS, 10 * SharedConstants.TICKS_PER_SECOND, 0));
                consumeToInventory(player, stack);
                VersatileConfig.applyPotionSickness(target, 15);
                play(server, target);
            }
            case MED_BAG, CBT ->
                    throw new IllegalStateException("Refused above, not injected on a hit");
            case MEDX -> {
                target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MEDX_TICKS, 2));
                VersatileConfig.applyPotionSickness(target, 5);
                consumeToInventory(player, stack);
                play(server, target);
            }
            case PSYCHO -> {
                target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, PSYCHO_TICKS, 0));
                target.addEffect(new MobEffectInstance(MobEffects.STRENGTH, PSYCHO_TICKS, 0));
                VersatileConfig.applyPotionSickness(target, 5);
                consumeToInventory(player, stack);
                play(server, target);
            }
            case TAINT -> {
                taint(target);
                consumeToInventory(player, stack);
                play(server, target);
                give(player, new ItemStack(ModItems.BOTTLE2_EMPTY.get()));
            }
            case AWESOME -> {
                consumeToInventory(player, stack);
                play(server, target);
                awesome(target);
                VersatileConfig.applyPotionSickness(target, 5);
            }
            case POISON -> {
                target.hurtServer(
                        server,
                        server.damageSources().source(ModDamageTypes.EUTHANIZED, player),
                        POISON_DAMAGE);
                consumeToInventory(player, stack);
                play(server, target);
            }
        }
    }

    private static void taint(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(HbmPotion.taint(), TAINT_TICKS, 0));
        entity.addEffect(
                new MobEffectInstance(MobEffects.NAUSEA, 5 * SharedConstants.TICKS_PER_SECOND, 0));
    }

    private static void awesome(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, AWESOME_TICKS, 9));
        entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, AWESOME_TICKS, 9));
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, AWESOME_TICKS, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.STRENGTH, AWESOME_TICKS, 24));
        entity.addEffect(new MobEffectInstance(MobEffects.HASTE, AWESOME_TICKS, 9));
        entity.addEffect(new MobEffectInstance(MobEffects.SPEED, AWESOME_TICKS, 6));
        entity.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, AWESOME_TICKS, 9));
        entity.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, AWESOME_TICKS, 9));
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, AWESOME_TICKS, 4));
        entity.addEffect(
                new MobEffectInstance(MobEffects.NAUSEA, 5 * SharedConstants.TICKS_PER_SECOND, 4));
        entity.addEffect(new MobEffectInstance(HbmPotion.radx(), AWESOME_TICKS, 9));
    }

    private ItemStack exchange(Player player, ItemStack stack) {
        return InventoryUtil.exchangeHeld(player, stack, new ItemStack(container.get()));
    }

    private void consumeToInventory(Player player, ItemStack stack) {
        stack.shrink(1);
        give(player, new ItemStack(container.get()));
    }

    public enum Type {
        ANTIDOTE(true),
        STIMPAK(true),
        SUPER_STIMPAK(true),
        MED_BAG(true),
        MEDX(true),
        PSYCHO(true),
        AWESOME(true),

        TAINT(false),
        POISON(false),
        CBT(false);

        private final boolean sicknessGated;

        Type(boolean sicknessGated) {
            this.sicknessGated = sicknessGated;
        }
    }
}
