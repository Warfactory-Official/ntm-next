// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actors;

import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

public interface ISpecialActor {
    void drawForegroundComponent(
            GuiGraphicsExtractor graphics, int w, int h, int ticks, float interp);

    void drawBackgroundComponent(JarRenderContext context, int ticks, float interp);

    void updateActor(JarScene scene);

    void setActorData(CompoundTag data);

    void setDataPoint(String tag, Object value);
}
