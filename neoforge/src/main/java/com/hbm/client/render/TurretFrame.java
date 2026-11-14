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
