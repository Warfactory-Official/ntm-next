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

package com.hbm.client.render.flywheel;

import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

public final class BeamInstance extends AbstractInstance {
    public static final InstanceType<BeamInstance> SOLID = type("beam_solid");
    public static final InstanceType<BeamInstance> LINE = type("beam_line");

    public BeamInstance(InstanceType<? extends BeamInstance> type, InstanceHandle handle) {
        super(type, handle);
    }

    public static void initTypes() {}

    private static InstanceType<BeamInstance> type(String vertex) {
        return SimpleInstanceType.builder(BeamInstance::new)
                .layout(
                        LayoutBuilder.create()
                                .vector("outerColor", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                                .vector("innerColor", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                                .matrix("pose", FloatRepr.FLOAT, 4)
                                .scalar("beamLength", FloatRepr.FLOAT)
                                .scalar("size", FloatRepr.FLOAT)
                                .scalar("thickness", FloatRepr.FLOAT)
                                .scalar("phase", IntegerRepr.INT)
                                .scalar("wave", IntegerRepr.INT)
                                .scalar("count", IntegerRepr.INT)
                                .build())
                .seed(pointer -> MemoryUtil.memSet(pointer, 0, 96))
                .vertexShader(RegistryUtil.id("instance/" + vertex + ".vert"))
                .cullShader(RegistryUtil.id("instance/cull/beam.glsl"))
                .build();
    }

    private static int rgba(int rgb, int alpha) {
        return (rgb >>> 16 & 255) | (rgb & 0xff00) | (rgb & 255) << 16 | alpha << 24;
    }

    public void update(
            Matrix4fc pose,
            float length,
            float size,
            float thickness,
            int phase,
            int wave,
            int count,
            int outer,
            int inner) {
        update(pose, length, size, thickness, phase, wave, count, outer, inner, 255);
    }

    public void update(
            Matrix4fc pose,
            float length,
            float size,
            float thickness,
            int phase,
            int wave,
            int count,
            int outer,
            int inner,
            int alpha) {
        setVisible(true);
        long ptr = slabPtr();
        MemoryUtil.memPutInt(ptr, rgba(outer, alpha));
        MemoryUtil.memPutInt(ptr + 4, rgba(inner, 255));
        ExtraMemoryOps.putMatrix4f(ptr + 8, pose);
        MemoryUtil.memPutFloat(ptr + 72, length);
        MemoryUtil.memPutFloat(ptr + 76, size);
        MemoryUtil.memPutFloat(ptr + 80, thickness);
        MemoryUtil.memPutInt(ptr + 84, phase);
        MemoryUtil.memPutInt(ptr + 88, wave);
        MemoryUtil.memPutInt(ptr + 92, count);
        setChanged();
    }
}
