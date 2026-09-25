// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.lib.Library;
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

public final class ParticleInstance extends BillboardInstance {
    public static final InstanceType<ParticleInstance> TYPE = type("particle", "particle");
    public static final InstanceType<ParticleInstance> GROW_IN =
            type("particle_grow_in", "particle");
    public static final InstanceType<ParticleInstance> HADRON =
            type("particle_hadron", "particle_hadron");

    public ParticleInstance(InstanceType<? extends ParticleInstance> type, InstanceHandle handle) {
        super(type, handle);
    }

    private static InstanceType<ParticleInstance> type(String vertex, String cull) {
        return SimpleInstanceType.builder(ParticleInstance::new)
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
                .vertexShader(Library.id("instance/" + vertex + ".vert"))
                .cullShader(Library.id("instance/cull/" + cull + ".glsl"))
                .build();
    }

    public void previousPosition(float x, float y, float z) {
        ExtraMemoryOps.putVector3f(slabPtr() + 44, x, y, z);
    }

    public void animation(float previousRoll, float roll, float age, float lifetime) {
        ExtraMemoryOps.putVector4f(slabPtr() + 56, previousRoll, roll, age, lifetime);
    }
}
