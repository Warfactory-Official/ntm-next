// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuNukeGadget;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenNukeGadget extends ScreenInfoContainer<MenuNukeGadget> {

    private static final Identifier TEXTURE = Library.id("textures/gui/weapon/gadgetschematic.png");

    public ScreenNukeGadget(MenuNukeGadget menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        if (lens(1))
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 19, 176.0F, 0.0F, 24, 24, 256, 256);
        if (lens(2))
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 106, 19, 200.0F, 0.0F, 24, 24, 256, 256);
        if (lens(3))
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 43, 176.0F, 24.0F, 24, 24, 256, 256);
        if (lens(4))
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    106,
                    43,
                    200.0F,
                    24.0F,
                    24,
                    24,
                    256,
                    256);
        if (menu.isReady()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    134,
                    35,
                    176.0F,
                    48.0F,
                    16,
                    16,
                    256,
                    256);
        }

        drawInfoPanel(graphics, -16, 16, 16, 16, 2);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                -16,
                16,
                16,
                16,
                -8,
                32,
                lineArray("desc.gui.nukeGadget.desc"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private boolean lens(int slot) {
        return menu.part(slot).is(ModItems.EARLY_EXPLOSIVE_LENSES.get());
    }
}
