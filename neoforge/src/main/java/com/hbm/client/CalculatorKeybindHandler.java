// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.client.gui.ScreenCalculator;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class CalculatorKeybindHandler {

    public static final KeyMapping OPEN =
            new KeyMapping("hbm.key.calculator", GLFW.GLFW_KEY_N, ToolKeybindHandler.CATEGORY);

    private CalculatorKeybindHandler() {}

    public static void poll() {
        if (!OPEN.consumeClick()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        client.player.closeContainer();
        client.gui.setScreen(new ScreenCalculator());
    }
}
