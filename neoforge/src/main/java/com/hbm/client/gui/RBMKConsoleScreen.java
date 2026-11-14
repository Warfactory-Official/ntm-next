/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.gui;

import com.hbm.blockentity.machine.rbmk.ColumnSnapshot;
import com.hbm.blockentity.machine.rbmk.RBMKColumnType;
import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.inventory.container.RBMKConsoleMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RBMKConsoleAssignColumnsLayout;
import com.hbm.packet.layout.RBMKConsoleCompressorsLayout;
import com.hbm.packet.layout.RBMKConsoleCycleScreenLayout;
import com.hbm.packet.layout.RBMKConsoleManualLevelLayout;
import com.hbm.packet.layout.RBMKConsoleRodColorLayout;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.sound.ModSounds;
import com.hbm.util.GameTime;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class RBMKConsoleScreen extends InfoContainerScreen<RBMKConsoleMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/rbmk/gui_rbmk_console.png");
    private static final int CELL = 10;
    private static final int ROW = 15;

    private static final Hotspot GRID = new Hotspot(86, 11, 150, 150);
    private static final Hotspot SELECT_CONTROL = new Hotspot(61, 70, 10, 10);
    private static final Hotspot DESELECT = new Hotspot(72, 70, 10, 10);
    private static final Hotspot COMPRESSOR = new Hotspot(70, 82, 12, 12);
    private static final Hotspot AZ5 = new Hotspot(30, 138, 28, 28);
    private static final Hotspot APPLY_LEVEL = new Hotspot(48, 82, 12, 12);
    private static final Hotspot[] SCREENS =
            IntStream.range(0, 6)
                    .mapToObj(id -> new Hotspot(6 + 40 * (id % 2), 8 + 21 * (id / 2), 18, 18))
                    .toArray(Hotspot[]::new);
    private static final Hotspot[] ASSIGNS =
            IntStream.range(0, 6)
                    .mapToObj(id -> new Hotspot(24 + 40 * (id % 2), 8 + 21 * (id / 2), 18, 18))
                    .toArray(Hotspot[]::new);

    private static final String[] GROUP_NAMES = {"red", "yellow", "green", "blue", "purple"};
    private static final ChatFormatting[] GROUP_FORMATS = {
        ChatFormatting.RED,
        ChatFormatting.YELLOW,
        ChatFormatting.GREEN,
        ChatFormatting.BLUE,
        ChatFormatting.LIGHT_PURPLE
    };
    private static final Hotspot[] GROUPS =
            IntStream.range(0, GROUP_NAMES.length)
                    .mapToObj(k -> new Hotspot(6 + k * 11, 70, 10, 10))
                    .toArray(Hotspot[]::new);

    private final boolean[] selection = new boolean[ROW * ROW];
    private boolean az5Lid = true;
    private long lastPress = Long.MIN_VALUE;
    private EditBox field;

    public RBMKConsoleScreen(RBMKConsoleMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 244, 172);
    }

    private static void line(
            GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int wx = dx >= dy ? 0 : 1, wy = dx >= dy ? 1 : 0;
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1 + wx, y0 + 1 + wy, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static void blit(
            GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, width, height, 256, 256);
    }

    private static void gauge(
            GuiGraphicsExtractor graphics, int x, int cellY, int u, int width, int fill) {
        if (fill > 0) blit(graphics, x, cellY + CELL - 1 - fill, u, 191 - fill, width, fill);
    }

    private static int eighths(double value, double max) {
        return (int) Math.ceil(value * 8 / max);
    }

    private static void coolantGauges(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int u,
            int water,
            int maxWater,
            int steam,
            int maxSteam) {
        if (maxWater > 0) gauge(graphics, x + 1, y, u, 3, eighths(water, maxWater));
        if (maxSteam > 0) gauge(graphics, x + 6, y, u + 5, 3, eighths(steam, maxSteam));
    }

    private static void click(float pitch) {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    private static void sound(SoundEvent event, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
    }

    @Override
    protected void init() {
        super.init();
        field = new EditBox(this.font, leftPos + 9, topPos + 84, 35, 9, Component.empty());
        field.setBordered(false);
        field.setMaxLength(3);
        field.setTextColor(CommonColors.GREEN);
        addRenderableWidget(field);
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
        var console = console();

        if (az5Lid) blit(graphics, AZ5.x(), AZ5.y(), 228, 172, 28, 28);
        else if (lastPress + 3000 >= GameTime.millis()) blit(graphics, 30, 136, 228, 228, 28, 28);

        for (int id = 0; id < SCREENS.length; id++)
            blit(
                    graphics,
                    SCREENS[id].x(),
                    SCREENS[id].y(),
                    console.screens[id].readout.atlasU(),
                    238,
                    18,
                    18);

        drawGrid(graphics, console);
        drawFluxGraph(graphics, console);

        drawTooltips(graphics, mouseX, mouseY, console);
    }

    private void drawGrid(GuiGraphicsExtractor graphics, RBMKConsoleBlockEntity console) {
        int[] steamTiers = {
            ColumnSnapshot.fluidId(NTMFluids.STEAM),
            ColumnSnapshot.fluidId(NTMFluids.HOTSTEAM),
            ColumnSnapshot.fluidId(NTMFluids.SUPERHOTSTEAM),
            ColumnSnapshot.fluidId(NTMFluids.ULTRAHOTSTEAM)
        };

        for (int i = 0; i < console.columns.length; i++) {
            var col = console.columns[i];
            if (col == null) continue;

            int x = GRID.x() + CELL * (i % ROW);
            int y = GRID.y() + CELL * (i / ROW);
            blit(graphics, x, y, col.type().offset, 172, CELL, CELL);

            int heat = (int) Math.min(Math.ceil((col.heat - 20) * 10 / col.heatCeiling), 10);
            if (heat > 0) blit(graphics, x, y + CELL - heat, 0, 192 - heat, 10, heat);

            switch (col) {
                case ColumnSnapshot.Control rod -> {
                    if (rod.type() == RBMKColumnType.CONTROL && rod.dye > -1)
                        blit(graphics, x, y, rod.dye * CELL, 202, 10, 10);
                    int withdrawn = 8 - (int) Math.ceil(rod.extraction * 8);
                    if (withdrawn > 0) blit(graphics, x + 4, y + 1, 24, 183, 2, withdrawn);
                }
                case ColumnSnapshot.Fuel fuel when fuel.hullMeltingPoint > 0 -> {
                    gauge(
                            graphics,
                            x + 1,
                            y,
                            11,
                            2,
                            Math.min(eighths(fuel.hullHeat - 20, fuel.hullMeltingPoint), 8));
                    gauge(graphics, x + 4, y, 14, 2, Math.min(eighths(fuel.enrichment, 1), 8));
                    gauge(graphics, x + 7, y, 17, 2, Math.min(eighths(fuel.xenon, 100), 8));
                }
                case ColumnSnapshot.Boiler boiler -> {
                    coolantGauges(
                            graphics,
                            x,
                            y,
                            41,
                            boiler.waterFill,
                            boiler.waterCapacity,
                            boiler.steamFill,
                            boiler.steamCapacity);
                    for (int t = 0; t < steamTiers.length; t++)
                        if (boiler.steamFluid == steamTiers[t])
                            blit(graphics, x + 4, y + 1 + 2 * t, 44, 183 + 2 * t, 2, 2);
                }
                case ColumnSnapshot.Heater heater ->
                        coolantGauges(
                                graphics,
                                x,
                                y,
                                131,
                                heater.feedFill,
                                heater.feedCapacity,
                                heater.productFill,
                                heater.productCapacity);
                default -> {}
            }

            if (selection[i]) blit(graphics, x, y, 0, 192, 10, 10);
        }
    }

    private void drawFluxGraph(GuiGraphicsExtractor graphics, RBMKConsoleBlockEntity console) {
        int[] flux = console.fluxHistory;
        var stats = Arrays.stream(flux).summaryStatistics();
        int highest = stats.getMax(), lowest = stats.getMin();
        int range = Math.max(highest - lowest, 1);

        int lastX = 0, lastY = 0;
        for (int k = 0; k < flux.length; k++) {
            int x = (int) (7 + k * 74.0 / flux.length);
            int y = (int) (127 - (flux[k] - lowest) * 24.0 / range);
            if (k > 0) line(graphics, lastX, lastY, x, y, CommonColors.GREEN);
            lastX = x;
            lastY = y;
        }

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        fluxBound(graphics, String.valueOf(highest), 196);
        fluxBound(
                graphics,
                String.valueOf(lowest),
                (int) ((133 - this.font.lineHeight * 0.5F) / 0.5F));
        graphics.pose().popMatrix();
    }

    private void fluxBound(GuiGraphicsExtractor graphics, String value, int y) {
        graphics.text(this.font, value, 16, y, CommonColors.GREEN, false);
        graphics.text(
                this.font,
                value,
                (int) ((80 - this.font.width(value) * 0.5F) / 0.5F),
                y,
                CommonColors.GREEN,
                false);
    }

    private void drawTooltips(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, RBMKConsoleBlockEntity console) {
        int index = columnAt(mouseX, mouseY, console);
        if (index >= 0) {
            List<Component> list = new ArrayList<>();
            list.add(Component.literal(console.columns[index].type().toString()));
            list.addAll(console.columns[index].tooltip());
            graphics.setComponentTooltipForNextFrame(this.font, list, mouseX, mouseY);
        }

        tip(
                graphics,
                mouseX,
                mouseY,
                SELECT_CONTROL,
                Component.translatable("desc.gui.rbmkConsole.selectAllControlRods"));
        tip(
                graphics,
                mouseX,
                mouseY,
                DESELECT,
                Component.translatable("desc.gui.rbmkConsole.deselectAll"));
        tip(
                graphics,
                mouseX,
                mouseY,
                COMPRESSOR,
                Component.translatable("desc.gui.rbmkConsole.cycleSteamChannel"));

        for (int id = 0; id < SCREENS.length; id++) {
            tip(
                    graphics,
                    mouseX,
                    mouseY,
                    SCREENS[id],
                    Component.translatable(console.screens[id].readout.buttonKey(), id + 1)
                            .withStyle(ChatFormatting.YELLOW));
            tip(
                    graphics,
                    mouseX,
                    mouseY,
                    ASSIGNS[id],
                    Component.translatable("rbmk.console.assign", id + 1));
        }

        for (int k = 0; k < GROUPS.length; k++) {
            var format = GROUP_FORMATS[k];
            tip(
                    graphics,
                    mouseX,
                    mouseY,
                    GROUPS[k],
                    Component.translatable("desc.gui.rbmkConsole.leftClickSelect", GROUP_NAMES[k])
                            .withStyle(format),
                    Component.translatable("desc.gui.rbmkConsole.rightClickAssign", GROUP_NAMES[k])
                            .withStyle(format));
        }
    }

    private void tip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            Hotspot spot,
            Component... lines) {
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                spot.x(),
                spot.y(),
                spot.width(),
                spot.height(),
                List.of(lines));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();
        var console = console();

        int column = columnAt(mx, my, console);
        if (column >= 0) return toggle(column);
        if (over(mx, my, DESELECT)) return deselectAll();
        if (over(mx, my, SELECT_CONTROL))
            return select(console, c -> c.type() == RBMKColumnType.CONTROL, 1.5F);
        if (over(mx, my, COMPRESSOR))
            return send(
                    RBMKConsoleCompressorsLayout.encode(
                            menu.containerId, selection, console.columns),
                    1.0F);
        for (int k = 0; k < GROUPS.length; k++)
            if (over(mx, my, GROUPS[k])) return pressGroup(console, k, event.button());
        if (over(mx, my, AZ5)) return pressAz5(console);
        if (over(mx, my, APPLY_LEVEL)) return applyLevel(console);
        for (int id = 0; id < SCREENS.length; id++) {
            if (over(mx, my, SCREENS[id]))
                return send(RBMKConsoleCycleScreenLayout.encode(menu.containerId, id), 0.5F);
            if (over(mx, my, ASSIGNS[id]))
                return send(
                        RBMKConsoleAssignColumnsLayout.encode(menu.containerId, id, selection),
                        0.75F);
        }

        return super.mouseClicked(event, doubleClick);
    }

    private int columnAt(int mx, int my, RBMKConsoleBlockEntity console) {
        if (!over(mx, my, GRID)) return -1;
        int index = (mx - leftPos - GRID.x()) / CELL + (my - topPos - GRID.y()) / CELL * ROW;
        return index < console.columns.length && console.columns[index] != null ? index : -1;
    }

    private boolean over(int mx, int my, Hotspot spot) {
        return checkClick(mx, my, spot.x(), spot.y(), spot.width(), spot.height());
    }

    private boolean toggle(int index) {
        selection[index] = !selection[index];
        click(0.75F + (selection[index] ? 0.25F : 0.0F));
        return true;
    }

    private boolean deselectAll() {
        Arrays.fill(selection, false);
        click(0.5F);
        return true;
    }

    private boolean select(
            RBMKConsoleBlockEntity console, Predicate<ColumnSnapshot> filter, float pitch) {
        for (int i = 0; i < selection.length; i++)
            selection[i] = console.columns[i] != null && filter.test(console.columns[i]);
        click(pitch);
        return true;
    }

    private static boolean send(ThreadedPayload payload, float pitch) {
        PacketWire.sendToServer(payload);
        click(pitch);
        return true;
    }

    private boolean pressGroup(RBMKConsoleBlockEntity console, int group, int button) {
        float pitch = 0.8F + group * 0.1F;
        if (button == InputConstants.MOUSE_BUTTON_LEFT)
            return select(
                    console,
                    c ->
                            c.type() == RBMKColumnType.CONTROL
                                    && ((ColumnSnapshot.Control) c).dye == group,
                    pitch);
        if (button == InputConstants.MOUSE_BUTTON_RIGHT)
            return send(
                    RBMKConsoleRodColorLayout.encode(
                            menu.containerId, group, selection, console.columns),
                    pitch);
        click(pitch);
        return true;
    }

    private boolean pressAz5(RBMKConsoleBlockEntity console) {
        if (az5Lid) {
            az5Lid = false;
            sound(ModSounds.RBMK_AZ5_COVER.get(), 0.5F);
        } else if (lastPress + 3000 < GameTime.millis()) {
            lastPress = GameTime.millis();
            sound(ModSounds.RBMK_SHUTDOWN.get(), 1.0F);
            send(
                    RBMKConsoleManualLevelLayout.encode(
                            menu.containerId, 0D, selection, true, console.columns),
                    1.0F);
        }
        return true;
    }

    private boolean applyLevel(RBMKConsoleBlockEntity console) {
        double typed;
        try {
            typed = Double.parseDouble(field.getValue());
        } catch (NumberFormatException _) {
            return true;
        }
        if (!Double.isFinite(typed)) return true;
        int pct = (int) Math.clamp(typed, 0, 100);
        field.setValue(String.valueOf(pct));
        return send(
                RBMKConsoleManualLevelLayout.encode(
                        menu.containerId, pct * 0.01D, selection, false, console.columns),
                1.0F);
    }

    private RBMKConsoleBlockEntity console() {
        return menu.blockEntity();
    }

    private record Hotspot(int x, int y, int width, int height) {}
}
