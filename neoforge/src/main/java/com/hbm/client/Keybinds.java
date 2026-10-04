// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.client.gui.ScreenCalculator;
import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.EnumCraneKey;
import com.hbm.handler.EnumToolKey;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.IKeybindReceiver;
import com.hbm.items.tool.ItemSettingsTool;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.Library;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toserver.CraneKeyPayload;
import com.hbm.packet.toserver.KeybindPayload;
import com.hbm.packet.toserver.ToolKeyPayload;
import com.hbm.platform.NeoForgeKeybindPlatform;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class Keybinds {
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Library.id("key"));

    public static final GameplayKey TOOL_ALT =
            new GameplayKey(
                    "key.hbm.copyToolAlt", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY, Action.TOOL, 0);
    public static final GameplayKey TOOL_CTRL =
            new GameplayKey(
                    "key.hbm.copyToolCtrl", GLFW.GLFW_KEY_LEFT_CONTROL, CATEGORY, Action.TOOL, 1);
    public static final GameplayKey CRANE_UP =
            new GameplayKey(
                    "key.hbm.craneUp", GLFW.GLFW_KEY_UP, KeyMapping.Category.MISC, Action.CRANE, 0);
    public static final GameplayKey CRANE_DOWN =
            new GameplayKey(
                    "key.hbm.craneDown",
                    GLFW.GLFW_KEY_DOWN,
                    KeyMapping.Category.MISC,
                    Action.CRANE,
                    1);
    public static final GameplayKey CRANE_LEFT =
            new GameplayKey(
                    "key.hbm.craneLeft",
                    GLFW.GLFW_KEY_LEFT,
                    KeyMapping.Category.MISC,
                    Action.CRANE,
                    2);
    public static final GameplayKey CRANE_RIGHT =
            new GameplayKey(
                    "key.hbm.craneRight",
                    GLFW.GLFW_KEY_RIGHT,
                    KeyMapping.Category.MISC,
                    Action.CRANE,
                    3);
    public static final GameplayKey CRANE_LOAD =
            new GameplayKey(
                    "key.hbm.craneLoad",
                    GLFW.GLFW_KEY_ENTER,
                    KeyMapping.Category.MISC,
                    Action.CRANE,
                    4);
    public static final GameplayKey TOGGLE_HUD =
            new GameplayKey("key.hbm.toggleHUD", GLFW.GLFW_KEY_V, CATEGORY, Action.PLAYER, 0);
    public static final GameplayKey TOGGLE_BACKPACK =
            new GameplayKey("key.hbm.toggleBack", GLFW.GLFW_KEY_C, CATEGORY, Action.PLAYER, 1);
    public static final GameplayKey TOGGLE_MAGNET =
            new GameplayKey("key.hbm.toggleMagnet", GLFW.GLFW_KEY_Z, CATEGORY, Action.PLAYER, 2);
    public static final GameplayKey DASH =
            new GameplayKey("key.hbm.dash", GLFW.GLFW_KEY_LEFT_SHIFT, CATEGORY, Action.PLAYER, 3);
    public static final GameplayKey TRAIN_INVENTORY =
            new GameplayKey("key.hbm.trainInv", GLFW.GLFW_KEY_R, CATEGORY, Action.PLAYER, 4);
    public static final GameplayKey GUN_PRIMARY =
            new GameplayKey(
                    "key.hbm.gunPrimary",
                    InputConstants.Type.MOUSE,
                    GLFW.GLFW_MOUSE_BUTTON_LEFT,
                    CATEGORY,
                    Action.GUN,
                    0);
    public static final GameplayKey GUN_SECONDARY =
            new GameplayKey(
                    "key.hbm.gunSecondary",
                    InputConstants.Type.MOUSE,
                    GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                    CATEGORY,
                    Action.GUN,
                    1);
    public static final GameplayKey GUN_TERTIARY =
            new GameplayKey(
                    "key.hbm.gunTertiary",
                    InputConstants.Type.MOUSE,
                    GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
                    CATEGORY,
                    Action.GUN,
                    2);
    public static final GameplayKey RELOAD =
            new GameplayKey("key.hbm.reload", GLFW.GLFW_KEY_R, CATEGORY, Action.GUN, 3);
    public static final GameplayKey ABILITY_CYCLE =
            new GameplayKey(
                    "key.hbm.ability",
                    InputConstants.Type.MOUSE,
                    GLFW.GLFW_MOUSE_BUTTON_RIGHT,
                    CATEGORY,
                    Action.ABILITY,
                    0);
    public static final GameplayKey ABILITY_CONFIGURE =
            new GameplayKey(
                    "key.hbm.abilityAlt", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY, Action.ABILITY, 1);
    public static final GameplayKey CALCULATOR =
            new GameplayKey("hbm.key.calculator", GLFW.GLFW_KEY_N, CATEGORY, Action.CALCULATOR, 0);
    public static final KeyMapping MANUAL =
            new KeyMapping("key.hbm.qmaw", GLFW.GLFW_KEY_F1, CATEGORY);
    public static final HeldKey DETAILS =
            new HeldKey("key.hbm.tooltipDetails", GLFW.GLFW_KEY_LEFT_SHIFT);
    public static final HeldKey PRESENTATION =
            new HeldKey("key.hbm.presentation", GLFW.GLFW_KEY_LEFT_SHIFT);
    public static final HeldKey DIAGNOSTICS =
            new HeldKey("key.hbm.guiDiagnostics", GLFW.GLFW_KEY_LEFT_ALT);
    public static final HeldKey BATCH = new HeldKey("key.hbm.guiBatch", GLFW.GLFW_KEY_LEFT_SHIFT);

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
    private static final EnumKeybind[] PLAYER_KEYS = {
        EnumKeybind.TOGGLE_HEAD,
        EnumKeybind.TOGGLE_JETPACK,
        EnumKeybind.TOGGLE_MAGNET,
        EnumKeybind.DASH,
        EnumKeybind.TRAIN,
    };
    private static final EnumKeybind[] GUN_KEYS = {
        EnumKeybind.GUN_PRIMARY,
        EnumKeybind.GUN_SECONDARY,
        EnumKeybind.GUN_TERTIARY,
        EnumKeybind.RELOAD,
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
                key.observe(action != GLFW.GLFW_RELEASE);
        }
    }

    public static void mouse(long window, MouseButtonInfo event, int action) {
        if (window != Minecraft.getInstance().getWindow().handle()) return;
        modifiers = event.modifiers();
        InputConstants.Key button = InputConstants.Type.MOUSE.getOrCreate(event.button());
        for (KeyMapping mapping : ALL) {
            if (mapping instanceof HeldKey key && ((KeyMapping) key).getKey().equals(button))
                key.observe(action != GLFW.GLFW_RELEASE);
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
            HbmPlayerProps props = HbmPlayerProps.getData(mc.player);
            boolean jump = mc.gui.screen() == null && mc.options.keyJump.isDown();
            if (jump != props.getKeyPressed(EnumKeybind.JETPACK)) {
                props.setKeyPressed(EnumKeybind.JETPACK, jump);
                PacketWire.sendToServer(new KeybindPayload(EnumKeybind.JETPACK, jump));
            }
        }
        if (calculatorRequested) {
            calculatorRequested = false;
            if (mc.player != null) {
                mc.player.closeContainer();
                mc.gui.setScreen(new ScreenCalculator());
            }
        }
        if (cyclePressed
                && (mc.player == null || mc.gui.screen() != null || !ABILITY_CYCLE.isDown())) {
            if (mc.player != null) sendCycle(false);
            cyclePressed = false;
        }
    }

    private static void playerChanged(int index, boolean current) {
        HbmPlayerProps.getData(Minecraft.getInstance().player)
                .setKeyPressed(PLAYER_KEYS[index], current);
        PacketWire.sendToServer(new KeybindPayload(PLAYER_KEYS[index], current));
    }

    private static void toolChanged(int index, boolean current) {
        HbmPlayerProps.getData(Minecraft.getInstance().player)
                .setToolKeyPressed(EnumToolKey.VALUES[index], current);
        PacketWire.sendToServer(new ToolKeyPayload(index, current));
    }

    private static void craneChanged(int index, boolean current) {
        HbmPlayerProps.getData(Minecraft.getInstance().player)
                .setCraneKeyPressed(EnumCraneKey.VALUES[index], current);
        PacketWire.sendToServer(new CraneKeyPayload(index, current));
    }

    private static void gunChanged(int index, boolean current) {
        Player player = Minecraft.getInstance().player;
        EnumKeybind key = GUN_KEYS[index];
        HbmPlayerProps.getData(player).setKeyPressed(key, current);
        PacketWire.sendToServer(new KeybindPayload(key, current));
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()
                && held.getItem() instanceof IKeybindReceiver receiver
                && receiver.canHandleKeybind(player, held, key))
            receiver.handleKeybindClient(player, held, key, current);
    }

    public static boolean isGunKey(KeyMapping key) {
        return (!((KeyMapping) GUN_PRIMARY).isUnbound()
                        && ((KeyMapping) GUN_PRIMARY).getKey().equals(key.getKey())
                        && ((KeyMapping) GUN_PRIMARY).getKeyModifier() == key.getKeyModifier())
                || (!((KeyMapping) GUN_SECONDARY).isUnbound()
                        && ((KeyMapping) GUN_SECONDARY).getKey().equals(key.getKey())
                        && ((KeyMapping) GUN_SECONDARY).getKeyModifier() == key.getKeyModifier())
                || (!((KeyMapping) GUN_TERTIARY).isUnbound()
                        && ((KeyMapping) GUN_TERTIARY).getKey().equals(key.getKey())
                        && ((KeyMapping) GUN_TERTIARY).getKeyModifier() == key.getKeyModifier())
                || (!((KeyMapping) RELOAD).isUnbound()
                        && ((KeyMapping) RELOAD).getKey().equals(key.getKey())
                        && ((KeyMapping) RELOAD).getKeyModifier() == key.getKeyModifier());
    }

    public static boolean isGunHeld() {
        Player player = Minecraft.getInstance().player;
        return player != null && player.getMainHandItem().getItem() instanceof ItemGunBaseNT;
    }

    private static void abilityChanged(int index, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        if (index == 0) {
            if (pressed && NeoForgeKeybindPlatform.sameChord(ABILITY_CYCLE, mc.options.keyUse))
                return;
            if (cyclePressed != pressed) {
                sendCycle(pressed);
                cyclePressed = pressed;
            }
        } else if (pressed) {
            Player player = mc.player;
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof IKeybindReceiver receiver
                    && receiver.canHandleKeybind(player, held, EnumKeybind.ABILITY_ALT))
                receiver.handleKeybindClient(player, held, EnumKeybind.ABILITY_ALT, true);
        }
    }

    public static boolean cycleFromUse() {
        if (!NeoForgeKeybindPlatform.sameChord(
                ABILITY_CYCLE, Minecraft.getInstance().options.keyUse)) return false;
        if (!cyclePressed) {
            sendCycle(true);
            cyclePressed = true;
        }
        return true;
    }

    private static void sendCycle(boolean pressed) {
        PacketWire.sendToServer(new KeybindPayload(EnumKeybind.ABILITY_CYCLE, pressed));
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
            ((KeyMapping) this).setKeyConflictContext(KeyConflictContext.IN_GAME);
        }

        @Override
        public void setDown(boolean down) {
            downKey = ((KeyMapping) this).getKey();
            super.setDown(down);
            refresh();
        }

        @Override
        public void setKey(InputConstants.Key key) {
            setDown(false);
            super.setKey(key);
        }

        private void refresh() {
            if (!downKey.equals(((KeyMapping) this).getKey())) super.setDown(false);
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
                                                instanceof ItemSettingsTool;
                                case ABILITY ->
                                        mc.player.getMainHandItem().getItem()
                                                instanceof ItemToolAbility;
                                case PLAYER ->
                                        index != 4
                                                || mc.player.getVehicle()
                                                                instanceof EntityRailCarBase
                                                        && !(isGunHeld()
                                                                && (!((KeyMapping) this).isUnbound()
                                                                        && ((KeyMapping) this)
                                                                                .getKey()
                                                                                .equals(
                                                                                        ((KeyMapping)
                                                                                                        RELOAD)
                                                                                                .getKey())
                                                                        && ((KeyMapping) this)
                                                                                        .getKeyModifier()
                                                                                == ((KeyMapping)
                                                                                                RELOAD)
                                                                                        .getKeyModifier()));
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
            pressedKey = ((KeyMapping) this).getKey();
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
                    && pressedKey.equals(((KeyMapping) this).getKey())
                    && NeoForgeKeybindPlatform.modifierMatches(this, modifiers);
        }

        @Override
        public void setKey(InputConstants.Key key) {
            physicalDown = false;
            super.setKey(key);
        }
    }
}
