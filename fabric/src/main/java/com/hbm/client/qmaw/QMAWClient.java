// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.qmaw;

import com.hbm.client.ModifierKeys;
import com.hbm.client.ToolKeybindHandler;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.qmaw.QMAWCatalog;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class QMAWClient {
    public static final KeyMapping OPEN_MANUAL =
            new KeyMapping("key.hbm.qmaw", GLFW.GLFW_KEY_F1, ToolKeybindHandler.CATEGORY);
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

        tooltip(ItemFluidIcon.make(fluid), lines);
    }

    private static void tooltip(ItemStack stack, Consumer<Component> lines) {
        QMAWCatalog catalog = catalog();
        Identifier page = catalog.forStack(stack);
        if (page != null)
            lines.accept(
                    Component.translatable("qmaw.tab", OPEN_MANUAL.getTranslatedKeyMessage())
                            .withStyle(ChatFormatting.YELLOW));

        if (Thread.currentThread() == extracting && hoverScreen != null) {
            hovered = page;
            hoverCatalog = catalog;
        }
    }

    public static boolean key(Screen screen, KeyEvent event) {
        return OPEN_MANUAL.matches(event) && open(screen);
    }

    public static boolean mouse(Screen screen, MouseButtonEvent event) {
        return OPEN_MANUAL.matchesMouse(event) && open(screen);
    }

    private static boolean open(Screen screen) {
        if (screen != hoverScreen
                || screen instanceof ScreenQMAW
                || hovered == null
                || ModifierKeys.leftShiftHeld()) return false;
        QMAWCatalog catalog = catalog();
        if (catalog != hoverCatalog) return false;
        Identifier page = hovered;
        if (catalog.page(page) == null) return false;
        Minecraft mc = Minecraft.getInstance();
        mc.player.closeContainer();
        mc.gui.setScreen(new ScreenQMAW(catalog, page));
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
