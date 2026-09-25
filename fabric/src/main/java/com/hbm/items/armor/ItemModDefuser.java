// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.util.TickPhase;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ItemModDefuser extends ItemArmorMod {
    public ItemModDefuser(Item.Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    public static boolean castrateCreeper(
            Creeper creeper, @Nullable LivingEntity entity, boolean dropItem) {
        creeper.setSwellDir(-1);
        creeper.getEntityData().set(Creeper.DATA_IS_IGNITED, false);
        if (!(creeper.level() instanceof ServerLevel level)) return false;

        SwellGoal swellGoal =
                creeper.getGoalSelector().getAvailableGoals().stream()
                        .map(goal -> goal.getGoal())
                        .filter(SwellGoal.class::isInstance)
                        .map(SwellGoal.class::cast)
                        .findFirst()
                        .orElse(null);
        if (swellGoal == null) return false;

        creeper.getGoalSelector().removeGoal(swellGoal);
        if (dropItem) {
            level.playSound(
                    null,
                    creeper.getX(),
                    creeper.getY(),
                    creeper.getZ(),
                    ModSounds.PIN_BREAK.get(),
                    SoundSource.PLAYERS,
                    1F,
                    1F);
            creeper.spawnAtLocation(level, new ItemStack(ModItems.SAFETY_FUSE));
            DamageSource source =
                    entity != null
                            ? level.damageSources().mobAttack(entity)
                            : level.damageSources().magic();
            creeper.hurtServer(level, source, 1F);

            creeper.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 0, 200));
        }
        HbmLivingProps.setDefused(creeper, true);
        return true;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.modDefuser.defusesNearbyCreepers")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ")
                        .append(stack.getHoverName())
                        .append(" (Defuses creepers)")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity.level() instanceof ServerLevel level) || !TickPhase.every(entity, 20)) return;
        for (Creeper creeper :
                level.getEntitiesOfClass(Creeper.class, entity.getBoundingBox().inflate(5D))) {
            castrateCreeper(creeper, entity, true);
        }
    }
}
