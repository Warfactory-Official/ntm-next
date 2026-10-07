// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface DeferredGuiDraws {

    void hbm$defer(Consumer<GuiGraphicsExtractor> draw);
}
