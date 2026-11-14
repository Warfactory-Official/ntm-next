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

package com.hbm.client.render.flywheel.armature;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.api.layout.UnsignedIntegerRepr;
import java.util.function.LongConsumer;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class ArmatureType implements InstanceType<ArmatureInstance> {
    private final Layout layout =
            LayoutBuilder.create()
                    .matrix("rootPose", FloatRepr.FLOAT, 4)
                    .vectorArray("postRows", FloatRepr.FLOAT, 4, 3)
                    .vectorArray("poseKeys", FloatRepr.FLOAT, 4, 5)
                    .vector("orientationCurve", FloatRepr.FLOAT, 4)
                    .vector("clock", UnsignedIntegerRepr.UNSIGNED_INT, 4)
                    .vector("style", UnsignedIntegerRepr.UNSIGNED_INT, 4)
                    .vector("sphere", FloatRepr.FLOAT, 4)
                    .build();
    private final LongConsumer seed = address -> MemoryUtil.memSet(address, 0, layout.byteSize());

    @Override
    public ArmatureInstance create(InstanceHandle handle) {
        return new ArmatureInstance(this, handle);
    }

    @Override
    public Layout layout() {
        return layout;
    }

    @Override
    public LongConsumer seed() {
        return seed;
    }

    @Override
    public Identifier vertexShader() {
        return Identifier.fromNamespaceAndPath("hbm", "instance/armature/vertex.vert");
    }

    @Override
    public Identifier cullShader() {
        return Identifier.fromNamespaceAndPath("hbm", "instance/armature/cull.glsl");
    }

    @Override
    public boolean timeAnimated() {
        return true;
    }

    int offset(String field) {
        return layout.asMap().get(field).byteOffset();
    }
}
