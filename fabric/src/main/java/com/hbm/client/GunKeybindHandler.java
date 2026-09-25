// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.IKeybindReceiver;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.packet.toserver.KeybindPayload;
import com.hbm.platform.Services;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class GunKeybindHandler {

    public static final GunKeyMapping[] KEYS = {
        new GunKeyMapping(
                "key.hbm.gunPrimary",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_LEFT,
                ToolKeybindHandler.CATEGORY),
        new GunKeyMapping(
                "key.hbm.gunSecondary",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                ToolKeybindHandler.CATEGORY),
        new GunKeyMapping(
                "key.hbm.gunTertiary",
                InputConstants.Type.MOUSE,
                GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                ToolKeybindHandler.CATEGORY),
        new GunKeyMapping(
                "key.hbm.reload",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                ToolKeybindHandler.CATEGORY),
    };
    private static final EnumKeybind[] GUN_KEYS = {
        EnumKeybind.GUN_PRIMARY,
        EnumKeybind.GUN_SECONDARY,
        EnumKeybind.GUN_TERTIARY,
        EnumKeybind.RELOAD,
    };

    private GunKeybindHandler() {}

    public static void poll() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        HbmPlayerProps props = HbmPlayerProps.getData(player);

        boolean inGame = mc.gui.screen() == null;

        for (int i = 0; i < KEYS.length; i++) {
            EnumKeybind key = GUN_KEYS[i];
            boolean current = inGame && KEYS[i].rawDown();

            if (!current && inGame && KEYS[i].consumeClick()) current = true;

            boolean last = props.getKeyPressed(key);
            if (last == current) continue;

            props.setKeyPressed(key, current);
            Services.NETWORK.sendToServer(new KeybindPayload(key, current));
            onPressedClient(player, key, current);
        }
    }

    private static void onPressedClient(Player player, EnumKeybind key, boolean state) {
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && held.getItem() instanceof IKeybindReceiver rec) {
            if (rec.canHandleKeybind(player, held, key))
                rec.handleKeybindClient(player, held, key, state);
        }
    }

    public static boolean isGunKey(KeyMapping vanillaKey) {
        for (GunKeyMapping key : KEYS) {
            if (key.sameKeyAs(vanillaKey)) return true;
        }
        return false;
    }

    public static boolean isGunHeld() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        ItemStack held = player.getMainHandItem();
        return !held.isEmpty() && held.getItem() instanceof ItemGunBaseNT;
    }

    public static final class GunKeyMapping extends KeyMapping {
        public GunKeyMapping(
                String name, InputConstants.Type type, int value, KeyMapping.Category category) {
            super(name, type, value, category);
        }

        public boolean rawDown() {
            if (isUnbound()) return false;
            var window = Minecraft.getInstance().getWindow();
            if (this.key.getType() == InputConstants.Type.MOUSE) {
                return GLFW.glfwGetMouseButton(window.handle(), this.key.getValue())
                        == GLFW.GLFW_PRESS;
            }
            return InputConstants.isKeyDown(window, this.key.getValue());
        }

        public boolean sameKeyAs(KeyMapping vanilla) {
            return !isUnbound() && this.same(vanilla);
        }
    }
}
