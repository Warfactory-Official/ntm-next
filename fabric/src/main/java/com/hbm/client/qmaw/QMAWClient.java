// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.qmaw;

import com.hbm.client.Keybinds;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.platform.FabricKeybindPlatform;
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
        tooltip(ItemFluidIcon.make(fluid), lines);
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
                && FabricKeybindPlatform.modifierMatches(Keybinds.MANUAL, event.modifiers())
                && open(screen);
    }

    public static boolean mouse(Screen screen, MouseButtonEvent event) {
        return Keybinds.MANUAL.matchesMouse(event)
                && FabricKeybindPlatform.modifierMatches(Keybinds.MANUAL, event.modifiers())
                && open(screen);
    }

    private static boolean open(Screen screen) {
        if (screen != hoverScreen
                || screen instanceof ScreenQMAW
                || hovered == null
                || Keybinds.PRESENTATION.isDown()) return false;
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
