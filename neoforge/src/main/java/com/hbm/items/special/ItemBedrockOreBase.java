// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
import com.hbm.items.special.ItemBedrockOreNew.BedrockOreType;
import com.hbm.items.tool.ItemOreDensityScanner;
import com.hbm.world.feature.BedrockOreField;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemBedrockOreBase extends Item {

    public ItemBedrockOreBase(Properties properties) {
        super(properties);
    }

    public static BedrockOreSample sample(ItemStack stack) {
        return stack.getOrDefault(
                ModDataComponents.BEDROCK_ORE_SAMPLE.get(), BedrockOreSample.EMPTY);
    }

    public static double getOreAmount(ItemStack stack, BedrockOreType type) {
        return sample(stack).amount(type);
    }

    public static void setOreAmount(
            ItemStack stack, BedrockOreField field, int x, int z, double multiplier) {
        double richness = field.richness(x, z);
        double lightMetal = field.lightMetal(richness, x, z) * multiplier;
        double heavyMetal = field.heavyMetal(richness, x, z) * multiplier;
        double rareEarth = field.rareEarth(richness, x, z) * multiplier;
        double actinide = field.actinide(richness, x, z) * multiplier;
        double nonMetal = field.nonMetal(richness, x, z) * multiplier;
        double crystalline = field.crystalline(richness, x, z) * multiplier;

        stack.set(
                ModDataComponents.BEDROCK_ORE_SAMPLE.get(),
                new BedrockOreSample(
                        lightMetal, heavyMetal, rareEarth, actinide, nonMetal, crystalline));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        BedrockOreSample sample = sample(stack);
        for (BedrockOreType type : BedrockOreType.VALUES) {
            double amount = sample.amount(type);
            double rounded = ((int) (amount * 100)) / 100D;
            MutableComponent line =
                    Component.translatable("item.hbm.bedrock_ore.type." + type.suffix)
                            .append(Component.literal(": " + rounded + " ("))
                            .append(
                                    Component.translatable(
                                                    ItemOreDensityScanner.translateDensity(amount))
                                            .withStyle(ItemOreDensityScanner.getColor(amount)))
                            .append(Component.literal(")").withStyle(ChatFormatting.GRAY));
            adder.accept(line);
        }
    }
}
