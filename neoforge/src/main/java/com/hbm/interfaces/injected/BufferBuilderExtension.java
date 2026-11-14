/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.interfaces.injected;

import com.mojang.blaze3d.vertex.PoseStack;

public interface BufferBuilderExtension {
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
