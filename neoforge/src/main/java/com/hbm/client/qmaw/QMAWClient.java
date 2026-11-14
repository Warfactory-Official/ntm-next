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

package com.hbm.client.qmaw;

import com.hbm.client.Keybinds;
import com.hbm.items.machine.FluidIconItem;
import com.hbm.platform.KeybindPlatform;
import com.hbm.qmaw.QMAWCatalog;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public final class QMAWClient {
    private static @Nullable Screen hoverScreen;
    private static @Nullable Identifier hovered;
    private static @Nullable QMAWCatalog hoverCatalog;
    private static @Nullable Thread extracting;

    private QMAWClient() {}

    public static QMAWCatalog catalog() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return QMAWCatalog.EMPTY;
        return connection.getConnection().isMemoryConnection()
                ? QMAWCatalog.server()
                : QMAWCatalog.remote();
    }

    public static void beginFrame() {
        hoverScreen = Minecraft.getInstance().gui.screen();
        hovered = null;
        hoverCatalog = null;
        extracting = Thread.currentThread();
    }

    public static void endFrame() {
        extracting = null;
    }

    public static void tooltip(ItemStack stack, List<Component> lines) {
        tooltip(stack, lines::add);
    }

    public static void fluidTooltip(Fluid fluid, Consumer<Component> lines) {
        tooltip(FluidIconItem.make(fluid), lines);
    }

    private static void tooltip(ItemStack stack, Consumer<Component> lines) {
        QMAWCatalog catalog = catalog();
        Identifier page = catalog.forStack(stack);
        if (page != null)
            lines.accept(
                    Component.translatable("qmaw.tab", Keybinds.MANUAL.getTranslatedKeyMessage())
                            .withStyle(ChatFormatting.YELLOW));
        if (Thread.currentThread() == extracting && hoverScreen != null) {
            hovered = page;
            hoverCatalog = catalog;
        }
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
        if (screen != hoverScreen
                || screen instanceof QMAWScreen
                || hovered == null
                || Keybinds.PRESENTATION.isDown()) return false;
        QMAWCatalog catalog = catalog();
        if (catalog != hoverCatalog) return false;
        Identifier page = hovered;
        if (catalog.page(page) == null) return false;
        Minecraft mc = Minecraft.getInstance();
        mc.player.closeContainer();
        mc.gui.setScreen(new QMAWScreen(catalog, page));
        return true;
    }

    public static void disconnected() {
        QMAWCatalog.clearRemote();
        hoverScreen = null;
        hovered = null;
        hoverCatalog = null;
        extracting = null;
    }
}
