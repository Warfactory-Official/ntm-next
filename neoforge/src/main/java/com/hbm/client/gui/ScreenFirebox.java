// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuFirebox;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.BlockEntityHeaterFirebox;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenFirebox extends ScreenInfoContainer<MenuFirebox> {

    private static final Identifier TEXTURE = Library.id("textures/gui/machine/gui_firebox.png");

    public ScreenFirebox(MenuFirebox menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
        this.titleLabelY = 6;
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

        BlockEntityHeaterFirebox be = firebox();

        int heat = be.heatEnergy * 69 / be.getMaxHeat();
        if (heat > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 81, 28, 176, 0, heat, 5, 256, 256);

        int burn = be.burnTime * 70 / Math.max(be.maxBurnTime, 1);
        if (burn > 0)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 81, 37, 176, 5, burn, 5, 256, 256);

        if (be.wasOn) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 25, 26, 176, 10, 18, 18, 256, 256);
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                80,
                27,
                71,
                7,
                List.of(
                        Component.literal(
                                String.format(Locale.US, "%,d", be.heatEnergy)
                                        + " / "
                                        + String.format(Locale.US, "%,d", be.getMaxHeat())
                                        + "TU")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                80,
                36,
                71,
                7,
                List.of(
                        Component.literal(be.burnHeat + "TU/t"),
                        Component.literal((be.burnTime / SharedConstants.TICKS_PER_SECOND) + "s")));

        if (this.menu.getCarried().isEmpty()
                && this.hoveredSlot != null
                && !this.hoveredSlot.hasItem()
                && (this.hoveredSlot == this.menu.slots.get(0)
                        || this.hoveredSlot == this.menu.slots.get(1))) {
            List<String> bonuses = be.getFuelBonuses();
            if (!bonuses.isEmpty()) {
                List<Component> lines = new ArrayList<>(bonuses.size());
                for (String s : bonuses) lines.add(Component.literal(s));
                graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityHeaterFirebox firebox() {
        return menu.blockEntity();
    }
}
