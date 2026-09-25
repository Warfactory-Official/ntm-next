// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachineAutocrafter;
import com.hbm.lib.Library;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.IControlReceiverFilter;
import com.hbm.tileentity.machine.BlockEntityMachineAutocrafter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ScreenMachineAutocrafter extends ScreenInfoContainer<MenuMachineAutocrafter> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_autocrafter.png");

    private static final int POWER_X = 17, POWER_Y = 45, POWER_W = 16, POWER_H = 52;
    private static final int POWER_U = 176, POWER_BOTTOM = 97;

    private static final int TOOLTIP_LIFT = 30;

    public ScreenMachineAutocrafter(
            MenuMachineAutocrafter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 240);
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

        BlockEntityMachineAutocrafter be = menu.blockEntity();
        int filled = (int) (be.getPower() * POWER_H / be.getMaxPower());
        if (filled > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    POWER_X,
                    POWER_BOTTOM - filled,
                    POWER_U,
                    POWER_H - filled,
                    POWER_W,
                    filled,
                    256,
                    256);
        }

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.getPower(),
                be.getMaxPower());

        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < BlockEntityMachineAutocrafter.GRID_SIZE; i++) {
                Slot slot = menu.getSlot(i);
                String mode = be.matcher.mode(i);
                if (mode != null && overSlot(slot, mouseX, mouseY)) {
                    graphics.setComponentTooltipForNextFrame(
                            font,
                            List.of(
                                    Component.translatable("desc.shared.rightClickToChange")
                                            .withStyle(ChatFormatting.RED),
                                    ModulePatternMatcher.getLabel(mode)),
                            mouseX,
                            mouseY - TOOLTIP_LIFT);
                }
            }

            Slot result = menu.getSlot(BlockEntityMachineAutocrafter.SLOT_TEMPLATE_RESULT);
            if (result.hasItem() && overSlot(result, mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(
                        font,
                        List.of(
                                Component.translatable("desc.shared.rightClickToChange")
                                        .withStyle(ChatFormatting.RED),
                                Component.literal((be.recipeIndex + 1) + " / " + be.recipeCount)
                                        .withStyle(ChatFormatting.YELLOW)),
                        mouseX,
                        mouseY - TOOLTIP_LIFT);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private boolean overSlot(Slot slot, int mouseX, int mouseY) {
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    public Rect2i patternArea(int index) {
        Slot slot = menu.getSlot(index);
        return new Rect2i(leftPos + slot.x, topPos + slot.y, 16, 16);
    }

    public void sendPattern(int index, ItemStack stack) {
        CompoundTag data = new CompoundTag();
        data.putInt("slot", index);
        IControlReceiverFilter.writeStack(data, stack);
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
    }
}
