// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.items.armor.ArmorDash;
import com.hbm.lib.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public final class DashBarRenderer {

    private static final Identifier MISC = Library.id("textures/misc/overlay_misc.png");

    private static final int WIDTH = 30;
    private static final int HEIGHT = 10;
    private static final int PER_ROW = 3;

    private static final float V_RED = 18F;
    private static final float V_NORMAL = 28F;
    private static final float V_PARTIAL = 38F;
    private static final float V_EMPTY = 48F;
    private static final float V_FLASH = 58F;
    private static final float U_BAR = 76F;

    private static final float FADE_STEP = 0.04F;

    private static final int FLASH_AT = ArmorDash.PER_DASH - 3;

    private static float fade;
    private static int fadeBar = -1;

    private DashBarRenderer() {}

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        HbmPlayerProps props = HbmPlayerProps.getData(player);
        int dashes = props.dashCount;
        if (dashes <= 0) {
            fade = 0F;
            fadeBar = -1;
            return;
        }

        int stamina = props.stamina;
        int filled = stamina / ArmorDash.PER_DASH;
        int partial = stamina % ArmorDash.PER_DASH;

        int posX = 16;
        int posY = mc.getWindow().getGuiScaledHeight() - 42;

        g.blit(RenderPipelines.GUI_TEXTURED, MISC, posX - 10, posY, 107F, 18F, 7, HEIGHT, 256, 256);

        if (filled < dashes && partial >= FLASH_AT) {
            fade = 1F;
            fadeBar = filled;
        }

        for (int bar = 0; bar < dashes; bar++) {
            int x = posX + (WIDTH + 2) * (bar % PER_ROW);
            int y = posY - 12 * (bar / PER_ROW);

            g.blit(
                    RenderPipelines.GUI_TEXTURED,
                    MISC,
                    x,
                    y,
                    U_BAR,
                    V_EMPTY,
                    WIDTH,
                    HEIGHT,
                    256,
                    256);

            float v = V_NORMAL;
            int fill = WIDTH;
            if (filled < bar) {
                v = V_EMPTY;
            } else if (filled == bar) {

                v = bar == 0 ? V_RED : V_PARTIAL;
                fill = (int) (partial * (WIDTH / (float) ArmorDash.PER_DASH));
            }
            if (fill > 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, MISC, x, y, U_BAR, v, fill, HEIGHT, 256, 256);
            }

            if (fade > 0F && bar == fadeBar) {
                g.blit(
                        RenderPipelines.GUI_TEXTURED,
                        MISC,
                        x,
                        y,
                        U_BAR,
                        V_FLASH,
                        WIDTH,
                        HEIGHT,
                        256,
                        256,
                        ARGB.color((int) (fade * 255F), 255, 255, 255));
            }
        }

        if (fade > 0F) fade = Math.max(0F, fade - FADE_STEP);
    }
}
