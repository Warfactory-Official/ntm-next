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

import com.hbm.render.anim.GpuAnimationTrack;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.api.layout.UnsignedIntegerRepr;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class DoorInstance extends TransformedInstance {
    public static final int KEYS = 4;
    public static final InstanceType<DoorInstance> TYPE =
            SimpleInstanceType.builder(DoorInstance::new)
                    .layout(
                            LayoutBuilder.create()
                                    .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                                    .vector("overlay", IntegerRepr.SHORT, 2)
                                    .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                                    .matrix("pose", FloatRepr.FLOAT, 4)
                                    .vector("timing", UnsignedIntegerRepr.UNSIGNED_INT, 4)
                                    .vector("shapes", UnsignedIntegerRepr.UNSIGNED_INT, 2)
                                    .vector("rest", FloatRepr.FLOAT, 2)
                                    .vectorArray("ends", UnsignedIntegerRepr.UNSIGNED_INT, KEYS, 2)
                                    .vectorArray("values", FloatRepr.FLOAT, KEYS, 2)
                                    .vector("bounds", FloatRepr.FLOAT, 4)
                                    .build())
                    .seed(
                            pointer -> {
                                MemoryUtil.memPutInt(pointer, 0xFFFFFFFF);
                                ExtraMemoryOps.put2x16(pointer + 4, OverlayTexture.NO_OVERLAY);
                                ExtraMemoryOps.putMatrix4f(pointer + 12, new Matrix4f());
                            })
                    .vertexShader(RegistryUtil.id("instance/door.vert"))
                    .cullShader(RegistryUtil.id("instance/cull/door.glsl"))
                    .timeAnimated()
                    .build();
    private static final int OFF_TIMING = offset("timing");
    private static final int OFF_SHAPES = offset("shapes");
    private static final int OFF_REST = offset("rest");
    private static final int OFF_ENDS = offset("ends");
    private static final int OFF_VALUES = offset("values");
    private static final int OFF_BOUNDS = offset("bounds");

    public DoorInstance(InstanceType<? extends DoorInstance> type, InstanceHandle handle) {
        super(type, handle);
    }

    public static void initTypes() {}

    private static int offset(String element) {
        return TYPE.layout().asMap().get(element).byteOffset();
    }

    public DoorInstance part(int kind, int part) {
        MemoryUtil.memPutInt(slabPtr() + OFF_TIMING + 8, kind | part << 8);
        return this;
    }

    public DoorInstance clips(
            GpuAnimationTrack first, GpuAnimationTrack second, long startMillis, int duration) {
        long ptr = slabPtr();
        MemoryUtil.memPutInt(ptr + OFF_TIMING, (int) startMillis);
        MemoryUtil.memPutInt(ptr + OFF_TIMING + 4, duration);
        MemoryUtil.memPutInt(
                ptr + OFF_TIMING + 12, first.ends().length | second.ends().length << 8);
        MemoryUtil.memPutInt(ptr + OFF_SHAPES, shapes(first));
        MemoryUtil.memPutInt(ptr + OFF_SHAPES + 4, shapes(second));
        MemoryUtil.memPutFloat(ptr + OFF_REST, first.rest());
        MemoryUtil.memPutFloat(ptr + OFF_REST + 4, second.rest());
        keys(ptr, 0, first);
        keys(ptr, KEYS, second);
        return this;
    }

    public DoorInstance silo(float openTicks, float direction, float timeToOpen, float[] origin) {
        long ptr = slabPtr();
        ExtraMemoryOps.putVector4f(ptr + OFF_VALUES, openTicks, direction, timeToOpen, 0F);
        ExtraMemoryOps.putVector4f(ptr + OFF_VALUES + 16, origin[0], origin[1], origin[2], 0F);
        return this;
    }

    public DoorInstance bounds(float x, float y, float z, float radius) {
        ExtraMemoryOps.putVector4f(slabPtr() + OFF_BOUNDS, x, y, z, radius);
        return this;
    }

    private static int shapes(GpuAnimationTrack track) {
        int packed = 0;
        for (int i = 0; i < track.shapes().length; i++) packed |= track.shapes()[i] << (i * 4);
        return packed;
    }

    private static void keys(long ptr, int first, GpuAnimationTrack track) {
        if (track.ends().length > KEYS) {
            throw new IllegalArgumentException(
                    "door channel of " + track.ends().length + " keys, at most " + KEYS);
        }
        for (int i = 0; i < KEYS; i++) {
            boolean present = i < track.ends().length;
            MemoryUtil.memPutInt(ptr + OFF_ENDS + (first + i) * 4L, present ? track.ends()[i] : 0);
            MemoryUtil.memPutFloat(
                    ptr + OFF_VALUES + (first + i) * 4L, present ? track.values()[i] : 0F);
        }
    }
}
