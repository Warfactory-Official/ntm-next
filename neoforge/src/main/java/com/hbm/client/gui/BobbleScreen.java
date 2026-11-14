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

import com.hbm.blockentity.BobbleBlockEntity;
import com.hbm.blocks.generic.BobbleBlock.Bobble;
import com.hbm.blocks.generic.BobbleBlock.BobbleType;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

public class BobbleScreen extends Screen {
    private static final int SIZE_X = 300;
    private static final int SIZE_Y = 150;
    private static final int SLAB_COLOR = 0xCC003300;
    private static final int HEADING = 0xFF00FF00;
    private static final int BODY = 0xFF009900;

    private final BobbleBlockEntity bobble;

    public BobbleScreen(BobbleBlockEntity bobble) {
        super(Component.translatable("desc.gui.bobble.nuclearTechCommemorative"));
        this.bobble = bobble;
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) {
            this.minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(ModSounds.BOBBLE.get(), 1.0F));
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - SIZE_X) / 2;
        int top = (this.height - SIZE_Y) / 2;
        graphics.fill(left, top, left + SIZE_X, top + SIZE_Y, SLAB_COLOR);

        int centre = left + SIZE_X / 2;
        int nextLevel = top + 10;

        line(graphics, "Nuclear Tech Commemorative Bobblehead", centre, nextLevel, HEADING);
        nextLevel += 10;

        Bobble type = this.bobble.type;
        String bobbleName = type.fullName();
        if (type == BobbleType.MELLOW) {
            double guiMillis = (minecraft.gui.hud.getGuiTicks() + (double) partialTick) * 50D;
            bobbleName = anagramIt(bobbleName, "GEORGEWILLIAMPATON", guiMillis);
        }
        line(graphics, bobbleName, centre, nextLevel, BODY);
        nextLevel += 20;

        if (type.contribution() != null) {
            line(graphics, "Has contributed", centre, nextLevel, HEADING);
            nextLevel += 10;
            for (String text : type.contribution().split("\\$")) {
                line(graphics, text, centre, nextLevel, BODY);
                nextLevel += 10;
            }
            nextLevel += 10;
        }

        if (type.inscription() != null) {
            line(
                    graphics,
                    "On the bottom is the following inscription:",
                    centre,
                    nextLevel,
                    HEADING);
            nextLevel += 10;
            for (String text : type.inscription().split("\\$")) {
                line(graphics, text, centre, nextLevel, BODY);
                nextLevel += 10;
            }
        }
    }

    private void line(GuiGraphicsExtractor graphics, String text, int centre, int y, int color) {
        graphics.text(this.font, text, centre - this.font.width(text) / 2, y, color, true);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String anagramIt(String from, String to, double guiMillis) {
        double t = Math.sin(guiMillis / 1500.0) * 0.75 + 0.5;

        char[] lettersFrom = from.toCharArray();
        char[] lettersTo = to.toCharArray();
        boolean[] hasPairedLetter = new boolean[lettersFrom.length];
        List<double[]> letterTargets = new ArrayList<>();
        List<Character> letterChars = new ArrayList<>();

        for (int i = 0; i < lettersFrom.length; i++) {
            char letterFrom = lettersFrom[i];
            for (int o = 0; o < lettersTo.length; o++) {
                char letterTo = lettersTo[o];
                if (letterFrom == letterTo && !hasPairedLetter[o]) {
                    letterTargets.add(new double[] {lerp(i, o, t)});
                    letterChars.add(lettersFrom[i]);
                    hasPairedLetter[o] = true;
                    break;
                }
            }
        }

        for (int i = 0; i < letterTargets.size(); i++) {
            for (int j = i + 1; j < letterTargets.size(); j++) {
                if (letterTargets.get(i)[0] > letterTargets.get(j)[0]) {
                    double[] tempKey = letterTargets.get(i);
                    letterTargets.set(i, letterTargets.get(j));
                    letterTargets.set(j, tempKey);
                    char tempChar = letterChars.get(i);
                    letterChars.set(i, letterChars.get(j));
                    letterChars.set(j, tempChar);
                }
            }
        }

        StringBuilder anagrammedText = new StringBuilder();
        for (char in : letterChars) anagrammedText.append(in);
        return anagrammedText.toString();
    }

    private double lerp(double a, double b, double t) {
        t = Math.max(Math.min(t, 1), 0);
        return a * (1 - t) + b * t;
    }
}
