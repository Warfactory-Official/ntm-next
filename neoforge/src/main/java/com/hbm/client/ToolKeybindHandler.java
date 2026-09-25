// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumToolKey;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.ToolKeyPayload;
import com.hbm.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public final class ToolKeybindHandler {

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Library.id("key"));

    public static final KeyMapping[] KEYS = {
        new KeyMapping("key.hbm.copyToolAlt", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY),
        new KeyMapping("key.hbm.copyToolCtrl", GLFW.GLFW_KEY_LEFT_CONTROL, CATEGORY),
    };

    private static final boolean[] last = new boolean[EnumToolKey.VALUES.length];

    private ToolKeybindHandler() {}

    public static void poll() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        for (int i = 0; i < KEYS.length; i++) {
            boolean current = KEYS[i].isDown();
            if (current == last[i]) continue;
            last[i] = current;
            props.setToolKeyPressed(EnumToolKey.VALUES[i], current);
            Services.NETWORK.sendToServer(new ToolKeyPayload(i, current));
        }
    }
}
