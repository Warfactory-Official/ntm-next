// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuReactorResearch;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityReactorResearch;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public class ScreenReactorResearch extends ScreenInfoContainer<MenuReactorResearch> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/reactors/gui_research_reactor.png");

    private static final int FIELD_X = 8, FIELD_Y = 99, FIELD_W = 33, FIELD_H = 16;
    private static final int LEVER_X = 44, LEVER_Y = 97, LEVER_W = 11, LEVER_H = 20;
    private static final int PANEL_A_X = -14, PANEL_A_Y = 23;
    private static final int PANEL_B_X = -14, PANEL_B_Y = 61;
    private static final int PANEL_W = 16, PANEL_H = 16;
    private static final int LEVER_HOLD = 15;

    private final NumberDisplay fluxDisplay = new NumberDisplay(14, 25, 0x08FF00).setDigitLength(4);
    private final NumberDisplay heatDisplay = new NumberDisplay(12, 63, 0x08FF00).setDigitLength(3);
    private final NumberDisplay controlDisplay =
            new NumberDisplay(5, 101, 0x08FF00).setDigitLength(3);

    private EditBox field;
    private int leverTimer;

    public ScreenReactorResearch(MenuReactorResearch menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        field =
                new EditBox(
                        font,
                        leftPos + FIELD_X,
                        topPos + FIELD_Y,
                        FIELD_W,
                        FIELD_H,
                        Component.empty());
        field.setBordered(false);
        field.setMaxLength(3);
        field.setValue(String.valueOf((int) (reactor().controlLevel * 100)));

        addWidget(field);
    }

    private BlockEntityReactorResearch reactor() {
        return menu.blockEntity();
    }

    private int controlPercent() {
        try {
            return Mth.clamp(Integer.parseInt(field.getValue()), 0, 100);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected int titleCenterX() {
        return 121;
    }

    @Override
    protected int titleColor() {
        return ARGB.opaque(15066597);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
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
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityReactorResearch be = reactor();

        if (be.controlLevel <= 0.5D) {
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            81 + 36 * x,
                            26 + 36 * y,
                            176.0F,
                            0.0F,
                            8,
                            8,
                            256,
                            256);
                }
            }
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 2; y++) {
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            99 + 36 * x,
                            44 + 36 * y,
                            176.0F,
                            0.0F,
                            8,
                            8,
                            256,
                            256);
                }
            }
        }

        if (leverTimer > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    LEVER_X,
                    LEVER_Y,
                    176.0F,
                    8.0F,
                    LEVER_W,
                    LEVER_H,
                    256,
                    256);
            leverTimer--;
        }

        boolean blink = System.currentTimeMillis() % 1000L < 500L;
        fluxDisplay.draw(graphics, menu.getTotalFlux(), blink);
        heatDisplay.draw(graphics, (int) Math.round(menu.getHeat() * 0.00002D * 980D + 20D), blink);
        controlDisplay.draw(graphics, controlPercent(), blink);

        graphics.text(
                this.font,
                Component.translatable("desc.gui.reactorResearch.flux"),
                6,
                13,
                0xFFE5E5E5,
                false);
        graphics.text(
                this.font,
                Component.translatable("desc.gui.reactorResearch.heat"),
                6,
                51,
                0xFFE5E5E5,
                false);
        graphics.text(
                this.font,
                Component.translatable("desc.gui.reactorResearch.control"),
                6,
                89,
                0xFFE5E5E5,
                false);

        drawInfoPanel(graphics, PANEL_A_X, PANEL_A_Y, PANEL_W, PANEL_H, 3);
        drawInfoPanel(graphics, PANEL_B_X, PANEL_B_Y, PANEL_W, PANEL_H, 2);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_A_X,
                PANEL_A_Y,
                PANEL_W,
                PANEL_H,
                PANEL_A_X + 8,
                PANEL_A_Y + 16,
                List.of(
                        Component.translatable("desc.gui.reactorResearch.theReactorHasTo"),
                        Component.translatable("desc.gui.reactorResearch.inWaterOnIts"),
                        Component.translatable("desc.gui.reactorResearch.theNeutronFluxIs"),
                        Component.translatable(
                                "desc.gui.reactorResearch.adjacentBreedingReactors")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PANEL_B_X,
                PANEL_B_Y,
                PANEL_W,
                PANEL_H,
                PANEL_B_X + 8,
                PANEL_B_Y + 16,
                List.of(
                        Component.translatable("desc.gui.reactorResearch.thisReactorIsFueled"),
                        Component.translatable("desc.gui.reactorResearch.theReactionNeedsA")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();

        controlDisplay.setBlinks(checkClick(x, y, FIELD_X, FIELD_Y, FIELD_W, FIELD_H));

        if (checkClick(x, y, LEVER_X, LEVER_Y, LEVER_W, LEVER_H)) {
            double level;
            try {
                int percent = Mth.clamp(Integer.parseInt(field.getValue()), 0, 100);
                field.setValue(String.valueOf(percent));
                level = percent * 0.01D;
            } catch (NumberFormatException e) {
                return true;
            }

            CompoundTag control = new CompoundTag();
            control.putDouble("level", level);
            leverTimer = LEVER_HOLD;
            Services.NETWORK.sendToServer(new NbtControlPayload(reactor().getBlockPos(), control));
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(ModSounds.RBMK_AZ5_COVER.get(), 0.5F));
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }
}
