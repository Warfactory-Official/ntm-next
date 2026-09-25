// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

public final class GlyphidRenderState extends EntityRenderState {

    public Identifier texture;
    public float bodyYaw;
    public double scale;
    public byte armor;
    public byte subtype;
    public float walkCycle;
    public float swingProgress;
    public float deathAge;
}
