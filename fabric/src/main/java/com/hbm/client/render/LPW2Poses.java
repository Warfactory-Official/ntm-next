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

package com.hbm.client.render;

import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class LPW2Poses {
    public static final int CENTER = 0;
    public static final int ROTOR = 1;
    public static final int TURBINE_FRONT = 2;
    public static final int TURBINE_BACK = 3;
    public static final int PISTON = 4;
    public static final int ENGINE = 5;
    public static final int SHROUD_H = 6;
    public static final int SHROUD_V = 7;
    public static final int FLAPS = 8;
    public static final int SUSPENSION_LEFT = 16;
    public static final int SUSPENSION_RIGHT = 17;
    public static final int SUSPENSION_TOP = 18;
    public static final int SUSPENSION_BOTTOM = 19;
    public static final int WIRE_LEFT = 20;
    public static final int WIRE_RIGHT = 21;
    public static final int COVER = 22;
    public static final int SUSPENSION_COVER_FRONT = 23;
    public static final int SUSPENSION_COVER_BACK = 24;
    public static final int SUSPENSION_BACK_OUTER = 25;
    public static final int SUSPENSION_BACK_CENTER = 26;
    public static final int SERVERS = 27;
    public static final int MONITOR = 31;
    public static final int SCREEN = 32;
    public static final String[] GROUPS = LPW2AnimationData.GROUPS;

    private final Matrix4f[] poses = new Matrix4f[GROUPS.length];
    private final Matrix4f base = new Matrix4f();
    private final LPW2Clock clock = new LPW2Clock();
    private final double[] motion = new double[10];

    public LPW2Poses() {
        for (int slot = 0; slot < poses.length; slot++) {
            poses[slot] = new Matrix4f();
        }
    }

    public static float getAnimationTime(Level level, float partialTick) {
        return LPW2Clock.getWorldTime(level, partialTick);
    }

    public void update(float yaw, double mechanicalTime, double worldTime) {
        clock.prepare(mechanicalTime, worldTime);
        motion[LPW2AnimationData.AXIAL] =
                (LPW2Clock.harmonic(clock.axialPhase) + LPW2AnimationData.AXIAL_OFFSET)
                        * LPW2AnimationData.HALF;
        motion[LPW2AnimationData.HORIZONTAL] = LPW2Clock.rocking(clock.rockingPhase, Math.PI);
        motion[LPW2AnimationData.VERTICAL] = LPW2Clock.rocking(clock.rockingPhase, 0);
        motion[LPW2AnimationData.COVER] =
                LPW2Clock.harmonic(clock.rockingPhase) * LPW2AnimationData.HALF;
        motion[LPW2AnimationData.PISTON_WAVE] = LPW2Clock.softPeak(clock.pistonPhase);
        motion[LPW2AnimationData.ROTOR] =
                (LPW2Clock.softPeak(clock.rotorPhase)
                                + clock.rotorPhase * LPW2AnimationData.HALF
                                - 1)
                        / LPW2AnimationData.ROTOR_NORMALIZATION;
        motion[LPW2AnimationData.TURBINE] = clock.turbine;
        motion[LPW2AnimationData.SERVER_B] = LPW2Clock.rocking(clock.serverPhase, Math.PI);
        motion[LPW2AnimationData.SERVER_D] = LPW2Clock.rocking(clock.serverPhase, 0);

        base.identity().translate(0.5f, 0, 0.5f).rotateY(radians(yaw));
        for (int slot = 0; slot < poses.length; slot++) {
            Matrix4f pose = poses[slot];
            pose.set(base);
            apply(pose, slot);
        }
    }

    public Matrix4fc getPartPose(int slot) {
        return poses[slot];
    }

    public float getScroll() {
        return clock.scroll;
    }

    private void apply(Matrix4f pose, int slot) {
        int row = slot * LPW2AnimationData.COLUMNS;
        double[] data = LPW2AnimationData.PARTS;
        int kind = (int) data[row + LPW2AnimationData.KIND];
        double first = motion[(int) data[row + LPW2AnimationData.SIGNAL_0]];
        double second = motion[(int) data[row + LPW2AnimationData.SIGNAL_1]];
        int p = row + LPW2AnimationData.DATA_0;
        int q = row + LPW2AnimationData.DATA_1;
        switch (kind) {
            case LPW2AnimationData.TRANSLATE ->
                    pose.translate(
                            (float) (data[p] * first),
                            (float) (data[p + 1] * first),
                            (float) (data[p + 2] * second));
            case LPW2AnimationData.CENTER_SPIN -> {
                pose.translate(0, 0, (float) (data[p] * motion[LPW2AnimationData.AXIAL]));
                pivot(pose, 2, data[p + 1] * first, 0, data[p + 2], data[p + 3]);
            }
            case LPW2AnimationData.PISTON -> {
                pose.translate(0, 0, (float) (data[p] * motion[LPW2AnimationData.AXIAL]));
                pose.translate(
                        0,
                        0,
                        (float)
                                (data[p + 1] * motion[LPW2AnimationData.PISTON_WAVE]
                                        + data[p + 2]));
            }
            case LPW2AnimationData.ENGINE -> {
                pose.translate(0, 0, (float) (data[p] * motion[LPW2AnimationData.AXIAL]));
                pose.translate(0, (float) data[p + 1], (float) data[p + 2]);
                pose.rotateY(radians(data[p + 3] * motion[LPW2AnimationData.VERTICAL]));
                pose.rotateX(radians(data[q] * motion[LPW2AnimationData.HORIZONTAL]));
                pose.translate(0, (float) -data[p + 1], (float) -data[p + 2]);
            }
            case LPW2AnimationData.FLAP -> {
                pose.translate((float) (data[p] * first), (float) (data[p + 1] * first), 0);
                pivot(pose, 2, data[p + 2], 0, data[q + 1], 0);
                pivot(pose, 0, data[p + 3] + data[q] * second, 0, data[q + 2], data[q + 3]);
            }
            case LPW2AnimationData.STRETCH -> {
                int axis = (int) data[p + 3];
                float scale = (float) ((data[p + 1] + data[p + 2] * first) / data[p + 1]);
                float anchor = (float) data[p];
                switch (axis) {
                    case 0 ->
                            pose.translate(anchor, 0, 0)
                                    .scale(scale, 1, 1)
                                    .translate(-anchor, 0, 0);
                    case 1 ->
                            pose.translate(0, anchor, 0)
                                    .scale(1, scale, 1)
                                    .translate(0, -anchor, 0);
                    case 2 ->
                            pose.translate(0, 0, anchor)
                                    .scale(1, 1, scale)
                                    .translate(0, 0, -anchor);
                    default -> throw new IllegalStateException("Unknown stretch axis: " + axis);
                }
            }
            case LPW2AnimationData.WIRE ->
                    pivot(pose, 1, data[p] * first, data[p + 1], data[p + 2], data[p + 3]);
            default -> throw new IllegalStateException("Unknown motion kind: " + kind);
        }
    }

    private static void pivot(Matrix4f pose, int axis, double angle, double x, double y, double z) {
        pose.translate((float) x, (float) y, (float) z);
        float radians = radians(angle);
        switch (axis) {
            case 0 -> pose.rotateX(radians);
            case 1 -> pose.rotateY(radians);
            case 2 -> pose.rotateZ(radians);
            default -> throw new IllegalStateException("Unknown rotation axis: " + axis);
        }
        pose.translate((float) -x, (float) -y, (float) -z);
    }

    private static float radians(double degrees) {
        return (float) Math.toRadians(degrees);
    }
}
