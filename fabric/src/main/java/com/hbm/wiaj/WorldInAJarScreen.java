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

package com.hbm.wiaj;

import com.hbm.client.Keybinds;
import com.hbm.client.gui.GUIElements;
import com.hbm.util.RegistryUtil;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.JarActor;
import com.hbm.wiaj.cannery.CanneryBase;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public final class WorldInAJarScreen extends Screen {
    private static final Identifier GUI = RegistryUtil.id("textures/gui/gui_utility.png");
    private final JarScript script;
    private final ItemStack icon;
    private final CanneryBase[] seeAlso;
    private final ActorFancyPanel titlePanel;
    private final ActorFancyPanel[] seeAlsoTitles;

    public WorldInAJarScreen(
            JarScript script, String title, ItemStack icon, CanneryBase... seeAlso) {
        super(Component.translatable(title));
        this.script = script;
        this.icon = icon;
        this.seeAlso = seeAlso;
        titlePanel =
                new ActorFancyPanel(40, 27, new Object[][] {{Component.translatable(title)}}, 0)
                        .setColors(CanneryBase.colorGold)
                        .setOrientation(ActorFancyPanel.Orientation.LEFT);
        seeAlsoTitles = new ActorFancyPanel[seeAlso.length];
        for (int i = 0; i < seeAlso.length; i++) {
            seeAlsoTitles[i] =
                    new ActorFancyPanel(
                                    40,
                                    27 + 36 * (i + 1),
                                    new Object[][] {{Component.translatable(seeAlso[i].getName())}},
                                    0)
                            .setColors(CanneryBase.colorGrey)
                            .setOrientation(ActorFancyPanel.Orientation.LEFT);
        }
    }

    public static WorldInAJarScreen of(CanneryBase cannery) {
        return new WorldInAJarScreen(
                cannery.createScript(), cannery.getName(), cannery.getIcon(), cannery.seeAlso());
    }

    @Override
    protected void init() {
        super.init();
        script.run();
    }

    @Override
    public void tick() {
        script.run();
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        script.interp = script.isPaused() || script.currentScene == null ? 0F : partialTick;
        graphics.guiRenderState.addPicturesInPictureState(
                new JarRenderState(script, partialTick, 0, 0, width, height));
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (JarActor actor : script.actors.values()) {
            actor.drawOverlay(graphics, width, height, script.ticksElapsed, script.interp);
        }
        if (Keybinds.DIAGNOSTICS.isDown()) {
            Component cursor =
                    Component.literal((mouseX - width / 2) + " / " + (mouseY - height / 2));
            GUIElements.drawStackText(
                    graphics,
                    font,
                    List.<Object[]>of(new Object[] {cursor}),
                    mouseX,
                    mouseY,
                    width,
                    height);
        }

        int playX = width / 2 - 12;
        int buttonY = height - 36;
        button(
                graphics,
                playX,
                buttonY,
                script.isPaused() ? 64 : 40,
                in(mouseX, mouseY, playX, buttonY, 24, 24) ? 24 : 48);
        button(
                graphics,
                playX - 36,
                buttonY,
                88,
                script.sceneNumber == 0
                        ? 72
                        : in(mouseX, mouseY, playX - 36, buttonY, 24, 24) ? 24 : 48);
        button(
                graphics,
                playX + 36,
                buttonY,
                112,
                script.sceneNumber >= script.scenes.size()
                        ? 72
                        : in(mouseX, mouseY, playX + 36, buttonY, 24, 24) ? 24 : 48);

        button(graphics, 15, 15, 136, 48);
        graphics.item(icon, 19, 19);
        graphics.itemDecorations(font, icon, 19, 19);
        if (in(mouseX, mouseY, 15, 15, 24, 24))
            titlePanel.drawOverlay(graphics, 0, 0, script.ticksElapsed, script.interp);

        for (int i = 0; i < seeAlso.length; i++) {
            int y = 15 + 36 * (i + 1);
            button(graphics, 15, y, 136, 72);
            ItemStack stack = seeAlso[i].getIcon();
            graphics.item(stack, 19, y + 4);
            graphics.itemDecorations(font, stack, 19, y + 4);
            if (in(mouseX, mouseY, 15, y, 24, 24))
                seeAlsoTitles[i].drawOverlay(graphics, 0, 0, script.ticksElapsed, script.interp);
        }
    }

    private static boolean in(int mouseX, int mouseY, int x, int y, int w, int h) {
        return x <= mouseX && mouseX < x + w && y < mouseY && mouseY <= y + h;
    }

    private static void button(GuiGraphicsExtractor graphics, int x, int y, int u, int v) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI, x, y, u, v, 24, 24, 256, 256);
    }

    private void click() {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();
        int playX = width / 2 - 12, buttonY = height - 36;
        if (in(x, y, playX, buttonY, 24, 24)) {
            click();
            if (script.isPaused()) script.unpause();
            else script.pause();
            return true;
        }
        if (script.sceneNumber > 0 && in(x, y, playX - 36, buttonY, 24, 24)) {
            click();
            script.rewindOne();
            return true;
        }
        if (script.sceneNumber < script.scenes.size() && in(x, y, playX + 36, buttonY, 24, 24)) {
            click();
            script.forwardOne();
            return true;
        }
        for (int i = 0; i < seeAlso.length; i++) {
            int iconY = 15 + 36 * (i + 1);
            if (!in(x, y, 15, iconY, 24, 24)) continue;
            click();
            minecraft.gui.setScreen(of(seeAlso[i]));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
