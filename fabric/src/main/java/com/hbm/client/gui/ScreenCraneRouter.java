// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuCraneRouter;
import com.hbm.lib.Library;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.BlockEntityCraneRouter;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ScreenCraneRouter extends ScreenInfoContainer<MenuCraneRouter> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/storage/gui_crane_router.png");

    private static final int TOOLTIP_LIFT = 30;

    private static final int BUTTON_X = 7;
    private static final int BUTTON_Y = 16;
    private static final int BUTTON_SIZE = 18;
    private static final int BUTTON_COLUMN_STEP = 222;
    private static final int BUTTON_ROW_STEP = 26;

    public ScreenCraneRouter(MenuCraneRouter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 201);
        this.titleLabelY = 5;
        this.inventoryLabelX = 47;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static int buttonX(int column) {
        return BUTTON_X + column * BUTTON_COLUMN_STEP;
    }

    private static int buttonY(int row) {
        return BUTTON_Y + row * BUTTON_ROW_STEP;
    }

    private static List<Component> modeLines(int mode) {
        return switch (mode) {
            case BlockEntityCraneRouter.MODE_WHITELIST ->
                    List.of(
                            Component.translatable("desc.gui.craneRouter.whitelist"),
                            Component.translatable("desc.gui.craneRouter.routeIfFilterMatches"));
            case BlockEntityCraneRouter.MODE_BLACKLIST ->
                    List.of(
                            Component.translatable("desc.gui.craneRouter.blacklist"),
                            Component.translatable("desc.gui.craneRouter.routeIfFilterDoesn"));
            case BlockEntityCraneRouter.MODE_WILDCARD ->
                    List.of(
                            Component.translatable("desc.gui.craneRouter.wildcard"),
                            Component.translatable("desc.gui.craneRouter.routeIfNoOther"));
            default -> List.of(Component.translatable("desc.gui.craneRouter.off"));
        };
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityCraneRouter router = menu.blockEntity();

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 0, 0, 0.0F, 0.0F, 256, 93, 256, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 39, 93, 39, 93, 176, 108, 256, 256);

        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                int index = column * 3 + row;
                int x = buttonX(column);
                int y = buttonY(row);

                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y,
                        238,
                        93 + router.modes[index] * 18,
                        BUTTON_SIZE,
                        BUTTON_SIZE,
                        256,
                        256);
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        x,
                        y,
                        BUTTON_SIZE,
                        BUTTON_SIZE,
                        modeLines(router.modes[index]));
            }
        }

        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < BlockEntityCraneRouter.SLOT_COUNT; i++) {
                Slot slot = menu.getSlot(i);
                String mode =
                        router.patterns[i / BlockEntityCraneRouter.FILTERS_PER_SIDE].mode(
                                i % BlockEntityCraneRouter.FILTERS_PER_SIDE);

                if (mode != null && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
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
        }

        if (Minecraft.getInstance().hasAltDown()) {
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.getSlot(i);
                graphics.text(
                        font, Component.literal(String.valueOf(i)), slot.x + 2, slot.y, -1, true);
                graphics.text(
                        font,
                        Component.literal(String.valueOf(slot.getContainerSlot())),
                        slot.x + 2,
                        slot.y + 8,
                        0xFFFF8080,
                        true);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();

        for (int column = 0; column < 2; column++) {
            for (int row = 0; row < 3; row++) {
                if (!checkClick(mx, my, buttonX(column), buttonY(row), BUTTON_SIZE, BUTTON_SIZE))
                    continue;

                CompoundTag data = new CompoundTag();
                data.putInt("toggle", column * 3 + row);
                Services.NETWORK.sendToServer(
                        new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
                playClick();
            }
        }

        return super.mouseClicked(event, doubleClick);
    }
}
