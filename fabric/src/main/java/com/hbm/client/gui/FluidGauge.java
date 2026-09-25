// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.lib.Library;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public final class FluidGauge {

    private static final Map<Fluid, Identifier> SHEETS = new ConcurrentHashMap<>();

    private FluidGauge() {}

    public static @Nullable Identifier sheet(Fluid fluid) {
        if (!NTMFluidProperties.isOwn(fluid)) return null;
        return SHEETS.computeIfAbsent(
                fluid,
                own -> Library.id("textures/gui/fluids/" + NTMFluids.spritePath(own) + ".png"));
    }

    public static void vertical(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, Fluid type) {
        Identifier own = sheet(type);
        if (own != null) {
            graphics.blit(own, x, y, x + width, y + height, 0F, width / 16F, 1F - height / 16F, 1F);
            return;
        }
        tileForeign(graphics, type, x, y, width, height, false, true, false);
    }

    public static void hanging(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, Fluid type) {
        Identifier own = sheet(type);
        if (own != null) {
            graphics.blit(own, x, y, x + width, y + height, 0F, width / 16F, 1F, 1F - height / 16F);
            return;
        }
        tileForeign(graphics, type, x, y, width, height, false, false, true);
    }

    public static void horizontal(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, Fluid type) {
        Identifier own = sheet(type);
        if (own != null) {
            graphics.blit(own, x, y, x + width, y + height, 1F, 1F - width / 16F, 0F, height / 16F);
            return;
        }
        tileForeign(graphics, type, x, y, width, height, true, false, false);
    }

    public static void still(
            GuiGraphicsExtractor graphics, Fluid type, int x, int y, int width, int height) {
        FluidState state = type.defaultFluidState();
        FluidModel model = model(state);
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                model.stillMaterial().sprite(),
                x,
                y,
                width,
                height,
                tint(model, state));
    }

    private static FluidModel model(FluidState state) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state);
    }

    private static int tint(FluidModel model, FluidState state) {
        return model.tintSource() == null
                ? -1
                : model.tintSource().color(state.createLegacyBlock());
    }

    private static void tileForeign(
            GuiGraphicsExtractor graphics,
            Fluid type,
            int x,
            int y,
            int width,
            int height,
            boolean mirrorU,
            boolean anchorBottom,
            boolean flipV) {
        FluidState state = type.defaultFluidState();
        FluidModel model = model(state);
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int color = tint(model, state);

        AbstractTexture atlas =
                Minecraft.getInstance().getTextureManager().getTexture(sprite.atlasLocation());
        TextureSetup setup = TextureSetup.singleTexture(atlas.getTextureView(), atlas.getSampler());
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());

        for (int px = 0; px < width; px += 16) {
            int tileW = Math.min(16, width - px);
            float u0 = sprite.getU(mirrorU ? 1F : 0F);
            float u1 = sprite.getU(mirrorU ? 1F - tileW / 16F : tileW / 16F);
            for (int py = 0; py < height; py += 16) {
                int tileH = Math.min(16, height - py);
                float v0 = sprite.getV(flipV ? 1F : anchorBottom ? 1F - tileH / 16F : 0F);
                float v1 = sprite.getV(flipV ? 1F - tileH / 16F : anchorBottom ? 1F : tileH / 16F);
                int ty = anchorBottom ? y + height - py - tileH : y + py;
                graphics.guiRenderState.addGuiElement(
                        new BlitRenderState(
                                RenderPipelines.GUI_TEXTURED,
                                setup,
                                pose,
                                x + px,
                                ty,
                                x + px + tileW,
                                ty + tileH,
                                u0,
                                u1,
                                v0,
                                v1,
                                color,
                                null));
            }
        }
    }
}
