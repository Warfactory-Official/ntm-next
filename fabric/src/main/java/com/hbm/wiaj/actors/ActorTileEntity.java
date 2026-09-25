// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actors;

import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;

public final class ActorTileEntity extends ActorBase {
    private final ITileActorRenderer renderer;

    public ActorTileEntity(ITileActorRenderer renderer) {
        this.renderer = renderer;
    }

    public ActorTileEntity(ITileActorRenderer renderer, CompoundTag data) {
        this.renderer = renderer;
        this.data = data;
    }

    @Override
    public void drawForegroundComponent(
            GuiGraphicsExtractor graphics, int w, int h, int ticks, float interp) {}

    @Override
    public void drawBackgroundComponent(JarRenderContext context, int ticks, float interp) {
        renderer.renderActor(context, ticks, interp, data);
    }

    @Override
    public void updateActor(JarScene scene) {
        renderer.updateActor(scene.script.ticksElapsed, data);
    }
}
