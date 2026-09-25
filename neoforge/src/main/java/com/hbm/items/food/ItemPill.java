// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.config.VersatileConfig;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

public class ItemPill extends ItemCustomLore {

    public static final FoodProperties FOOD =
            new FoodProperties.Builder().nutrition(0).saturationModifier(0F).alwaysEdible().build();
    public static final Consumable CONSUMABLE = Consumable.builder().consumeSeconds(0.5F).build();
    public final Type type;
    private final RandomSource random = RandomSource.create();

    public ItemPill(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (VersatileConfig.hasPotionSickness(player)) return InteractionResult.PASS;
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof Player player) || !(level instanceof ServerLevel server))
            return result;

        VersatileConfig.applyPotionSickness(player, 5);
        switch (type) {
            case IODINE -> {
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
            case PLAN_C -> {
                for (int i = 0; i < 10; i++) {
                    player.hurtServer(
                            server,
                            level.damageSources()
                                    .source(
                                            random.nextBoolean()
                                                    ? ModDamageTypes.EUTHANIZED_SELF
                                                    : ModDamageTypes.EUTHANIZED_SELF_2),
                            1_000F);
                }
            }
            case RED ->
                    player.addEffect(
                            new MobEffectInstance(
                                    HbmPotion.death(), 60 * SharedConstants.TICKS_PER_MINUTE, 0));
            case RADX ->
                    player.addEffect(
                            new MobEffectInstance(
                                    HbmPotion.radx(), 3 * SharedConstants.TICKS_PER_MINUTE, 0));
            case SIOX -> clearLungDisease(player);
            case HERBAL -> {
                clearLungDisease(player);
                HbmLivingProps.incrementRadiation(player, -100D);
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.NAUSEA, 10 * SharedConstants.TICKS_PER_SECOND, 0));
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.WEAKNESS, 10 * SharedConstants.TICKS_PER_MINUTE, 2));
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.MINING_FATIGUE,
                                10 * SharedConstants.TICKS_PER_MINUTE,
                                2));
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.POISON, 5 * SharedConstants.TICKS_PER_SECOND, 2));
                player.addEffect(
                        new UncurableEffectInstance(
                                HbmPotion.potionsickness(), 10 * SharedConstants.TICKS_PER_MINUTE));
            }
            case XANAX ->
                    HbmLivingProps.setDigamma(
                            player, Math.max(HbmLivingProps.getDigamma(player) - 0.5D, 0D));
            case CHOCOLATE -> {
                if (random.nextInt(25) == 0) {
                    player.hurtServer(
                            server, level.damageSources().source(ModDamageTypes.OVERDOSE), 1_000F);
                }
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.HASTE, SharedConstants.TICKS_PER_MINUTE, 3));
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.SPEED, SharedConstants.TICKS_PER_MINUTE, 3));
                player.addEffect(
                        new MobEffectInstance(
                                MobEffects.JUMP_BOOST, SharedConstants.TICKS_PER_MINUTE, 3));
            }
            case FMN -> {
                HbmLivingProps.setDigamma(player, Math.min(HbmLivingProps.getDigamma(player), 2D));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
            case FIVE_HTP -> {
                HbmLivingProps.setDigamma(player, 0D);
                player.addEffect(
                        new MobEffectInstance(
                                HbmPotion.stability(), 10 * SharedConstants.TICKS_PER_MINUTE));
            }
        }
        return result;
    }

    private static void clearLungDisease(Player player) {
        HbmLivingProps.setAsbestos(player, 0);
        HbmLivingProps.setBlackLung(
                player,
                Math.min(HbmLivingProps.getBlackLung(player), HbmLivingProps.maxBlacklung / 5));
    }

    public enum Type {
        IODINE,
        PLAN_C,
        RED,
        RADX,
        SIOX,
        HERBAL,
        XANAX,
        CHOCOLATE,
        FMN,
        FIVE_HTP
    }
}
