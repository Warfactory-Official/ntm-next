// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.projectile.EntityB92Beam;
import com.hbm.items.ModDataComponents;
import com.hbm.main.Polaroid;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class GunB92 extends Item {

    public static final int ANIMATION_END = 30;
    public static final int RELOAD_FRAME = 15;
    public static final int MAX_POWER = 10;

    public GunB92(Properties properties) {
        super(properties);
    }

    @Override
    public boolean releaseUsing(
            ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player) || player.isShiftKeyDown()) return false;
        if (getUseDuration(stack, entity) - remainingTime < 10) return false;

        if (level instanceof ServerLevel server) {
            RandomSource random = server.getRandom();
            for (int i = 0; i < getPower(stack); i++) {
                EntityB92Beam beam = new EntityB92Beam(ModEntities.BEAM_BOMB.get(), player, 3.0F);
                float divergence = Math.min(i * 0.2F, 1F);
                if (i > 0) {
                    beam.setDeltaMovement(
                            beam.getDeltaMovement()
                                    .add(
                                            random.nextGaussian() * divergence,
                                            random.nextGaussian() * divergence,
                                            random.nextGaussian() * divergence));
                }
                server.addFreshEntity(beam);
            }
            server.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.GUN_SPARK_SHOOT.get(),
                    SoundSource.PLAYERS,
                    5.0F,
                    1.0F);
        }

        setAnimation(stack, 1);
        setPower(stack, 0);
        return true;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        int frame = getAnimation(stack);
        if (frame <= 0) return;
        setAnimation(stack, frame < ANIMATION_END ? frame + 1 : 0);
        if (frame != RELOAD_FRAME) return;

        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.GUN_B92_RELOAD.get(),
                SoundSource.PLAYERS,
                2.0F,
                0.9F);
        setPower(stack, getPower(stack) + 1);
        if (getPower(stack) <= MAX_POWER) return;

        setPower(stack, 0);
        EntityNukeExplosionMK3 blast =
                EntityNukeExplosionMK3.statFacFleija(
                        level, entity.getX(), entity.getY(), entity.getZ(), 50);
        if (blast.isRemoved()) return;
        level.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                100.0F,
                level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.addFreshEntity(blast);
        level.addFreshEntity(
                EntityCloudFleijaRainbow.statFac(
                        level, 50, entity.getX(), entity.getY(), entity.getZ()));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() && getPower(stack) > 0) {
            if (getAnimation(stack) == 0) player.startUsingItem(hand);
        } else if (getAnimation(stack) == 0) {
            setAnimation(stack, 1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (Polaroid.id() == 11) {
            adder.accept(Component.translatable("desc.item.gunB92.aWeaponThat"));
            adder.accept(Component.translatable("desc.item.gunB92.itScreamsFor"));
        } else if (Polaroid.id() == 18) {
            adder.accept(Component.translatable("desc.item.gunB92.oneCouldTurn"));
            adder.accept(Component.translatable("desc.item.gunB92.byOverloadingThe"));
        } else {
            adder.accept(Component.translatable("desc.item.gunB92.stayAwayFrom"));
        }
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.gunB92.projectilesExplodeOn"));
        adder.accept(Component.translatable("desc.item.gunB92.sneakWhileHolding"));
        adder.accept(Component.translatable("desc.item.gunB92.toChargeAdditional"));
        adder.accept(Component.translatable("desc.item.gunB92.theMoreEnergy"));
        adder.accept(Component.translatable("desc.item.gunB92.theBeamsBecome"));
        adder.accept(Component.translatable("desc.item.gunB92.onlyUpToTen"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.gunB92.itsNerfOr"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.gunB92.legendaryWeapon"));
    }

    public static int getAnimation(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.B92_ANIMATION.get(), 0);
    }

    public static void setAnimation(ItemStack stack, int frame) {
        stack.set(ModDataComponents.B92_ANIMATION.get(), frame);
    }

    public static int getPower(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.B92_ENERGY.get(), 0);
    }

    public static void setPower(ItemStack stack, int power) {
        stack.set(ModDataComponents.B92_ENERGY.get(), power);
    }

    public static float getRotationFromAnim(int frame) {
        float rad = 0.0174533F * 7.5F;
        if (frame < 10) return 0;
        int i = frame - 10;
        if (i < 6) return rad * i;
        if (i > 14) return rad * (5 - (i - 15));
        return rad * 5;
    }

    public static float getTransFromAnim(int frame) {
        float i = frame;
        if (i < 10) return 0;
        i -= 10;
        if (i > 4 && i < 10) return (i - 5) * 0.05F;
        if (i > 9 && i < 15) return (10 * 0.05F) - ((i - 5) * 0.05F);
        return 0;
    }
}
