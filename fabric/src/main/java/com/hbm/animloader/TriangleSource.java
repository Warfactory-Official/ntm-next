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

package com.hbm.animloader;

public final class TriangleSource {
    public static final int COMPONENTS = 8;

    private final float[] corners;
    private final float radius;

    public TriangleSource(float[] corners) {
        if (corners.length % (3 * COMPONENTS) != 0) {
            throw new IllegalArgumentException("Incomplete triangle corners");
        }
        this.corners = corners.clone();
        double radiusSquared = 0;
        for (int offset = 0; offset < corners.length; offset += COMPONENTS) {
            for (int component = 0; component < COMPONENTS; component++) {
                if (!Float.isFinite(corners[offset + component])) {
                    throw new IllegalArgumentException("Nonfinite triangle attribute");
                }
            }
            float x = corners[offset];
            float y = corners[offset + 1];
            float z = corners[offset + 2];
            radiusSquared =
                    Math.max(radiusSquared, (double) x * x + (double) y * y + (double) z * z);
        }
        radius = Math.nextUp((float) Math.sqrt(radiusSquared));
    }

    public int getCornerCount() {
        return corners.length / COMPONENTS;
    }

    public float getComponent(int corner, int component) {
        return corners[corner * COMPONENTS + component];
    }

    public float[] copyCorners() {
        return corners.clone();
    }

    public float getRadius() {
        return radius;
    }
}
