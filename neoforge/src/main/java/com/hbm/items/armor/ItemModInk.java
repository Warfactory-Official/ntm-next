// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemModInk extends ItemArmorMod {
    private static final Item[] RED_FLOWERS = {
        Items.POPPY,
        Items.BLUE_ORCHID,
        Items.ALLIUM,
        Items.AZURE_BLUET,
        Items.RED_TULIP,
        Items.ORANGE_TULIP,
        Items.WHITE_TULIP,
        Items.PINK_TULIP,
        Items.OXEYE_DAISY
    };

    public ItemModInk(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.modInk.10ChanceToNullify")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
        adder.accept(
                Component.translatable("desc.item.modInk.flowers")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ")
                        .append(stack.getHoverName())
                        .append(" (10% chance to nullify damage)")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public float modDamage(
            LivingEntity entity, DamageSource source, float amount, ItemStack armor) {
        if (entity.getRandom().nextInt(10) != 0) return amount;
        if (entity.level() instanceof ServerLevel level) {
            if (entity.getRandom().nextInt(10) == 0) entity.spawnAtLocation(level, Items.DANDELION);
            entity.spawnAtLocation(
                    level,
                    new ItemStack(RED_FLOWERS[entity.getRandom().nextInt(RED_FLOWERS.length)]));
        }
        return 0F;
    }
}
