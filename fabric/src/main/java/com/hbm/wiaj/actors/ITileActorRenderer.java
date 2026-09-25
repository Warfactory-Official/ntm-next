// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.actors;

import com.hbm.wiaj.JarRenderContext;
import net.minecraft.nbt.CompoundTag;

public interface ITileActorRenderer {
    void renderActor(JarRenderContext context, int ticks, float interp, CompoundTag data);

    void updateActor(int ticks, CompoundTag data);
}
