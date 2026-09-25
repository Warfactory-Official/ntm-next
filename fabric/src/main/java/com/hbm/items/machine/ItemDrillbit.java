// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemDrillbit extends Item {

    public final EnumDrillType type;

    public ItemDrillbit(Properties properties, EnumDrillType type) {
        super(properties);
        this.type = type;
    }

    public static @Nullable EnumDrillType typeOf(ItemStack stack) {
        return stack.getItem() instanceof ItemDrillbit bit ? bit.type : null;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.translatable("desc.shared.speed", (int) (type.speed * 100) + "%")
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable("desc.item.drillbit.tier", type.tier)
                        .withStyle(ChatFormatting.YELLOW));
        if (type.fortune > 0) {
            adder.accept(
                    Component.translatable("desc.item.drillbit.fortune", type.fortune)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (type.vein)
            adder.accept(
                    Component.translatable("desc.item.drillbit.veinMiner")
                            .withStyle(ChatFormatting.GREEN));
        if (type.silk)
            adder.accept(
                    Component.translatable("desc.item.drillbit.silkTouch")
                            .withStyle(ChatFormatting.GREEN));
    }

    public enum EnumDrillType {
        STEEL(1.0D, 1, 0, false, false),
        STEEL_DIAMOND(1.0D, 1, 2, false, true),
        HSS(1.2D, 2, 0, true, false),
        HSS_DIAMOND(1.2D, 2, 3, true, true),
        DESH(1.5D, 3, 1, true, true),
        DESH_DIAMOND(1.5D, 3, 4, true, true),
        TCALLOY(2.0D, 4, 1, true, true),
        TCALLOY_DIAMOND(2.0D, 4, 4, true, true),
        FERRO(2.5D, 5, 1, true, true),
        FERRO_DIAMOND(2.5D, 5, 4, true, true);

        public static final EnumDrillType[] VALUES = values();
        public final double speed;
        public final int tier;
        public final int fortune;
        public final boolean vein;
        public final boolean silk;
        public final String id = name().toLowerCase(Locale.ROOT);

        EnumDrillType(double speed, int tier, int fortune, boolean vein, boolean silk) {
            this.speed = speed;
            this.tier = tier;
            this.fortune = fortune;
            this.vein = vein;
            this.silk = silk;
        }
    }
}
