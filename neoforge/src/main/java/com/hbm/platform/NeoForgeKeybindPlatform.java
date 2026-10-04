// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.InputQuirks;
import org.lwjgl.glfw.GLFW;

public final class NeoForgeKeybindPlatform {
    public static boolean sameChord(KeyMapping first, KeyMapping second) {
        return !first.isUnbound()
                && first.getKey().equals(second.getKey())
                && first.getKeyModifier() == second.getKeyModifier();
    }

    public static boolean modifierMatches(KeyMapping mapping, int modifiers) {
        int mask =
                switch (mapping.getKeyModifier()) {
                    case NONE -> 0;
                    case SHIFT -> GLFW.GLFW_MOD_SHIFT;
                    case CONTROL -> GLFW.GLFW_MOD_CONTROL;
                    case CONTROL_OR_COMMAND ->
                            InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY
                                    ? GLFW.GLFW_MOD_SUPER
                                    : GLFW.GLFW_MOD_CONTROL;
                    case ALT -> GLFW.GLFW_MOD_ALT;
                };
        return (modifiers & mask) == mask;
    }
}
