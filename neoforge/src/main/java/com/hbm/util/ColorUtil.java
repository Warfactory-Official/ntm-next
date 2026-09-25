// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ColorUtil {

    private static final int[] DYE_RGB = new int[DyeColor.values().length];

    private static final TagKey<Item>[] DYE_TAGS = dyeTags();

    static {
        DYE_RGB[DyeColor.WHITE.ordinal()] = 15790320;
        DYE_RGB[DyeColor.ORANGE.ordinal()] = 15435844;
        DYE_RGB[DyeColor.MAGENTA.ordinal()] = 12801229;
        DYE_RGB[DyeColor.LIGHT_BLUE.ordinal()] = 6719955;
        DYE_RGB[DyeColor.YELLOW.ordinal()] = 14602026;
        DYE_RGB[DyeColor.LIME.ordinal()] = 4312372;
        DYE_RGB[DyeColor.PINK.ordinal()] = 14188952;
        DYE_RGB[DyeColor.GRAY.ordinal()] = 4408131;
        DYE_RGB[DyeColor.LIGHT_GRAY.ordinal()] = 11250603;
        DYE_RGB[DyeColor.CYAN.ordinal()] = 2651799;
        DYE_RGB[DyeColor.PURPLE.ordinal()] = 8073150;
        DYE_RGB[DyeColor.BLUE.ordinal()] = 2437522;
        DYE_RGB[DyeColor.BROWN.ordinal()] = 5320730;
        DYE_RGB[DyeColor.GREEN.ordinal()] = 3887386;
        DYE_RGB[DyeColor.RED.ordinal()] = 11743532;
        DYE_RGB[DyeColor.BLACK.ordinal()] = 1973019;
    }

    private ColorUtil() {}

    @SuppressWarnings("unchecked")
    private static TagKey<Item>[] dyeTags() {
        DyeColor[] colors = DyeColor.values();
        TagKey<Item>[] tags = new TagKey[colors.length];
        for (DyeColor color : colors) {
            tags[color.ordinal()] =
                    TagKey.create(
                            Registries.ITEM,
                            Identifier.parse("c:dyes/" + color.getSerializedName()));
        }
        return tags;
    }

    public static int getColorFromDye(ItemStack stack) {

        DyeColor dyed = stack.get(DataComponents.DYE);
        if (dyed != null) return DYE_RGB[dyed.ordinal()];
        for (int i = 0; i < DYE_TAGS.length; i++) {
            if (stack.is(DYE_TAGS[i])) return DYE_RGB[i];
        }
        return 0;
    }
}
