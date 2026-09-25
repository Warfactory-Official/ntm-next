// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ModifierKeys {

    private ModifierKeys() {}

    public static boolean leftShiftHeld() {
        return InputConstants.isKeyDown(
                Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT);
    }
}
