// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.render.loader.HFRWavefrontObject;
import java.util.ArrayList;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class TurretFrame {

    private final ArrayList<Part> parts = new ArrayList<>();
    private int size;

    public Part push() {
        while (parts.size() <= size) parts.add(new Part());
        Part part = parts.get(size++);
        part.reset();
        return part;
    }

    public void rewind() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public Part get(int index) {
        return parts.get(index);
    }

    public static final class Part {

        public final Matrix4f pose = new Matrix4f();
        public @Nullable HFRWavefrontObject model;

        public int group = -1;
        public @Nullable Identifier texture;

        void reset() {
            model = null;
            group = -1;
            texture = null;
            pose.identity();
        }

        public Part obj(HFRWavefrontObject model, int group) {
            this.model = model;
            this.group = group;
            return this;
        }

        public Part texture(Identifier texture) {
            this.texture = texture;
            return this;
        }
    }
}
