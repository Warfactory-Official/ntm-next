// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.IKeybindReceiver;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class AbilityKeybindHandler {

    public static final KeyMapping[] KEYS = {
        new KeyMapping("key.hbm.abilityAlt", GLFW.GLFW_KEY_LEFT_ALT, ToolKeybindHandler.CATEGORY),
    };

    private static boolean last;

    private AbilityKeybindHandler() {}

    public static void poll() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;

        boolean current = minecraft.gui.screen() == null && KeyOverlap.isDown(KEYS[0]);
        if (current == last) return;
        last = current;

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !(held.getItem() instanceof IKeybindReceiver receiver)) return;
        if (receiver.canHandleKeybind(player, held, EnumKeybind.ABILITY_ALT)) {
            receiver.handleKeybindClient(player, held, EnumKeybind.ABILITY_ALT, current);
        }
    }
}
