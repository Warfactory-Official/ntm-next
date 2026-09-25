// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj;

import com.hbm.client.ModifierKeys;
import com.hbm.client.qmaw.QMAWClient;
import com.hbm.wiaj.cannery.CanneryBase;
import com.hbm.wiaj.cannery.Jars;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class CanneryClient {
    private static @Nullable Screen hoverScreen;
    private static @Nullable CanneryBase hovered;
    private static @Nullable Thread extracting;

    private CanneryClient() {}

    public static void beginFrame() {
        hoverScreen = Minecraft.getInstance().gui.screen();
        hovered = null;
        extracting = Thread.currentThread();
    }

    public static void endFrame() {
        extracting = null;
    }

    public static void tooltip(ItemStack stack, List<Component> tooltip) {
        CanneryBase lesson = Jars.find(stack);
        if (lesson != null) {
            Component keys =
                    Component.translatable("key.keyboard.left.shift")
                            .append(Component.literal(" + "))
                            .append(QMAWClient.OPEN_MANUAL.getTranslatedKeyMessage());
            tooltip.add(Component.translatable("cannery.f1", keys).withStyle(ChatFormatting.GREEN));
        }
        if (Thread.currentThread() == extracting && hoverScreen != null) hovered = lesson;
    }

    public static boolean key(Screen screen, KeyEvent event) {
        return QMAWClient.OPEN_MANUAL.matches(event) && open(screen);
    }

    public static boolean mouse(Screen screen, MouseButtonEvent event) {
        return QMAWClient.OPEN_MANUAL.matchesMouse(event) && open(screen);
    }

    private static boolean open(Screen screen) {
        if (!ModifierKeys.leftShiftHeld()
                || screen != hoverScreen
                || hovered == null
                || screen instanceof ScreenWorldInAJar) return false;
        Minecraft mc = Minecraft.getInstance();
        mc.player.closeContainer();
        mc.gui.setScreen(ScreenWorldInAJar.of(hovered));
        return true;
    }

    public static void disconnected() {
        hoverScreen = null;
        hovered = null;
        extracting = null;
    }
}
