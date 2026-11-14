/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import com.hbm.client.render.flywheel.ChainInstance;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.model.Model;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector4f;

public final class ArcFurnaceAnimationData {
    public static final String HULL_GROUP = "Furnace";
    public static final int PART_COUNT = 18;
    public static final int HOT_CONTENTS = 0;
    public static final int COLD_CONTENTS = 1;
    public static final int LID = 2;
    public static final int RING_START = 3;
    public static final int FRESH_START = 6;
    public static final int USED_START = 9;
    public static final int SHORT_START = 12;
    public static final int CABLE_START = 15;
    public static final int ELECTRODE_COUNT = 3;

    public static final int SCALAR_COUNT = 3;
    public static final int LIFT_SAMPLE = 0;
    public static final int JITTER_SAMPLE = 1;
    public static final int CABLE_DEGREES_SAMPLE = 2;

    public static final int CHANNEL_COUNT = 6;
    public static final int LIFT_CHANNEL = 0;
    public static final int JITTER_CHANNEL = 1;
    public static final int CABLE_RADIANS_CHANNEL = 2;
    public static final int CONTENTS_CHANNEL = 3;

    public static final float ROOT_X = 0.5F;
    public static final float ROOT_Y = 0F;
    public static final float ROOT_Z = 0.5F;
    public static final int NORTH_YAW = 90;
    public static final float LID_TRAVEL = 2F;
    public static final double JITTER_AMPLITUDE = 0.005D;
    public static final double CABLE_DEGREES = 30D;
    public static final float CABLE_PIVOT_Y = 5.5F;
    public static final double CONTENTS_LOW_Y = -1.75D;
    public static final double CONTENTS_TRAVEL = 1.75D;
    public static final float POUR_LIFETIME = 20F;
    public static final double OUTLET_REACH = 2.875D;
    public static final float OUTLET_Y = 1F;
    public static final float POUR_BASE = 0.625F;
    public static final float POUR_OFFSET = 0.625F;

    private static final String[] GROUPS = {
        "ContentsHot", "ContentsCold", "Lid",
        "Ring1", "Ring2", "Ring3",
        "Electrode1", "Electrode2", "Electrode3",
        "Electrode1Hot", "Electrode2Hot", "Electrode3Hot",
        "Electrode1Short", "Electrode2Short", "Electrode3Short",
        "Cable1", "Cable2", "Cable3"
    };
    private static final float[] CABLE_Z = {0.5F, 0F, -0.5F};
    private static final int[] CHANNEL_MASKS = {
        8, 0, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 7, 7, 7
    };
    private static final float[] LOW_CHANNELS = {
        0F,
        (float) -JITTER_AMPLITUDE,
        (float) (-CABLE_DEGREES * Mth.DEG_TO_RAD),
        (float) CONTENTS_LOW_Y,
        0F,
        0F
    };
    private static final float[] HIGH_CHANNELS = {
        1F, (float) JITTER_AMPLITUDE, (float) (CABLE_DEGREES * Mth.DEG_TO_RAD), 0F, 0F, 0F
    };
    private static final ChainInstance.Chain[] CHAINS = buildChains();

    private ArcFurnaceAnimationData() {}

    public static String group(int part) {
        return GROUPS[part];
    }

    public static float cablePivotZ(int electrode) {
        return CABLE_Z[electrode];
    }

    public static int channelMask(int part) {
        return CHANNEL_MASKS[part];
    }

    public static int yaw(Direction facing) {
        return Facing.yaw(facing, NORTH_YAW);
    }

    public static void evaluate(
            long gameTick,
            float partialTick,
            float previousLift,
            float currentLift,
            boolean progressing,
            double[] samples) {
        float lift = previousLift + (currentLift - previousLift) * partialTick;
        samples[LIFT_SAMPLE] = lift;
        if (progressing) {
            float phase = phase(gameTick, partialTick);
            samples[JITTER_SAMPLE] = Math.sin(phase) * JITTER_AMPLITUDE;
            samples[CABLE_DEGREES_SAMPLE] = Math.sin(phase / 2D) * CABLE_DEGREES;
        } else {
            samples[JITTER_SAMPLE] = 0D;
            samples[CABLE_DEGREES_SAMPLE] = 0D;
        }
    }

    public static float phase(long gameTick, float partialTick) {
        return gameTick + partialTick;
    }

    public static double contentsY(int amount, int capacity) {
        return CONTENTS_LOW_Y + amount * CONTENTS_TRAVEL / capacity;
    }

    public static void writeChannels(double[] samples, int amount, int capacity, float[] channels) {
        channels[LIFT_CHANNEL] = (float) samples[LIFT_SAMPLE];
        channels[JITTER_CHANNEL] = (float) samples[JITTER_SAMPLE];
        channels[CABLE_RADIANS_CHANNEL] = (float) (samples[CABLE_DEGREES_SAMPLE] * Mth.DEG_TO_RAD);
        channels[CONTENTS_CHANNEL] = (float) contentsY(amount, capacity);
        channels[4] = 0F;
        channels[5] = 0F;
    }

    public static Matrix4f rootPose(Matrix4f destination, Direction facing) {
        return destination
                .translation(ROOT_X, ROOT_Y, ROOT_Z)
                .rotateY(yaw(facing) * Mth.DEG_TO_RAD);
    }

    public static void nativeRoot(PoseStack poses, Direction facing, Quaternionf rotation) {
        poses.translate(ROOT_X, ROOT_Y, ROOT_Z);
        poses.rotate((Quaternionfc) rotation.rotationY(yaw(facing) * Mth.DEG_TO_RAD));
    }

    public static void nativeContents(PoseStack poses, int amount, int capacity) {
        poses.translate(0D, contentsY(amount, capacity), 0D);
    }

    public static void nativeLid(PoseStack poses, double[] samples) {
        poses.translate(0D, LID_TRAVEL * samples[LIFT_SAMPLE], samples[JITTER_SAMPLE]);
    }

    public static void nativeCable(
            PoseStack poses, int electrode, double[] samples, Quaternionf rotation) {
        float offset = CABLE_Z[electrode];
        poses.translate(0D, CABLE_PIVOT_Y, offset);
        float degrees = (float) samples[CABLE_DEGREES_SAMPLE];
        poses.rotate((Quaternionfc) rotation.rotationX(degrees * Mth.DEG_TO_RAD));
        poses.translate(0D, -CABLE_PIVOT_Y, -offset);
    }

    public static void nativeOutlet(PoseStack poses, Direction facing) {
        poses.translate(
                ROOT_X + facing.getStepX() * OUTLET_REACH,
                OUTLET_Y,
                ROOT_Z + facing.getStepZ() * OUTLET_REACH);
    }

    public static ChainInstance.Chain chain(int part) {
        return CHAINS[part];
    }

    public static ChainInstance.Chain[] chains() {
        return CHAINS.clone();
    }

    public static Vector4f sweep(int part, Model model) {
        return CHAINS[part].sweep(model.boundingSphere(), LOW_CHANNELS, HIGH_CHANNELS);
    }

    public static AABB nativeBounds(BlockPos core) {
        return new AABB(
                core.getX() - 3D,
                core.getY() - 6D,
                core.getZ() - 3D,
                core.getX() + 4D,
                core.getY() + 8.5D,
                core.getZ() + 4D);
    }

    public static AABB visualMinimumBounds(BlockPos core) {
        return new AABB(
                core.getX() - 4D,
                core.getY() - 6D,
                core.getZ() - 4D,
                core.getX() + 5D,
                core.getY() + 7D,
                core.getZ() + 5D);
    }

    private static ChainInstance.Chain[] buildChains() {
        var chains = new ChainInstance.Chain[PART_COUNT];
        var contents = new ChainInstance.Chain().slideY(CONTENTS_CHANNEL, 1F).end();
        var fixed = new ChainInstance.Chain().end();
        var assembly = assemblyChain().end();
        chains[HOT_CONTENTS] = contents;
        chains[COLD_CONTENTS] = fixed;
        for (int part = LID; part < CABLE_START; part++) chains[part] = assembly;
        for (int electrode = 0; electrode < ELECTRODE_COUNT; electrode++) {
            float offset = CABLE_Z[electrode];
            chains[CABLE_START + electrode] =
                    assemblyChain()
                            .translate(0F, CABLE_PIVOT_Y, offset)
                            .rotateX(CABLE_RADIANS_CHANNEL, 1F)
                            .translate(0F, -CABLE_PIVOT_Y, -offset)
                            .end();
        }
        return chains;
    }

    private static ChainInstance.Chain assemblyChain() {
        return new ChainInstance.Chain()
                .slideY(LIFT_CHANNEL, LID_TRAVEL)
                .slideZ(JITTER_CHANNEL, 1F);
    }
}
