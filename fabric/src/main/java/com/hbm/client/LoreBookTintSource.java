// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.items.special.ItemBookLore;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class LoreBookTintSource implements ItemTintSource {

    public static final LoreBookTintSource COVER = new LoreBookTintSource(true);
    public static final LoreBookTintSource TITLE = new LoreBookTintSource(false);
    public static final MapCodec<LoreBookTintSource> COVER_CODEC = MapCodec.unit(COVER);
    public static final MapCodec<LoreBookTintSource> TITLE_CODEC = MapCodec.unit(TITLE);

    private final boolean cover;

    private LoreBookTintSource(boolean cover) {
        this.cover = cover;
    }

    @Override
    public int calculate(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        if (!(stack.getItem() instanceof ItemBookLore)) return CommonColors.WHITE;
        int color = cover ? ItemBookLore.coverColorOf(stack) : ItemBookLore.titleColorOf(stack);
        return 0xFF000000 | color;
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return cover ? COVER_CODEC : TITLE_CODEC;
    }
}
