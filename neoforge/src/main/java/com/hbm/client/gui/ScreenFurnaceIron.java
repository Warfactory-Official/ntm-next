// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFurnaceIron;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityFurnaceIron;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenFurnaceIron extends ScreenInfoContainer<MenuFurnaceIron> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_furnace_iron.png");

    public ScreenFurnaceIron(MenuFurnaceIron menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
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

        BlockEntityFurnaceIron be = menu.blockEntity();

        int p = be.progress * 70 / Math.max(be.processingTime, 1);
        if (p > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 53, 36, 176, 18, p, 5, 256, 256);
        int b = be.burnTime * 70 / Math.max(be.maxBurnTime, 1);
        if (b > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 53, 45, 176, 23, b, 5, 256, 256);
        if (be.canSmelt())
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 70, 16, 176, 0, 18, 18, 256, 256);

        if (menu.getCarried().isEmpty()) {
            if (isHovering(53, 53, 16, 16, mouseX, mouseY)
                    || isHovering(71, 53, 16, 16, mouseX, mouseY)) {
                List<String> desc = be.burnModule.getDesc();
                if (!desc.isEmpty()) {
                    List<Component> lines = new ArrayList<>();
                    for (String s : desc) lines.add(Component.literal(s));
                    graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
                }
            }
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                52,
                35,
                71,
                7,
                List.of(
                        Component.literal(
                                (be.progress * 100 / Math.max(be.processingTime, 1)) + "%")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                52,
                44,
                71,
                7,
                List.of(Component.literal((be.burnTime / SharedConstants.TICKS_PER_SECOND) + "s")));

        super.extractLabels(graphics, mouseX, mouseY);
    }
}
