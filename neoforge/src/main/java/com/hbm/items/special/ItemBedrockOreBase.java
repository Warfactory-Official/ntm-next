// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.items.ModDataComponents;
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

    public static double getOreAmount(ItemStack stack, BedrockOreKind type) {
        return sample(stack).amount(type);
    }

    public static void setOreAmount(
            ItemStack stack, BedrockOreField field, int x, int z, double multiplier) {
        setOreAmount(stack, field, BedrockOreSet.NTM, x, z, multiplier);
    }

    public static void setOreAmount(
            ItemStack stack,
            BedrockOreField field,
            BedrockOreSet set,
            int x,
            int z,
            double multiplier) {
        double richness = field.richness(x, z);
        double[] amounts = new double[set.kinds().size()];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = field.oreLevel(richness, x, z, set.kinds().get(i)) * multiplier;
        }
        stack.set(ModDataComponents.BEDROCK_ORE_SAMPLE.get(), new BedrockOreSample(set, amounts));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        BedrockOreSample sample = sample(stack);
        if (sample.set().origin() != null) adder.accept(sample.set().origin());
        for (int i = 0; i < sample.amounts().length; i++) {
            double amount = sample.amounts()[i];
            double rounded = ((int) (amount * 100)) / 100D;
            MutableComponent line =
                    sample.set()
                            .kinds()
                            .get(i)
                            .typeName()
                            .copy()
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
