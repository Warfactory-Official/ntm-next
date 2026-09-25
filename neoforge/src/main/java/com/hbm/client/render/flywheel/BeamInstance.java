// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.lib.Library;
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

    private static InstanceType<BeamInstance> type(String vertex) {
        return SimpleInstanceType.builder(BeamInstance::new)
                .layout(
                        LayoutBuilder.create()
                                .vector("outerColor", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                                .vector("innerColor", FloatRepr.NORMALIZED_UNSIGNED_BYTE, 4)
                                .matrix("pose", FloatRepr.FLOAT, 4)
                                .scalar("length", FloatRepr.FLOAT)
                                .scalar("size", FloatRepr.FLOAT)
                                .scalar("thickness", FloatRepr.FLOAT)
                                .scalar("phase", IntegerRepr.INT)
                                .scalar("wave", IntegerRepr.INT)
                                .scalar("count", IntegerRepr.INT)
                                .build())
                .seed(pointer -> MemoryUtil.memSet(pointer, 0, 96))
                .vertexShader(Library.id("instance/" + vertex + ".vert"))
                .cullShader(Library.id("instance/cull/beam.glsl"))
                .build();
    }

    private static int rgba(int rgb) {
        return (rgb >>> 16 & 255) | (rgb & 0xff00) | (rgb & 255) << 16 | 0xff000000;
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
        setVisible(true);
        long ptr = slabPtr();
        MemoryUtil.memPutInt(ptr, rgba(outer));
        MemoryUtil.memPutInt(ptr + 4, rgba(inner));
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
