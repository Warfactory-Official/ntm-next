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

import com.hbm.client.render.GlyphidAnimationData;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.InstanceTransforms;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

public final class GlyphidInstance extends TransformedInstance {
    private static final Layout LAYOUT =
            LayoutBuilder.create()
                    .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                    .vector("overlay", IntegerRepr.SHORT, 2)
                    .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                    .matrix("pose", FloatRepr.FLOAT, 4)
                    .vector("joints", FloatRepr.FLOAT, 3)
                    .scalar("part", IntegerRepr.INT)
                    .build();
    private static final int JOINTS = LAYOUT.asMap().get("joints").byteOffset();
    private static final int PART = LAYOUT.asMap().get("part").byteOffset();

    public static final InstanceType<GlyphidInstance> TYPE =
            SimpleInstanceType.<GlyphidInstance>builder(GlyphidInstance::new)
                    .layout(LAYOUT)
                    .seed(InstanceTypes.TRANSFORMED.seed())
                    .transform(GlyphidInstance::transform)
                    .vertexShader(Identifier.fromNamespaceAndPath("hbm", "instance/glyphid.vert"))
                    .cullShader(Identifier.fromNamespaceAndPath("hbm", "cull/glyphid.glsl"))
                    .build();

    public GlyphidInstance(InstanceType<? extends GlyphidInstance> type, InstanceHandle handle) {
        super(type, handle);
    }

    public GlyphidInstance part(int part) {
        MemoryUtil.memPutInt(slabPtr() + PART, part);
        return this;
    }

    public GlyphidInstance joints(float first, float second, float third) {
        long address = slabPtr() + JOINTS;
        MemoryUtil.memPutFloat(address, first);
        MemoryUtil.memPutFloat(address + Float.BYTES, second);
        MemoryUtil.memPutFloat(address + 2 * Float.BYTES, third);
        return this;
    }

    private static void transform(long address, Matrix4fc view, Matrix4f destination) {
        InstanceTransforms.TRANSFORMED.write(address, view, destination);
        GlyphidAnimationData.appendPose(
                destination,
                MemoryUtil.memGetInt(address + PART),
                MemoryUtil.memGetFloat(address + JOINTS),
                MemoryUtil.memGetFloat(address + JOINTS + Float.BYTES),
                MemoryUtil.memGetFloat(address + JOINTS + 2 * Float.BYTES));
    }
}
