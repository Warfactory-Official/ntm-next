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
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.api.layout.UnsignedIntegerRepr;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

public final class ScrollInstance extends UvTransformedInstance {
    public static final InstanceType<ScrollInstance> TYPE =
            SimpleInstanceType.builder(ScrollInstance::new)
                    .layout(
                            LayoutBuilder.create()
                                    .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                                    .vector("overlay", IntegerRepr.SHORT, 2)
                                    .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                                    .matrix("pose", FloatRepr.FLOAT, 4)
                                    .vector("uvRegion", FloatRepr.FLOAT, 4)
                                    .vector("scroll", FloatRepr.FLOAT, 4)
                                    .vector("clock", UnsignedIntegerRepr.UNSIGNED_INT, 3)
                                    .build())
                    .seed(
                            pointer -> {
                                MemoryUtil.memPutInt(pointer, 0xFFFFFFFF);
                                ExtraMemoryOps.put2x16(pointer + 4, OverlayTexture.NO_OVERLAY);
                                ExtraMemoryOps.putMatrix4f(pointer + 12, new Matrix4f());
                                ExtraMemoryOps.putVector4f(pointer + 76, 0F, 0F, 1F, 1F);
                            })
                    .vertexShader(RegistryUtil.id("instance/scroll.vert"))
                    .cullShader(
                            Identifier.fromNamespaceAndPath(
                                    "flywheel", "instance/cull/transformed.glsl"))
                    .timeAnimated()
                    .build();
    private static final int OFF_SCROLL = offset("scroll");
    private static final int OFF_CLOCK = offset("clock");

    public ScrollInstance(InstanceType<? extends ScrollInstance> type, InstanceHandle handle) {
        super(type, handle);
    }

    public static void initTypes() {}

    private static int offset(String element) {
        Layout.Element found = TYPE.layout().asMap().get(element);
        return found.byteOffset();
    }

    public ScrollInstance direction(float u, float v) {
        long ptr = slabPtr();
        MemoryUtil.memPutFloat(ptr + OFF_SCROLL + 8, u);
        MemoryUtil.memPutFloat(ptr + OFF_SCROLL + 12, v);
        return this;
    }

    public ScrollInstance scroll(float previous, float current) {
        long ptr = slabPtr();
        MemoryUtil.memPutFloat(ptr + OFF_SCROLL, previous);
        MemoryUtil.memPutFloat(ptr + OFF_SCROLL + 4, current);
        return this;
    }

    public ScrollInstance clock(long base, int period) {
        if (period < 0) throw new IllegalArgumentException("clock period " + period);
        long ptr = slabPtr();
        MemoryUtil.memPutInt(ptr + OFF_CLOCK, (int) base);
        MemoryUtil.memPutInt(ptr + OFF_CLOCK + 4, period);
        MemoryUtil.memPutInt(ptr + OFF_CLOCK + 8, 1);
        return this;
    }
}
