// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.recipeviewer;

import net.minecraft.network.chat.Component;

public interface PageLayout {

    PageSlot input(int x, int y);

    PageSlot output(int x, int y);

    PageSlot catalyst(int x, int y);

    PageSlot display(int x, int y);

    void arrow(int x, int y);

    void text(Component text, int x, int y, int width, int height);
}
