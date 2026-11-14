/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client;

import com.hbm.client.gui.CalculatorScreen;
import com.hbm.entity.train.RailCarBaseEntity;
import com.hbm.extprop.PlayerState;
import com.hbm.handler.CraneKey;
import com.hbm.handler.KeybindType;
import com.hbm.handler.ToolKey;
import com.hbm.items.tool.SettingsToolItem;
import com.hbm.items.tool.ToolAbilityItem;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CraneKeyLayout;
import com.hbm.packet.layout.KeybindLayout;
import com.hbm.packet.layout.ToolKeyLayout;
import com.hbm.platform.KeybindPlatform;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.entity.player.Player;

public final class Keybinds {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(RegistryUtil.id("key"));

    public static final GameplayKey TOOL_ALT =
            new GameplayKey(
                    "key.hbm.copyToolAlt", InputConstants.KEY_LALT, CATEGORY, Action.TOOL, 0);
    public static final GameplayKey TOOL_CTRL =
            new GameplayKey(
                    "key.hbm.copyToolCtrl", InputConstants.KEY_LCONTROL, CATEGORY, Action.TOOL, 1);
    public static final GameplayKey CRANE_UP =
            new GameplayKey("key.hbm.craneUp", InputConstants.KEY_UP, CATEGORY, Action.CRANE, 0);
    public static final GameplayKey CRANE_DOWN =
            new GameplayKey(
                    "key.hbm.craneDown", InputConstants.KEY_DOWN, CATEGORY, Action.CRANE, 1);
    public static final GameplayKey CRANE_LEFT =
            new GameplayKey(
                    "key.hbm.craneLeft", InputConstants.KEY_LEFT, CATEGORY, Action.CRANE, 2);
    public static final GameplayKey CRANE_RIGHT =
            new GameplayKey(
                    "key.hbm.craneRight", InputConstants.KEY_RIGHT, CATEGORY, Action.CRANE, 3);
    public static final GameplayKey CRANE_LOAD =
            new GameplayKey(
                    "key.hbm.craneLoad", InputConstants.KEY_RETURN, CATEGORY, Action.CRANE, 4);
    public static final GameplayKey TOGGLE_HUD =
            new GameplayKey("key.hbm.toggleHUD", InputConstants.KEY_V, CATEGORY, Action.PLAYER, 0);
    public static final GameplayKey TOGGLE_BACKPACK =
            new GameplayKey("key.hbm.toggleBack", InputConstants.KEY_C, CATEGORY, Action.PLAYER, 1);
    public static final GameplayKey TOGGLE_MAGNET =
            new GameplayKey(
                    "key.hbm.toggleMagnet", InputConstants.KEY_Z, CATEGORY, Action.PLAYER, 2);
    public static final GameplayKey DASH =
            new GameplayKey("key.hbm.dash", InputConstants.KEY_LSHIFT, CATEGORY, Action.PLAYER, 3);
    public static final GameplayKey TRAIN_INVENTORY =
            new GameplayKey("key.hbm.trainInv", InputConstants.KEY_R, CATEGORY, Action.PLAYER, 4);
    public static final GameplayKey GUN_PRIMARY =
            new GameplayKey(
                    "key.hbm.gunPrimary",
                    InputConstants.Type.MOUSE,
                    InputConstants.MOUSE_BUTTON_LEFT,
                    CATEGORY,
                    Action.GUN,
                    0);
    public static final GameplayKey GUN_SECONDARY =
            new GameplayKey(
                    "key.hbm.gunSecondary",
                    InputConstants.Type.MOUSE,
                    InputConstants.MOUSE_BUTTON_RIGHT,
                    CATEGORY,
                    Action.GUN,
                    1);
    public static final GameplayKey GUN_TERTIARY =
            new GameplayKey(
                    "key.hbm.gunTertiary",
                    InputConstants.Type.MOUSE,
                    InputConstants.MOUSE_BUTTON_MIDDLE,
                    CATEGORY,
                    Action.GUN,
                    2);
    public static final GameplayKey RELOAD =
            new GameplayKey("key.hbm.reload", InputConstants.KEY_R, CATEGORY, Action.GUN, 3);
    public static final GameplayKey ABILITY_CYCLE =
            new GameplayKey(
                    "key.hbm.ability",
                    InputConstants.Type.MOUSE,
                    InputConstants.MOUSE_BUTTON_RIGHT,
                    CATEGORY,
                    Action.ABILITY,
                    0);
    public static final GameplayKey ABILITY_CONFIGURE =
            new GameplayKey(
                    "key.hbm.abilityAlt", InputConstants.KEY_LALT, CATEGORY, Action.ABILITY, 1);
    public static final GameplayKey CALCULATOR =
            new GameplayKey(
                    "hbm.key.calculator", InputConstants.KEY_N, CATEGORY, Action.CALCULATOR, 0);
    public static final KeyMapping MANUAL =
            new KeyMapping("key.hbm.qmaw", InputConstants.KEY_F1, CATEGORY);
    public static final HeldKey DETAILS =
            new HeldKey("key.hbm.tooltipDetails", InputConstants.KEY_LSHIFT);
    public static final HeldKey PRESENTATION =
            new HeldKey("key.hbm.presentation", InputConstants.KEY_LSHIFT);
    public static final HeldKey DIAGNOSTICS =
            new HeldKey("key.hbm.guiDiagnostics", InputConstants.KEY_LALT);
    public static final HeldKey BATCH = new HeldKey("key.hbm.guiBatch", InputConstants.KEY_LSHIFT);

    public static final KeyMapping[] ALL = {
        CRANE_UP,
        CRANE_DOWN,
        CRANE_LEFT,
        CRANE_RIGHT,
        CRANE_LOAD,
        TOOL_ALT,
        TOOL_CTRL,
        GUN_PRIMARY,
        GUN_SECONDARY,
        GUN_TERTIARY,
        RELOAD,
        TOGGLE_HUD,
        TOGGLE_BACKPACK,
        TOGGLE_MAGNET,
        DASH,
        TRAIN_INVENTORY,
        ABILITY_CYCLE,
        ABILITY_CONFIGURE,
        DETAILS,
        PRESENTATION,
        DIAGNOSTICS,
        BATCH,
        CALCULATOR,
        MANUAL,
    };
    private static final KeybindType[] PLAYER_KEYS = {
        KeybindType.TOGGLE_HUD,
        KeybindType.TOGGLE_JETPACK,
        KeybindType.TOGGLE_MAGNET,
        KeybindType.DASH,
        KeybindType.TRAIN_INVENTORY,
    };
    private static final KeybindType[] GUN_KEYS = {
        KeybindType.GUN_PRIMARY,
        KeybindType.GUN_SECONDARY,
        KeybindType.GUN_TERTIARY,
        KeybindType.RELOAD,
    };
    private static int modifiers;
    private static boolean cyclePressed;
    private static boolean calculatorRequested;

    private Keybinds() {}

    public static void key(long window, int action, KeyEvent event) {
        if (window != Minecraft.getInstance().getWindow().handle()) return;
        modifiers = event.modifiers();
        for (KeyMapping mapping : ALL) {
            if (mapping instanceof HeldKey key && !key.isUnbound() && key.matches(event))
                key.observe(action != InputConstants.RELEASE);
        }
    }

    public static void mouse(long window, MouseButtonInfo event, int action) {
        if (window != Minecraft.getInstance().getWindow().handle()) return;
        modifiers = event.modifiers();
        InputConstants.Key button = InputConstants.Type.MOUSE.getOrCreate(event.button());
        for (KeyMapping mapping : ALL) {
            if (mapping instanceof HeldKey key
                    && KeyMappingHelper.getBoundKeyOf((KeyMapping) key).equals(button))
                key.observe(action != InputConstants.RELEASE);
        }
    }

    public static void poll() {
        Minecraft mc = Minecraft.getInstance();
        boolean active = mc.isWindowActive();
        if (!active) modifiers = 0;
        for (KeyMapping mapping : ALL) {
            while (mapping.consumeClick()) {}
            if (mapping instanceof GameplayKey key) key.refresh();
            else if (!active && mapping instanceof HeldKey key) key.physicalDown = false;
        }
        if (mc.player != null) {
            PlayerState props = PlayerState.getOrCreate(mc.player);
            boolean jump = mc.gui.screen() == null && mc.options.keyJump.isDown();
            if (jump != props.held(KeybindType.JUMP)) {
                props.setHeld(KeybindType.JUMP, jump);
                PacketWire.sendToServer(KeybindLayout.encode(KeybindType.JUMP, jump));
            }
        }
        if (calculatorRequested) {
            calculatorRequested = false;
            if (mc.player != null) {
                mc.player.closeContainer();
                mc.gui.setScreen(new CalculatorScreen());
            }
        }
        if (cyclePressed
                && (mc.player == null || mc.gui.screen() != null || !ABILITY_CYCLE.isDown())) {
            if (mc.player != null) sendCycle(false);
            cyclePressed = false;
        }
    }

    private static void playerChanged(int index, boolean current) {
        PlayerState.getOrCreate(Minecraft.getInstance().player)
                .setHeld(PLAYER_KEYS[index], current);
        PacketWire.sendToServer(KeybindLayout.encode(PLAYER_KEYS[index], current));
    }

    private static void toolChanged(int index, boolean current) {
        PlayerState.getOrCreate(Minecraft.getInstance().player)
                .setHeld(ToolKey.VALUES[index], current);
        PacketWire.sendToServer(ToolKeyLayout.encode(ToolKey.VALUES[index], current));
    }

    private static void craneChanged(int index, boolean current) {
        PlayerState.getOrCreate(Minecraft.getInstance().player)
                .setHeld(CraneKey.VALUES[index], current);
        PacketWire.sendToServer(CraneKeyLayout.encode(CraneKey.VALUES[index], current));
    }

    private static void gunChanged(int index, boolean current) {
        Player player = Minecraft.getInstance().player;
        KeybindType key = GUN_KEYS[index];
        PlayerState.getOrCreate(player).setHeld(key, current);
        PacketWire.sendToServer(KeybindLayout.encode(key, current));
    }

    public static boolean isGunKey(KeyMapping key) {
        return KeybindPlatform.sameChord(GUN_PRIMARY, key)
                || KeybindPlatform.sameChord(GUN_SECONDARY, key)
                || KeybindPlatform.sameChord(GUN_TERTIARY, key)
                || KeybindPlatform.sameChord(RELOAD, key);
    }

    public static boolean isGunHeld() {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem() instanceof GunBaseNTItem;
    }

    private static void abilityChanged(int index, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        if (index == 0) {
            if (pressed && KeybindPlatform.sameChord(ABILITY_CYCLE, mc.options.keyUse)) return;
            if (cyclePressed != pressed) {
                sendCycle(pressed);
                cyclePressed = pressed;
            }
        } else {
            PacketWire.sendToServer(KeybindLayout.encode(KeybindType.ABILITY_CONFIGURE, pressed));
        }
    }

    public static boolean cycleFromUse() {
        if (!KeybindPlatform.sameChord(ABILITY_CYCLE, Minecraft.getInstance().options.keyUse))
            return false;
        if (!cyclePressed) {
            sendCycle(true);
            cyclePressed = true;
        }
        return true;
    }

    private static void sendCycle(boolean pressed) {
        PacketWire.sendToServer(KeybindLayout.encode(KeybindType.ABILITY_CYCLE, pressed));
    }

    private static void calculatorChanged(boolean pressed) {
        if (pressed) calculatorRequested = true;
    }

    private enum Action {
        PLAYER,
        TOOL,
        CRANE,
        GUN,
        ABILITY,
        CALCULATOR
    }

    public static final class GameplayKey extends KeyMapping {
        private final Action action;
        private final int index;
        private boolean activeDown;
        private InputConstants.Key downKey = InputConstants.UNKNOWN;

        private GameplayKey(String name, int value, Category category, Action action, int index) {
            this(name, InputConstants.Type.KEYSYM, value, category, action, index);
        }

        private GameplayKey(
                String name,
                InputConstants.Type type,
                int value,
                Category category,
                Action action,
                int index) {
            super(name, type, value, category);
            this.action = action;
            this.index = index;
        }

        @Override
        public void setDown(boolean down) {
            downKey = KeyMappingHelper.getBoundKeyOf((KeyMapping) this);
            super.setDown(down);
            refresh();
        }

        @Override
        public void setKey(InputConstants.Key key) {
            setDown(false);
            super.setKey(key);
        }

        private void refresh() {
            if (!downKey.equals(KeyMappingHelper.getBoundKeyOf((KeyMapping) this)))
                super.setDown(false);
            Minecraft mc = Minecraft.getInstance();
            boolean current =
                    mc.player != null
                            && mc.gui.screen() == null
                            && mc.isWindowActive()
                            && !isUnbound()
                            && super.isDown()
                            && switch (action) {
                                case GUN -> isGunHeld();
                                case TOOL ->
                                        mc.player.getMainHandItem().getItem()
                                                instanceof SettingsToolItem;
                                case ABILITY ->
                                        mc.player.getMainHandItem().getItem()
                                                instanceof ToolAbilityItem;
                                case PLAYER ->
                                        index != 4
                                                || mc.player.getVehicle()
                                                                instanceof RailCarBaseEntity
                                                        && !(isGunHeld()
                                                                && KeybindPlatform.sameChord(
                                                                        this, RELOAD));
                                default -> true;
                            };
            if (current == activeDown) return;
            activeDown = current;
            if (mc.player == null) return;
            switch (action) {
                case PLAYER -> playerChanged(index, current);
                case TOOL -> toolChanged(index, current);
                case CRANE -> craneChanged(index, current);
                case GUN -> gunChanged(index, current);
                case ABILITY -> abilityChanged(index, current);
                case CALCULATOR -> calculatorChanged(current);
            }
        }
    }

    public static final class HeldKey extends KeyMapping {
        private boolean physicalDown;
        private InputConstants.Key pressedKey = InputConstants.UNKNOWN;

        private void observe(boolean down) {
            pressedKey = KeyMappingHelper.getBoundKeyOf((KeyMapping) this);
            physicalDown = down;
        }

        private HeldKey(String name, int code) {
            super(name, code, CATEGORY);
        }

        @Override
        public boolean isDown() {
            return RenderSystem.isOnRenderThread()
                    && Minecraft.getInstance().isWindowActive()
                    && !isUnbound()
                    && physicalDown
                    && pressedKey.equals(KeyMappingHelper.getBoundKeyOf((KeyMapping) this))
                    && KeybindPlatform.modifierMatches(this, modifiers);
        }

        @Override
        public void setKey(InputConstants.Key key) {
            physicalDown = false;
            super.setKey(key);
        }
    }
}
