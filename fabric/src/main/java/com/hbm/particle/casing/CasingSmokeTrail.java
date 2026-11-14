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

package com.hbm.particle.casing;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

public final class CasingSmokeTrail {
    private static final int STRIDE = 4;
    private static final double JITTER = 0.001D;
    private static final double RISE = 0.05D;

    private final int window;
    private final double lift;
    private final int nodeLife;
    private double[] nodes = new double[8 * STRIDE];
    private int count;
    private boolean anchored;
    private double anchorX;
    private double anchorY;
    private double anchorZ;

    private float[] frame = new float[8 * STRIDE];
    private int frameCount;
    private double frameX;
    private double frameY;
    private double frameZ;
    private float frameHalfX;
    private float frameHalfZ;
    private int frameLight;
    private boolean frameForVisual;

    public CasingSmokeTrail(int window, double lift, int nodeLife) {
        this.window = window;
        this.lift = lift;
        this.nodeLife = nodeLife;
    }

    public int getNodeCount() {
        return count;
    }

    public double getNode(int index, int component) {
        return nodes[index * STRIDE + component];
    }

    public void tick(int age, RandomSource random) {
        if (age > window) {
            count = 0;
            return;
        }
        double fade = 1D / nodeLife;
        for (int i = 0, end = count * STRIDE; i < end; i += STRIDE) {
            nodes[i] += random.nextGaussian() * JITTER;
            nodes[i + 2] += random.nextGaussian() * JITTER;
            nodes[i + 1] += RISE * lift;
            nodes[i + 3] = Math.max(0D, nodes[i + 3] - fade);
        }
        if (age < window) append(count == 0 ? 0D : 1D);
    }

    private void append(double alpha) {
        int at = count * STRIDE;
        if (at == nodes.length) nodes = Arrays.copyOf(nodes, nodes.length * 2);
        nodes[at] = 0D;
        nodes[at + 1] = 0D;
        nodes[at + 2] = 0D;
        nodes[at + 3] = alpha;
        count++;
    }

    public void setAnchor(double x, double y, double z) {
        if (!anchored) {
            anchored = true;
            anchorX = x;
            anchorY = y;
            anchorZ = z;
        }
        if (count > 0) {
            double dx = anchorX - x;
            double dy = anchorY - y;
            double dz = anchorZ - z;
            for (int i = 0, end = count * STRIDE; i < end; i += STRIDE) {
                nodes[i] += dx;
                nodes[i + 1] += dy;
                nodes[i + 2] += dz;
            }
        }
        anchorX = x;
        anchorY = y;
        anchorZ = z;
    }

    public void publish(
            int age,
            double originX,
            double originY,
            double originZ,
            float halfX,
            float halfZ,
            int light,
            boolean forVisual) {
        if (frame.length < count * STRIDE)
            frame = new float[Math.max(count * STRIDE, frame.length * 2)];
        double fade = 1D - (double) age / window;
        for (int i = 0, end = count * STRIDE; i < end; i += STRIDE) {
            frame[i] = (float) nodes[i];
            frame[i + 1] = (float) nodes[i + 1];
            frame[i + 2] = (float) nodes[i + 2];
            frame[i + 3] = (float) (nodes[i + 3] * fade);
        }
        frameCount = count;
        frameX = originX;
        frameY = originY;
        frameZ = originZ;
        frameHalfX = halfX;
        frameHalfZ = halfZ;
        frameLight = light;
        frameForVisual = forVisual;
    }

    public int getSegmentCount() {
        return Math.max(0, frameCount - 1);
    }

    public int getVisualSegmentCount() {
        return frameForVisual ? getSegmentCount() : 0;
    }

    public float[] getFrame() {
        return frame;
    }

    public double getFrameX() {
        return frameX;
    }

    public double getFrameY() {
        return frameY;
    }

    public double getFrameZ() {
        return frameZ;
    }

    public float getFrameHalfWidth() {
        return frameHalfX;
    }

    public float getFrameHalfDepth() {
        return frameHalfZ;
    }

    public int getFrameLight() {
        return frameLight;
    }
}
