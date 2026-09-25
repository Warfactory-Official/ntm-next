// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuNukeMan;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.tileentity.bomb.BlockEntityNukeMan;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenNukeMan extends ScreenInfoContainer<MenuNukeMan> {

    private static final Identifier TEXTURE = Library.id("textures/gui/weapon/fatmanschematic.png");

    public ScreenNukeMan(MenuNukeMan menu, Inventory playerInventory, Component title) {
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

        if (lens(BlockEntityNukeMan.SLOT_LENS_1)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 19, 176.0F, 0.0F, 24, 24, 256, 256);
        }
        if (lens(BlockEntityNukeMan.SLOT_LENS_2)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 106, 19, 200.0F, 0.0F, 24, 24, 256, 256);
        }
        if (lens(BlockEntityNukeMan.SLOT_LENS_3)) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 82, 43, 176.0F, 24.0F, 24, 24, 256, 256);
        }
        if (lens(BlockEntityNukeMan.SLOT_LENS_4)) {
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
        }
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
                lineArray("desc.gui.nukeMan.desc"));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private boolean lens(int slot) {
        return menu.part(slot).is(ModItems.EARLY_EXPLOSIVE_LENSES.get());
    }
}
