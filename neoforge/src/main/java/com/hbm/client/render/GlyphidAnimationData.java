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

import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class GlyphidAnimationData {
    public static final int BANK_SIZE = 22;
    public static final int PART_COUNT = 27;
    public static final int PAYLOAD_PART = 27;
    public static final int TOTAL_PART_COUNT = 28;
    public static final int ANGLES_PER_PART = 3;

    private static final float SHOULDER_Y = radians(10D);
    private static final float PAYLOAD_X = radians(90D);
    private static final String[] GROUPS = {
        "Body",
        "ArmorFront",
        "ArmorLeft",
        "ArmorRight",
        "ArmLeftUpper",
        "ArmLeftMid",
        "ArmLeftLower",
        "ArmLeftArmor",
        "ArmRightUpper",
        "ArmRightMid",
        "ArmRightLower",
        "ArmRightArmor",
        "JawTop",
        "JawLeft",
        "JawRight",
        "LegLeftUpper",
        "LegLeftLower",
        "LegRightUpper",
        "LegRightLower",
        "LegLeftUpper",
        "LegLeftLower",
        "LegRightUpper",
        "LegRightLower",
        "LegLeftUpper",
        "LegLeftLower",
        "LegRightUpper",
        "LegRightLower",
        "MiniNuke"
    };
    private static final int[] ARMOR_BITS = {
        -1, 0, 1, 2, -1, -1, -1, 3, -1, -1, -1, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1
    };
    private static final int[] ANGLE_INDICES = {
        -1, -1, -1,
        -1, -1, -1,
        -1, -1, -1,
        -1, -1, -1,
        0, -1, -1,
        0, 1, -1,
        0, 1, 2,
        0, 1, 2,
        3, -1, -1,
        3, 4, -1,
        3, 4, 5,
        3, 4, 5,
        6, 7, -1,
        6, 7, -1,
        6, 7, -1,
        8, 14, -1,
        8, 14, 18,
        11, 16, -1,
        11, 16, 20,
        9, 15, -1,
        9, 15, 19,
        12, 17, -1,
        12, 17, 21,
        10, 14, -1,
        10, 14, 18,
        13, 16, -1,
        13, 16, 20,
        -1, -1, -1
    };

    private GlyphidAnimationData() {}

    public static String group(int part) {
        return GROUPS[part];
    }

    public static int armorBit(int part) {
        return ARMOR_BITS[part];
    }

    public static int angleIndex(int part, int component) {
        return ANGLE_INDICES[part * ANGLES_PER_PART + component];
    }

    public static void evaluate(float walkingPosition, float swingProgress, float[] destination) {
        evaluateWalking(walkingPosition, destination);
        evaluateSwing(swingProgress, destination);
    }

    public static void evaluateWalking(float walkingPosition, float[] destination) {
        double walking = walkingPosition;
        double phase = walking % (Math.PI * 2D);
        double phaseA = Math.sin(phase);
        double phaseB = Math.sin(phase - Math.PI * .5D);
        double phaseC = Math.sin(phase - Math.PI);
        double phaseD = Math.sin(phase - Math.PI * .75D);

        destination[0] = radians(35D + phaseB * 20D);
        destination[1] = radians(-75D - phaseB * 20D + phaseA * 20D);
        destination[2] = radians(90D - phaseA * 45D);
        destination[3] = radians(35D + phaseC * 20D);
        destination[4] = radians(-75D - phaseC * 20D + phaseD * 20D);
        destination[5] = radians(90D - phaseD * 45D);
        for (int pair = 0; pair < 3; pair++) {
            double sign = pair == 1 ? -1D : 1D;
            double horizontal = phaseA * sign;
            destination[8 + pair] = radians(pair * 30D - 15D + horizontal * 7.5D);
            destination[11 + pair] = radians(pair * 30D - 45D + horizontal * 7.5D);
        }

        double outer = phaseB * 1D;
        double middle = phaseB * -1D;
        destination[14] = radians(15D + outer * 15D);
        destination[15] = radians(15D + middle * 15D);
        destination[16] = radians(-15D + outer * 15D);
        destination[17] = radians(-15D + middle * 15D);
        destination[18] = radians(-60D - outer * 15D);
        destination[19] = radians(-60D - middle * 15D);
        destination[20] = radians(60D - outer * 15D);
        destination[21] = radians(60D - middle * 15D);
    }

    public static void evaluateSwing(float swingProgress, float[] destination) {
        double swing = swingProgress;
        double jaw =
                Math.min(1D, Math.max(0D, Math.sin(swing * Math.PI * 2D - Math.PI * .5D))) * 20D;
        double head = Math.sin(swing * Math.PI) * 30D;
        destination[6] = radians(head);
        destination[7] = radians(jaw);
    }

    public static Matrix4f appendPose(
            Matrix4f destination, int occurrence, float first, float second, float third) {
        return switch (occurrence) {
            case 0, 1, 2, 3 -> destination;
            case 4 -> arm(destination, 1F, 1, first, second, third);
            case 5 -> arm(destination, 1F, 2, first, second, third);
            case 6, 7 -> arm(destination, 1F, 3, first, second, third);
            case 8 -> arm(destination, -1F, 1, first, second, third);
            case 9 -> arm(destination, -1F, 2, first, second, third);
            case 10, 11 -> arm(destination, -1F, 3, first, second, third);
            case 12 -> jaw(destination, 0F, first, second);
            case 13 -> jaw(destination, 1F, first, second);
            case 14 -> jaw(destination, -1F, first, second);
            case 15, 17, 19, 21, 23, 25 -> upperLeg(destination, first, second);
            case 16, 20, 24 -> lowerLeg(destination, .5625F, first, second, third);
            case 18, 22, 26 -> lowerLeg(destination, -.5625F, first, second, third);
            case PAYLOAD_PART -> {
                destination.translate(0F, 1F, 0F);
                rotateX(destination, PAYLOAD_X);
                yield destination;
            }
            default -> throw new IndexOutOfBoundsException(occurrence);
        };
    }

    public static Matrix4f appendNuclearScale(
            Matrix4f destination, float deathAge, double entityScale) {
        float progress = deathAge / 95F;
        float wobble = 1F + Mth.sin(progress * 100F) * progress * .01F;
        if (progress < 0F) progress = 0F;
        if (progress > 1F) progress = 1F;
        progress *= progress;
        progress *= progress;
        float horizontal = (1F + progress * .4F) * wobble;
        float vertical = (1F + progress * .1F) / wobble;
        float scale = (float) entityScale;
        destination.scale(horizontal, vertical, horizontal);
        destination.scale(scale);
        return destination;
    }

    public static int nuclearFlashAlpha(float deathAge) {
        float progress = deathAge / 20F;
        int alpha = (int) (progress * .2F * 255F);
        if ((int) (progress * 10F) % 4 < 2) return 0;
        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;
        return alpha;
    }

    private static float radians(double degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static Matrix4f arm(
            Matrix4f destination, float side, int depth, float first, float second, float third) {
        destination.translate(side * .25F, .625F, .0625F);
        rotateY(destination, side * SHOULDER_Y);
        rotateX(destination, first);
        destination.translate(-side * .25F, -.625F, -.0625F);
        if (depth >= 2) {
            destination.translate(side * .25F, .625F, .4375F);
            rotateX(destination, second);
            destination.translate(-side * .25F, -.625F, -.4375F);
        }
        if (depth >= 3) {
            destination.translate(side * .25F, .625F, .9375F);
            rotateX(destination, third);
            destination.translate(-side * .25F, -.625F, -.9375F);
        }
        return destination;
    }

    private static Matrix4f jaw(Matrix4f destination, float side, float head, float opening) {
        destination.translate(0F, .5F, .25F);
        rotateZ(destination, head);
        destination.translate(0F, -.5F, -.25F);
        destination.translate(0F, .5F, .25F);
        if (side == 0F) {
            rotateX(destination, -opening);
        } else {
            rotateY(destination, side * opening);
            rotateX(destination, opening);
        }
        destination.translate(0F, -.5F, -.25F);
        return destination;
    }

    private static Matrix4f upperLeg(Matrix4f destination, float yaw, float roll) {
        destination.translate(0F, .25F, 0F);
        rotateY(destination, yaw);
        rotateZ(destination, roll);
        destination.translate(0F, -.25F, 0F);
        return destination;
    }

    private static Matrix4f lowerLeg(
            Matrix4f destination, float pivotX, float first, float second, float third) {
        upperLeg(destination, first, second);
        destination.translate(pivotX, .25F, 0F);
        rotateZ(destination, third);
        destination.translate(-pivotX, -.25F, 0F);
        return destination;
    }

    private static void rotateColumns(
            Matrix4f destination, int firstColumn, int secondColumn, float angle) {
        float sine = (float) Math.sin(angle);
        float cosine = (float) Math.cos(angle);
        int properties = destination.properties() & Matrix4fc.PROPERTY_AFFINE;
        for (int row = 0; row < 4; row++) {
            float first = destination.get(firstColumn, row);
            float second = destination.get(secondColumn, row);
            destination.set(firstColumn, row, first * cosine + second * sine);
            destination.set(secondColumn, row, second * cosine - first * sine);
        }
        destination.assume(properties);
    }

    private static void rotateX(Matrix4f destination, float angle) {
        rotateColumns(destination, 1, 2, angle);
    }

    private static void rotateY(Matrix4f destination, float angle) {
        rotateColumns(destination, 0, 2, -angle);
    }

    private static void rotateZ(Matrix4f destination, float angle) {
        rotateColumns(destination, 0, 1, angle);
    }
}
