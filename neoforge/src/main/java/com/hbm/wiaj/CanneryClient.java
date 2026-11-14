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

package com.hbm.wiaj;

import com.hbm.client.Keybinds;
import com.hbm.platform.KeybindPlatform;
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
                    Keybinds.PRESENTATION
                            .getTranslatedKeyMessage()
                            .copy()
                            .append(Component.literal(" + "))
                            .append(Keybinds.MANUAL.getTranslatedKeyMessage());
            tooltip.add(Component.translatable("cannery.f1", keys).withStyle(ChatFormatting.GREEN));
        }
        if (Thread.currentThread() == extracting && hoverScreen != null) hovered = lesson;
    }

    public static boolean key(Screen screen, KeyEvent event) {
        return Keybinds.MANUAL.matches(event)
                && KeybindPlatform.modifierMatches(Keybinds.MANUAL, event.modifiers())
                && open(screen);
    }

    public static boolean mouse(Screen screen, MouseButtonEvent event) {
        return Keybinds.MANUAL.matchesMouse(event)
                && KeybindPlatform.modifierMatches(Keybinds.MANUAL, event.modifiers())
                && open(screen);
    }

    private static boolean open(Screen screen) {
        if (!Keybinds.PRESENTATION.isDown()
                || screen != hoverScreen
                || hovered == null
                || screen instanceof WorldInAJarScreen) return false;
        Minecraft mc = Minecraft.getInstance();
        mc.player.closeContainer();
        mc.gui.setScreen(WorldInAJarScreen.of(hovered));
        return true;
    }

    public static void disconnected() {
        hoverScreen = null;
        hovered = null;
        extracting = null;
    }
}
