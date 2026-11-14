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

package com.hbm.client.render;

import com.hbm.animloader.TriangleSource;
import com.hbm.render.loader.HFRWavefrontObject;
import java.util.ArrayList;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

public final class DoorFrame {
    private final ArrayList<Part> parts = new ArrayList<>();
    public boolean cull;
    public boolean blended;
    private int size;

    public Part push() {
        while (parts.size() <= size) parts.add(new Part());
        Part part = parts.get(size++);
        part.reset();
        return part;
    }

    public void rewind() {
        size = 0;
        cull = true;
        blended = false;
    }

    public int size() {
        return size;
    }

    public Part get(int index) {
        return parts.get(index);
    }

    public enum Clip {
        NONE,
        HALFSPACE,
        SLAB
    }

    public static final class Part {
        public final Matrix4f pose = new Matrix4f();
        public final Vector3f slide = new Vector3f();
        public final Vector4f plane = new Vector4f();
        private final Matrix4f drawPose = new Matrix4f();
        private final float[][] both = {new float[4], new float[4]};
        private final float[][] one = {both[0]};
        public @Nullable HFRWavefrontObject model;
        public int group = -1;
        public @Nullable TriangleSource mesh;
        public @Nullable Identifier texture;
        public Clip clip = Clip.NONE;
        public boolean hidden;

        private static void set(float[] p, float x, float y, float z, float d) {
            p[0] = x;
            p[1] = y;
            p[2] = z;
            p[3] = d;
        }

        void reset() {
            model = null;
            group = -1;
            mesh = null;
            texture = null;
            pose.identity();
            slide.zero();
            plane.zero();
            clip = Clip.NONE;
            hidden = false;
        }

        public Part obj(HFRWavefrontObject model, int group) {
            this.model = model;
            this.group = group;
            return this;
        }

        public Part node(TriangleSource mesh) {
            this.mesh = mesh;
            return this;
        }

        public Part texture(Identifier texture) {
            this.texture = texture;
            return this;
        }

        public Part slide(float x, float y, float z) {
            slide.set(x, y, z);
            return this;
        }

        public Part halfspace(float nx, float ny, float nz, float threshold) {
            plane.set(nx, ny, nz, threshold);
            clip = Clip.HALFSPACE;
            return this;
        }

        public Part slab(float nx, float ny, float nz, float threshold) {
            plane.set(nx, ny, nz, threshold);
            clip = Clip.SLAB;
            return this;
        }

        public Matrix4f drawPose() {
            return drawPose.set(pose).translate(slide);
        }

        public float @Nullable [][] cpuPlanes() {
            if (clip == Clip.NONE) return null;
            float nx = plane.x, ny = plane.y, nz = plane.z, w = plane.w;
            float d = nx * slide.x + ny * slide.y + nz * slide.z;
            set(both[0], -nx, -ny, -nz, d - w);
            if (clip == Clip.HALFSPACE) return one;
            set(both[1], nx, ny, nz, -w - d);
            return both;
        }
    }
}
