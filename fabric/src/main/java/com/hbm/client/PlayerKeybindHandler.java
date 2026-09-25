// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.packet.toserver.KeybindPayload;
import com.hbm.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

public final class PlayerKeybindHandler {

    public static final KeyMapping[] KEYS = {
        new KeyMapping("key.hbm.toggleHUD", GLFW.GLFW_KEY_V, ToolKeybindHandler.CATEGORY),
        new KeyMapping("key.hbm.toggleBack", GLFW.GLFW_KEY_C, ToolKeybindHandler.CATEGORY),
        new KeyMapping("key.hbm.toggleMagnet", GLFW.GLFW_KEY_Z, ToolKeybindHandler.CATEGORY),
        new KeyMapping("key.hbm.dash", GLFW.GLFW_KEY_LEFT_SHIFT, ToolKeybindHandler.CATEGORY),
        new KeyMapping("key.hbm.trainInv", GLFW.GLFW_KEY_R, ToolKeybindHandler.CATEGORY),
    };
    private static final EnumKeybind[] BINDS = {
        EnumKeybind.TOGGLE_HEAD,
        EnumKeybind.TOGGLE_JETPACK,
        EnumKeybind.TOGGLE_MAGNET,
        EnumKeybind.DASH,
        EnumKeybind.TRAIN
    };

    private PlayerKeybindHandler() {}

    public static void poll() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        for (int i = 0; i < KEYS.length; i++) {
            boolean current = KeyOverlap.isDown(KEYS[i]);
            if (current == props.getKeyPressed(BINDS[i])) continue;
            props.setKeyPressed(BINDS[i], current);
            Services.NETWORK.sendToServer(new KeybindPayload(BINDS[i], current));
        }

        boolean jump = Minecraft.getInstance().options.keyJump.isDown();
        if (jump != props.getKeyPressed(EnumKeybind.JETPACK)) {
            props.setKeyPressed(EnumKeybind.JETPACK, jump);
            Services.NETWORK.sendToServer(new KeybindPayload(EnumKeybind.JETPACK, jump));
        }
    }
}
