// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.client.ModifierKeys;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemScraps extends Item {

    public ItemScraps(Properties properties) {
        super(properties);
    }

    public static @Nullable MaterialStack getMats(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemScraps)) return null;

        ScrapData data = stack.get(ModDataComponents.SCRAP.get());
        if (data == null) return null;

        NTMMaterial mat = Mats.matById.get(data.material());
        if (mat == null) return null;
        return new MaterialStack(mat, data.amount());
    }

    public static ItemStack create(MaterialStack stack) {
        return create(stack, false);
    }

    public static ItemStack create(MaterialStack stack, boolean liquid) {
        ItemStack scrap = new ItemStack(ModItems.SCRAPS);
        scrap.set(
                ModDataComponents.SCRAP.get(),
                new ScrapData(stack.material.id, stack.amount, liquid));
        return scrap;
    }

    public static boolean isLiquid(ItemStack stack) {
        ScrapData data = stack.get(ModDataComponents.SCRAP.get());
        return data != null && data.liquid();
    }

    @Override
    public Component getName(ItemStack stack) {
        MaterialStack contents = getMats(stack);
        if (contents != null) {
            Component material = Component.translatable(contents.material.getUnlocalizedName());
            return isLiquid(stack) ? material : Component.translatable("item.hbm.scraps", material);
        }
        return Component.translatable("desc.item.scraps.foundryScraps");
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        MaterialStack contents = getMats(stack);
        if (contents != null) {
            adder.accept(
                    Component.literal(
                            Mats.formatAmount(contents.amount, ModifierKeys.leftShiftHeld())));

            if (isLiquid(stack) && contents.material.smeltable == SmeltingBehavior.ADDITIVE) {
                adder.accept(
                        Component.translatable("desc.item.scraps.additiveNotCastable")
                                .withStyle(ChatFormatting.DARK_RED));
            }
        }
    }

    public record ScrapData(int material, int amount, boolean liquid) {

        public static final Codec<ScrapData> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.INT
                                                        .fieldOf("material")
                                                        .forGetter(ScrapData::material),
                                                Codec.INT
                                                        .fieldOf("amount")
                                                        .forGetter(ScrapData::amount),
                                                Codec.BOOL
                                                        .optionalFieldOf("liquid", false)
                                                        .forGetter(ScrapData::liquid))
                                        .apply(i, ScrapData::new));

        public static final StreamCodec<ByteBuf, ScrapData> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT,
                        ScrapData::material,
                        ByteBufCodecs.VAR_INT,
                        ScrapData::amount,
                        ByteBufCodecs.BOOL,
                        ScrapData::liquid,
                        ScrapData::new);
    }
}
