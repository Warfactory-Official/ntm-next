/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.NuclearTech;
import com.hbm.blockentity.network.RadioAutocalBlockEntity;
import com.hbm.inventory.container.ScreenMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RadioAutocalIgnoreLayout;
import com.hbm.packet.layout.RadioAutocalPowerLayout;
import com.hbm.packet.layout.RadioAutocalRebootLayout;
import com.hbm.packet.layout.RadioAutocalScriptLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.Blaze3D;
import io.netty.buffer.ByteBufUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

public class RadioAUTOCALScreen extends MenuScreen<ScreenMenu<RadioAutocalBlockEntity>> {

    public static final String UPLOAD_FOLDER = "hbmComputerUpload";
    public static final String SCRIPT = "script.txt";
    public static final String DOCUMENTATION = "documentation_v1.1.md";
    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_rtty_autocal.png");
    private static final String LANG = "desc.gui.radioAUTOCAL.";
    private static final int X_SIZE = 170;
    private static final int Y_SIZE = 138;

    private final RadioAutocalBlockEntity autocal;
    private int guiLeft;
    private int guiTop;

    public RadioAUTOCALScreen(
            ScreenMenu<RadioAutocalBlockEntity> menu, Inventory inventory, Component title) {
        super(menu, title);
        this.autocal = menu.blockEntity();
    }

    public static Path uploadFolder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(UPLOAD_FOLDER);
    }

    public static void writeDocumentation(Path doc) throws IOException {
        Language language = Language.getInstance();
        assert language.has(LANG + "doc.0");
        List<String> lines = new ArrayList<>();
        for (int i = 0; language.has(LANG + "doc." + i); i++) {
            if (i > 0) lines.add("");
            lines.addAll(List.of(language.getOrDefault(LANG + "doc." + i).split("\n", -1)));
        }
        Files.write(doc, lines, StandardCharsets.UTF_8);
    }

    public static @Nullable String readUpload(Path folder) throws IOException {
        Path script = folder.resolve(SCRIPT);
        Files.createDirectories(folder);
        if (!Files.exists(script)) {
            Files.createFile(script);
            script.toFile().setExecutable(false);
            return null;
        }

        String payload = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);
        if (ByteBufUtil.utf8Bytes(payload) > RadioAutocalBlockEntity.MAX_SCRIPT_BYTES) {
            NuclearTech.LOGGER.warn(
                    "{} exceeds {} bytes and was not uploaded",
                    script,
                    RadioAutocalBlockEntity.MAX_SCRIPT_BYTES);
            return null;
        }
        return payload;
    }

    @Override
    protected void init() {
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - Y_SIZE) / 2;
    }

    @Override
    public void tick() {
        if (autocal.isRemoved()) onClose();
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                guiLeft,
                guiTop,
                0.0F,
                0.0F,
                X_SIZE,
                Y_SIZE,
                256,
                256);

        if (autocal.running()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + 8,
                    guiTop + 36,
                    X_SIZE,
                    0,
                    18,
                    18,
                    256,
                    256);
        }
        if (!autocal.ignoresErrors()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + 28,
                    guiTop + 36,
                    X_SIZE,
                    18,
                    18,
                    18,
                    256,
                    256);
        }
        if (!autocal.rebootsAutomatically()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + 48,
                    guiTop + 36,
                    X_SIZE,
                    36,
                    18,
                    18,
                    256,
                    256);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < autocal.historySize(); i++) {
            graphics.text(
                    font,
                    autocal.historyLine(i),
                    guiLeft + 7,
                    guiTop + 73 + i * 10,
                    0xFF00FF00,
                    false);
        }

        tooltip(graphics, mouseX, mouseY, 8, text("onOff", ChatFormatting.RED));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                28,
                text("ignoreErrors", ChatFormatting.RED),
                text("skipsInstructionsThatError"),
                text("leavingTheComputerTurned"),
                text("mayCauseUnintendedBehavior"),
                text("andInconsistencies"));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                48,
                text("automaticReboot", ChatFormatting.RED),
                text("restartsTheComputerAutomatically"),
                text("theProgramStopsDue"),
                text("orAfterFinishing"));

        tooltip(graphics, mouseX, mouseY, 84, text("uploadProgram", ChatFormatting.BLUE));
        tooltip(graphics, mouseX, mouseY, 104, text("openProgramFile", ChatFormatting.BLUE));
        tooltip(
                graphics,
                mouseX,
                mouseY,
                124,
                text("downloadProgram", ChatFormatting.BLUE),
                text("currentlyUnsupported", ChatFormatting.RED));
        tooltip(graphics, mouseX, mouseY, 144, text("openDocumentation", ChatFormatting.BLUE));
    }

    private static Component text(String key, ChatFormatting... style) {
        return Component.translatable(LANG + key).withStyle(style);
    }

    private void tooltip(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, Component... lines) {
        if (checkClick(mouseX, mouseY, left)) {
            graphics.setComponentTooltipForNextFrame(font, List.of(lines), mouseX, mouseY);
        }
    }

    private boolean checkClick(double x, double y, int left) {
        return guiLeft + left <= x
                && guiLeft + left + 18 > x
                && guiTop + 36 < y
                && guiTop + 36 + 18 >= y;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        double x = event.x();
        double y = event.y();
        if (checkClick(x, y, 8)) {
            PacketWire.sendToServer(RadioAutocalPowerLayout.encode(menu.containerId));
        }
        if (checkClick(x, y, 28)) {
            PacketWire.sendToServer(RadioAutocalIgnoreLayout.encode(menu.containerId));
        }
        if (checkClick(x, y, 48)) {
            PacketWire.sendToServer(RadioAutocalRebootLayout.encode(menu.containerId));
        }

        if (checkClick(x, y, 104)) {
            try {
                Path script = uploadFolder().resolve(SCRIPT);
                Files.createDirectories(script.getParent());
                if (!Files.exists(script)) Files.createFile(script);
                script.toFile().setExecutable(false);
                Blaze3D.openPath(script);
            } catch (IOException | RuntimeException ex) {
                NuclearTech.LOGGER.error("Couldn't open link", ex);
            }
        }

        if (checkClick(x, y, 144)) {
            try {
                Path doc = uploadFolder().resolve(DOCUMENTATION);
                Files.createDirectories(doc.getParent());
                if (!Files.exists(doc)) writeDocumentation(doc);
                Blaze3D.openPath(doc);
            } catch (IOException | RuntimeException ex) {
                NuclearTech.LOGGER.error("Couldn't open link", ex);
            }
        }

        if (checkClick(x, y, 84)) {
            try {
                String script = readUpload(uploadFolder());
                if (script == null) return handled;
                PacketWire.sendToServer(RadioAutocalScriptLayout.encode(menu.containerId, script));
            } catch (IOException | RuntimeException ignored) {
            }
        }

        if (!checkClick(x, y, 8)
                && !checkClick(x, y, 28)
                && !checkClick(x, y, 48)
                && !checkClick(x, y, 84)) return handled;
        if (minecraft != null) {
            minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
