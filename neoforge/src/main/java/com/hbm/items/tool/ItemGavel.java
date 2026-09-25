// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public final class ItemGavel extends Item {

    private final Type type;

    public ItemGavel(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(target.level() instanceof ServerLevel level)) return;

        if (type == Type.WOOD) {
            level.playSound(
                    null,
                    target.blockPosition(),
                    ModSounds.GAVEL.get(),
                    SoundSource.PLAYERS,
                    3.0F,
                    1.0F);
        } else if (type == Type.LEAD) {
            level.playSound(
                    null,
                    target.blockPosition(),
                    ModSounds.GAVEL.get(),
                    SoundSource.PLAYERS,
                    3.0F,
                    1.0F);
            target.addEffect(new MobEffectInstance(HbmPotion.lead(), 15 * 20, 4));
        } else {
            target.setHealth(target.getHealth() - target.getMaxHealth() / 3.0F);
            level.playSound(
                    null,
                    target.blockPosition(),
                    ModSounds.GAVEL.get(),
                    SoundSource.PLAYERS,
                    3.0F,
                    1.0F);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (type == Type.WOOD) {
            adder.accept(Component.translatable("desc.item.gavel.thunk"));
        } else if (type == Type.LEAD) {
            adder.accept(Component.translatable("desc.item.gavel.youAreHerebySentenced"));
        } else {
            adder.accept(Component.translatable("desc.item.gavel.theJokeItMakes"));
            adder.accept(Component.empty());
            adder.accept(
                    Component.translatable("desc.item.gavel.dealsAsMuchDamage")
                            .withStyle(ChatFormatting.BLUE));
        }
    }

    public enum Type {
        WOOD,
        LEAD,
        DIAMOND
    }
}
