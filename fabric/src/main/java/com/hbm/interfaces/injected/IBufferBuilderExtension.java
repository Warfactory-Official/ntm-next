// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces.injected;

import com.mojang.blaze3d.vertex.PoseStack;

public interface IBufferBuilderExtension {

    void hbm$ribbon(
            float hx,
            float hy,
            float hz,
            float tx,
            float ty,
            float tz,
            int headColor,
            int tailColor,
            float headWidth,
            float tailWidth,
            int light);

    boolean hbm$positionColor(float x, float y, float z, int color);

    boolean hbm$putPart(
            float[] src,
            int stride,
            int count,
            PoseStack.Pose pose,
            int color,
            int overlay,
            int light,
            float uScale,
            float vScale,
            float uOff,
            float vOff);

    boolean hbm$beginBlock(int count);

    void hbm$blockVertex(
            float x,
            float y,
            float z,
            int color,
            float u,
            float v,
            int overlay,
            int light,
            float nx,
            float ny,
            float nz);

    boolean hbm$beginColorBlock(int count);

    void hbm$colorVertex(float x, float y, float z, int color);

    void hbm$endBlock();
}
