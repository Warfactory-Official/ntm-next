// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class StackColor {

    private StackColor() {}

    public static int amplified(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() instanceof BlockItem item) return item.getBlock().defaultMapColor().col;
        return amplify(average(stack));
    }

    private static int average(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState state = new ItemStackRenderState();
        minecraft
                .getItemModelResolver()
                .updateForTopItem(state, stack, ItemDisplayContext.GUI, minecraft.level, null, 0);
        Material.Baked material = state.pickParticleMaterial(RandomSource.create(0L));
        if (material == null) return 0xFFFFFF;
        TextureAtlasSprite sprite = material.sprite();
        if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()))
            return 0xFFFFFF;
        NativeImage image = sprite.contents().originalImage;
        int r = 0, g = 0, b = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int pixel = image.getPixel(x, y);
                r += ARGB.red(pixel);
                g += ARGB.green(pixel);
                b += ARGB.blue(pixel);
            }
        }
        int pixels = image.getWidth() * image.getHeight();
        return ARGB.color(0, r / pixels, g / pixels, b / pixels);
    }

    private static int amplify(int hex) {
        int r = ARGB.red(hex), g = ARGB.green(hex), b = ARGB.blue(hex);
        int max = Math.max(Math.max(1, r), Math.max(g, b));
        return ARGB.color(255, r * 255 / max, g * 255 / max, b * 255 / max);
    }
}
