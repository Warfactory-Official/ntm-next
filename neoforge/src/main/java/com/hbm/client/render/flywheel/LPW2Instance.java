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

import com.hbm.client.render.LPW2AnimationData;
import com.hbm.client.render.LPW2Clock;
import com.hbm.client.render.LPW2Poses;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class LPW2Instance extends AbstractInstance {
    private static final Layout LAYOUT =
            LayoutBuilder.create()
                    .vector("base", FloatRepr.FLOAT, 4)
                    .vector("phase0", FloatRepr.FLOAT, 4)
                    .vector("phase1", FloatRepr.FLOAT, 3)
                    .vector("meta", FloatRepr.FLOAT, 4)
                    .vector("data0", FloatRepr.FLOAT, 4)
                    .vector("data1", FloatRepr.FLOAT, 4)
                    .vector("law0", FloatRepr.FLOAT, 4)
                    .vector("law1", FloatRepr.FLOAT, 4)
                    .build();
    private static final int BASE = offset("base");
    private static final int PHASE_0 = offset("phase0");
    private static final int PHASE_1 = offset("phase1");
    private static final int META = offset("meta");
    private static final int DATA_0 = offset("data0");
    private static final int DATA_1 = offset("data1");
    private static final int LAW_0 = offset("law0");
    private static final int LAW_1 = offset("law1");

    public static final InstanceType<LPW2Instance> TYPE =
            SimpleInstanceType.<LPW2Instance>builder(LPW2Instance::new)
                    .layout(LAYOUT)
                    .seed(_ -> {})
                    .vertexShader(
                            Identifier.fromNamespaceAndPath("hbm", "instance/lpw2/vertex.vert"))
                    .cullShader(Identifier.fromNamespaceAndPath("hbm", "instance/lpw2/cull.glsl"))
                    .build();

    private LPW2Instance(InstanceType<LPW2Instance> type, InstanceHandle handle) {
        super(type, handle);
    }

    public void initialize(int slot, float x, float y, float z, float yaw) {
        long address = slabPtr();
        put4(address + BASE, x + 0.5f, y, z + 0.5f, (float) Math.toRadians(yaw));
        int row = slot * LPW2AnimationData.COLUMNS;
        double[] data = LPW2AnimationData.PARTS;
        put4(
                address + META,
                (float) data[row + LPW2AnimationData.KIND],
                (float) data[row + LPW2AnimationData.SIGNAL_0],
                (float) data[row + LPW2AnimationData.SIGNAL_1],
                slot == LPW2Poses.SCREEN ? 1 : 0);
        putColumns(address + DATA_0, data, row + LPW2AnimationData.DATA_0);
        putColumns(address + DATA_1, data, row + LPW2AnimationData.DATA_1);
        put4(
                address + LAW_0,
                (float) LPW2AnimationData.HALF_PI,
                (float) LPW2AnimationData.AXIAL_OFFSET,
                (float) LPW2AnimationData.ROCK_NORMALIZATION,
                (float) LPW2AnimationData.ROTOR_NORMALIZATION);
        put4(
                address + LAW_1,
                (float) LPW2AnimationData.HARMONIC_SECOND,
                (float) LPW2AnimationData.HARMONIC_FOURTH,
                (float) LPW2AnimationData.ROCK_FREQUENCY,
                (float) LPW2AnimationData.HALF);
    }

    public void frame(LPW2Clock clock) {
        long address = slabPtr();
        put4(
                address + PHASE_0,
                (float) clock.axialPhase,
                (float) clock.rockingPhase,
                (float) clock.pistonPhase,
                (float) clock.rotorPhase);
        MemoryUtil.memPutFloat(address + PHASE_1, (float) clock.serverPhase);
        MemoryUtil.memPutFloat(address + PHASE_1 + Float.BYTES, (float) clock.turbine);
        MemoryUtil.memPutFloat(address + PHASE_1 + 2 * Float.BYTES, clock.scroll);
        setChanged();
    }

    private static int offset(String name) {
        return LAYOUT.asMap().get(name).byteOffset();
    }

    private static void putColumns(long address, double[] data, int start) {
        put4(
                address,
                (float) data[start],
                (float) data[start + 1],
                (float) data[start + 2],
                (float) data[start + 3]);
    }

    private static void put4(long address, float x, float y, float z, float w) {
        MemoryUtil.memPutFloat(address, x);
        MemoryUtil.memPutFloat(address + Float.BYTES, y);
        MemoryUtil.memPutFloat(address + 2 * Float.BYTES, z);
        MemoryUtil.memPutFloat(address + 3 * Float.BYTES, w);
    }
}
