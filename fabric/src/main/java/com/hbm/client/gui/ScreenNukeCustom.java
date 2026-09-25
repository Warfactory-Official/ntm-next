// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.explosion.ExplosionNukeCustom;
import com.hbm.inventory.container.MenuNukeCustom;
import com.hbm.lib.Library;
import com.hbm.tileentity.bomb.BlockEntityNukeCustom;
import com.hbm.util.I18nUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenNukeCustom extends ScreenInfoContainer<MenuNukeCustom> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/weapon/gun_bomb_schematic.png");

    private static final int PIP_Y = 89, PIP_SIZE = 18;

    private static final int[] PIP_X = {16, 34, 52, 70, 88, 106, 142};
    private static final int[] PIP_V = {0, 18, 36, 54, 72, 90, 108};

    public ScreenNukeCustom(MenuNukeCustom menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
    }

    public static Component stageHead(String key, float level, float adjusted) {
        return Component.translatable(key + ".head", level, adjusted)
                .withStyle(ChatFormatting.YELLOW);
    }

    private static List<Component> lines(String key) {
        List<Component> out = new ArrayList<>();
        for (String s : I18nUtil.resolveKeyArray(key)) out.add(Component.literal(s));
        return out;
    }

    private static int litStage(BlockEntityNukeCustom.Totals bomb) {
        if (bomb.euph() > 0) return 6;
        if (bomb.schrab() > 0) return 5;
        if (bomb.amat() > 0) return 3;
        if (bomb.hydro() > 0) return 2;
        if (bomb.nuke() > 0) return 1;
        if (bomb.tnt() > 0) return 0;
        return -1;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        BlockEntityNukeCustom.Totals bomb = menu.blockEntity().totals();

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        int lit = litStage(bomb);
        if (lit >= 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + PIP_X[lit],
                    topPos + PIP_Y,
                    176.0F,
                    PIP_V[lit],
                    PIP_SIZE,
                    PIP_SIZE,
                    256,
                    256);
        }

        if (bomb.dirty() > 0
                && bomb.nuke() > 0
                && bomb.amat() == 0
                && bomb.schrab() == 0
                && bomb.euph() == 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    leftPos + PIP_X[4],
                    topPos + PIP_Y,
                    176.0F,
                    PIP_V[4],
                    PIP_SIZE,
                    PIP_SIZE,
                    256,
                    256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityNukeCustom.Totals bomb = menu.blockEntity().totals();

        graphics.text(font, title, (imageWidth - font.width(title)) / 2, 6, -12566464, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 96 + 2, -12566464, false);

        stage(
                graphics,
                mouseX,
                mouseY,
                0,
                "desc.gui.nukeCustom.tnt",
                bomb.tnt(),
                Math.min(bomb.tnt(), ExplosionNukeCustom.MAX_TNT));
        stage(graphics, mouseX, mouseY, 1, "desc.gui.nukeCustom.nuke", bomb.nuke(), bomb.nukeAdj());
        stage(
                graphics,
                mouseX,
                mouseY,
                2,
                "desc.gui.nukeCustom.hydro",
                bomb.hydro(),
                bomb.hydroAdj());
        stage(graphics, mouseX, mouseY, 3, "desc.gui.nukeCustom.amat", bomb.amat(), bomb.amatAdj());
        stage(
                graphics,
                mouseX,
                mouseY,
                4,
                "desc.gui.nukeCustom.dirty",
                bomb.dirty(),
                Math.min(bomb.dirty(), ExplosionNukeCustom.MAX_DIRTY));
        stage(
                graphics,
                mouseX,
                mouseY,
                5,
                "desc.gui.nukeCustom.schrab",
                bomb.schrab(),
                bomb.schrabAdj());

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PIP_X[6],
                PIP_Y,
                PIP_SIZE,
                PIP_SIZE,
                lines("desc.gui.nukeCustom.euph"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void stage(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int pip,
            String key,
            float level,
            float adjusted) {
        List<Component> text = new ArrayList<>();
        text.add(stageHead(key, level, adjusted));
        text.addAll(lines(key));
        drawCustomInfoStat(graphics, mouseX, mouseY, PIP_X[pip], PIP_Y, PIP_SIZE, PIP_SIZE, text);
    }
}
