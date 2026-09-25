// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.NuclearTech;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.network.BlockEntityRadioAUTOCAL;
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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

public class ScreenRadioAUTOCAL extends Screen {

    public static final String UPLOAD_FOLDER = "hbmComputerUpload";
    public static final String SCRIPT = "script.txt";
    public static final String DOCUMENTATION = "documentation_v1.1.md";
    private static final Identifier TEXTURE =
            Library.id("textures/gui/machine/gui_rtty_autocal.png");
    private static final String LANG = "desc.gui.radioAUTOCAL.";
    private static final int X_SIZE = 170;
    private static final int Y_SIZE = 138;

    private final BlockEntityRadioAUTOCAL autocal;
    private int guiLeft;
    private int guiTop;

    public ScreenRadioAUTOCAL(BlockEntityRadioAUTOCAL autocal) {
        super(Component.translatable("block.hbm.radio_autocal"));
        this.autocal = autocal;
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

    public static CompoundTag readUpload(Path folder) throws IOException {
        Path script = folder.resolve(SCRIPT);
        Files.createDirectories(folder);
        if (!Files.exists(script)) {
            Files.createFile(script);
            script.toFile().setExecutable(false);
            return null;
        }

        String payload = new String(Files.readAllBytes(script), StandardCharsets.UTF_8);
        if (ByteBufUtil.utf8Bytes(payload) > BlockEntityRadioAUTOCAL.MAX_SCRIPT_BYTES) {
            NuclearTech.LOGGER.warn(
                    "{} exceeds {} bytes and was not uploaded",
                    script,
                    BlockEntityRadioAUTOCAL.MAX_SCRIPT_BYTES);
            return null;
        }
        CompoundTag data = new CompoundTag();
        data.putString("payload", payload);
        return data;
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

        if (autocal.isOn) {
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
        if (!autocal.ignoreError) {
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
        if (!autocal.autoReboot) {
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

        for (int i = 0; i < autocal.history.length; i++) {
            graphics.text(
                    font, autocal.history[i], guiLeft + 7, guiTop + 73 + i * 10, 0xFF00FF00, false);
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
        CompoundTag data = null;

        if (checkClick(x, y, 8)) {
            data = new CompoundTag();
            data.putBoolean("on", true);
        }
        if (checkClick(x, y, 28)) {
            data = new CompoundTag();
            data.putBoolean("ignore", true);
        }
        if (checkClick(x, y, 48)) {
            data = new CompoundTag();
            data.putBoolean("auto", true);
        }

        if (checkClick(x, y, 104)) {
            try {
                Path script = uploadFolder().resolve(SCRIPT);
                Files.createDirectories(script.getParent());
                if (!Files.exists(script)) Files.createFile(script);
                script.toFile().setExecutable(false);
                Util.getPlatform().openPath(script);
            } catch (IOException | RuntimeException ex) {
                NuclearTech.LOGGER.error("Couldn't open link", ex);
            }
        }

        if (checkClick(x, y, 144)) {
            try {
                Path doc = uploadFolder().resolve(DOCUMENTATION);
                Files.createDirectories(doc.getParent());
                if (!Files.exists(doc)) writeDocumentation(doc);
                Util.getPlatform().openPath(doc);
            } catch (IOException | RuntimeException ex) {
                NuclearTech.LOGGER.error("Couldn't open link", ex);
            }
        }

        if (checkClick(x, y, 84)) {
            try {
                data = readUpload(uploadFolder());
            } catch (IOException | RuntimeException ignored) {
            }
        }

        if (data == null) return handled;
        if (minecraft != null) {
            minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        Services.NETWORK.sendToServer(new NbtControlPayload(autocal.getBlockPos(), data));
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
