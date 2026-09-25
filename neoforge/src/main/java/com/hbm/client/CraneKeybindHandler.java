// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumCraneKey;
import com.hbm.packet.toserver.CraneKeyPayload;
import com.hbm.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public final class CraneKeybindHandler {

    public static final KeyMapping[] KEYS = {
        new KeyMapping("key.hbm.craneUp", GLFW.GLFW_KEY_UP, KeyMapping.Category.MISC),
        new KeyMapping("key.hbm.craneDown", GLFW.GLFW_KEY_DOWN, KeyMapping.Category.MISC),
        new KeyMapping("key.hbm.craneLeft", GLFW.GLFW_KEY_LEFT, KeyMapping.Category.MISC),
        new KeyMapping("key.hbm.craneRight", GLFW.GLFW_KEY_RIGHT, KeyMapping.Category.MISC),
        new KeyMapping("key.hbm.craneLoad", GLFW.GLFW_KEY_ENTER, KeyMapping.Category.MISC),
    };

    private static final boolean[] last = new boolean[EnumCraneKey.VALUES.length];

    private CraneKeybindHandler() {}

    public static void poll() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        for (int i = 0; i < KEYS.length; i++) {
            boolean current = KEYS[i].isDown();
            if (current != last[i]) {
                last[i] = current;
                props.setCraneKeyPressed(EnumCraneKey.VALUES[i], current);
                Services.NETWORK.sendToServer(new CraneKeyPayload(i, current));
            }
        }
    }
}
