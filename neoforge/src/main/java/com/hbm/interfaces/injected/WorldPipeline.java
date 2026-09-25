// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.hbm.client.render.WorldRenderPipeline;
import com.mojang.blaze3d.opengl.GlRenderPipeline;

public interface WorldPipeline {
    GlRenderPipeline hbm$program(WorldRenderPipeline pipeline);
}
