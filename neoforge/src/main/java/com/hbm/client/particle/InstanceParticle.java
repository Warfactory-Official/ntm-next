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

package com.hbm.client.particle;

import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.IntegerRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.BillboardInstance;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import org.lwjgl.system.MemoryUtil;

public final class InstanceParticle extends BillboardInstance {
    public static final InstanceType<InstanceParticle> TYPE = type("particle", "particle");
    public static final InstanceType<InstanceParticle> GROW_IN =
            type("particle_grow_in", "particle");
    public static final InstanceType<InstanceParticle> HADRON =
            type("particle_hadron", "particle_hadron");
    public static final InstanceType<InstanceParticle> UPRIGHT =
            type("particle_upright", "particle");
    public static final InstanceType<InstanceParticle> RBMK_JET =
            type("particle_rbmk_jet", "particle");
    public static final InstanceType<InstanceParticle> CASING_SMOKE =
            type("particle_casing_smoke", "particle_casing_smoke");

    public InstanceParticle(InstanceType<? extends InstanceParticle> type, InstanceHandle handle) {
        super(type, handle);
    }

    public static void initTypes() {}

    private static InstanceType<InstanceParticle> type(String vertex, String cull) {
        return SimpleInstanceType.builder(InstanceParticle::new)
                .layout(
                        LayoutBuilder.create()
                                .vector("color", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                                .vector("overlay", IntegerRepr.SHORT, 2)
                                .vector("light", FloatRepr.UNSIGNED_SHORT, 2)
                                .vector("position", FloatRepr.FLOAT, 3)
                                .scalar("size", FloatRepr.FLOAT)
                                .vector("uvRegion", FloatRepr.FLOAT, 4)
                                .vector("previousPosition", FloatRepr.FLOAT, 3)
                                .scalar("previousRoll", FloatRepr.FLOAT)
                                .scalar("roll", FloatRepr.FLOAT)
                                .scalar("age", FloatRepr.FLOAT)
                                .scalar("lifetime", FloatRepr.FLOAT)
                                .build())
                .seed(
                        pointer -> {
                            MemoryUtil.memPutInt(pointer, -1);
                            ExtraMemoryOps.put2x16(pointer + 4, OverlayTexture.NO_OVERLAY);
                            MemoryUtil.memPutFloat(pointer + 24, 1);
                            ExtraMemoryOps.putVector4f(pointer + 28, 0, 0, 1, 1);
                        })
                .vertexShader(RegistryUtil.id("instance/" + vertex + ".vert"))
                .cullShader(RegistryUtil.id("instance/cull/" + cull + ".glsl"))
                .build();
    }

    public void previousPosition(float x, float y, float z) {
        ExtraMemoryOps.putVector3f(slabPtr() + 44, x, y, z);
    }

    public void animation(float previousRoll, float roll, float age, float lifetime) {
        ExtraMemoryOps.putVector4f(slabPtr() + 56, previousRoll, roll, age, lifetime);
    }
}
