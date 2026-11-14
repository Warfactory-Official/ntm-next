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

import com.hbm.blockentity.door.DoorEasing;
import com.hbm.blockentity.door.DoorType;
import com.hbm.blockentity.door.DoorTypes;
import java.util.Arrays;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class DoorAnimationData {
    public static final int FIRE = 0,
            SECURE = 1,
            CARGO = 2,
            WATER = 3,
            VAULT = 4,
            QE_SLIDING = 5,
            SLIDING_SEAL = 6,
            QE_CONTAINMENT = 7,
            ROUND_AIRLOCK = 8,
            LARGE_VEHICLE = 9,
            SLIDE = 10,
            SILO = 11;
    public static final int CLIP = 7;

    private DoorAnimationData() {}

    public static int kind(DoorType door) {
        if (door == DoorTypes.FIRE_DOOR) return FIRE;
        if (door == DoorTypes.SECURE_ACCESS_DOOR) return SECURE;
        if (door == DoorTypes.CARGO_DOOR) return CARGO;
        if (door == DoorTypes.WATER_DOOR) return WATER;
        if (door == DoorTypes.VAULT_DOOR) return VAULT;
        if (door == DoorTypes.QE_SLIDING_DOOR) return QE_SLIDING;
        if (door == DoorTypes.SLIDING_SEAL_DOOR) return SLIDING_SEAL;
        if (door == DoorTypes.QE_CONTAINMENT) return QE_CONTAINMENT;
        if (door == DoorTypes.ROUND_AIRLOCK_DOOR) return ROUND_AIRLOCK;
        if (door == DoorTypes.LARGE_VEHICLE_DOOR) return LARGE_VEHICLE;
        if (door == DoorTypes.SLIDING_BLAST_DOOR) return SLIDE;
        if (door == DoorTypes.SILO_HATCH || door == DoorTypes.SILO_HATCH_LARGE) return SILO;
        throw new IllegalArgumentException("no shader pose for door " + door);
    }

    public static void pose(
            DoorType door,
            int kind,
            int index,
            double first,
            double second,
            Matrix4f pose,
            float[] clip) {
        pose.identity();
        Arrays.fill(clip, 0F);
        switch (kind) {
            case FIRE ->
                    pose.rotateY(Mth.HALF_PI)
                            .translate(-.5F, (float) Mth.clamp(first * 2.75, 0, 2.75), 0);
            case SECURE -> pose.translate(0, 1 + (float) Mth.clamp(first * 3.5, 0, 3.5), 0);
            case CARGO ->
                    pose.translation(
                            0,
                            index == 0
                                    ? (float) Mth.clamp(first, 0, 1)
                                    : (float) Mth.clamp(second, 0, 1) * 2,
                            0);
            case WATER -> {
                waterLeaf(pose, first);
                if (index == 1) pose.translate((float) (-.4 * second), 0, 0);
                else if (index > 1) {
                    float y = index == 2 ? 2.28125F : .71875F;
                    pose.translate(.40625F, y, 0)
                            .rotateZ((float) (second * 360) * Mth.DEG_TO_RAD)
                            .translate(-.40625F, -y, 0);
                }
            }
            case VAULT ->
                    pose.translate((float) -first, 0, 0)
                            .translate(0, 0, (float) (second * 5))
                            .translate(0, 2.5F, 0)
                            .rotateX(
                                    (float) (360D * (second * 5) / (4.25D * Math.PI))
                                            * Mth.DEG_TO_RAD)
                            .translate(0, -2.5F, 0);
            case QE_SLIDING -> {
                float travel = (float) Mth.clamp(first * .95, 0, .95);
                pose.translation(.53125F, .001F, index == 0 ? .5F + travel : .5F - travel);
            }
            case SLIDING_SEAL -> {
                pose.translation(.5F, 0, 0);
                clip(
                        clip,
                        0,
                        0,
                        (float) (DoorEasing.hermite((float) Mth.clamp(first, 0, 1)) * .9),
                        0,
                        0,
                        1,
                        .5001F);
            }
            case QE_CONTAINMENT -> {
                pose.translation(.25F, 0, 0);
                clip(clip, 0, (float) Mth.clamp(first * 2.25, 0, 2.25), 0, 0, 1, 0, 3);
            }
            case ROUND_AIRLOCK -> {
                float travel = (float) Mth.clamp(first * 1.5, 0, 1.5);
                pose.translation(0, 0, .5F);
                clip(clip, 0, 0, index == 0 ? travel : -travel, 0, 0, 1, 1.999F);
            }
            case LARGE_VEHICLE -> {
                float travel = (float) Mth.clamp(first * 3, 0, 3);
                pose.rotateY(Mth.HALF_PI);
                clip(clip, index == 0 ? -travel : travel, 0, 0, 1, 0, 0, 3.4375F);
            }
            case SLIDE -> {
                float travel = (float) Mth.clamp(first * 2.125, 0, 2.125);
                if (index >= 2) travel = -travel;
                if ((index & 1) == 0) clip(clip, 0, 0, travel, 0, 0, 1, 2.5F);
                else {
                    float swing = (float) Math.toRadians(90 + second * 90),
                            side = travel < 0 ? -1 : 1;
                    pose.translation(0, 0, travel).translate(0, 1.8125F, 0).rotateX(swing);
                    clip(
                            clip,
                            0,
                            -1.8125F,
                            0,
                            0,
                            side * (float) Math.sin(swing),
                            side * (float) Math.cos(swing),
                            2.5F - Math.abs(travel));
                }
            }
            case SILO -> {
                if (index != door.getRenderDefinition().getHingeIndex()) return;
                var hinge = door.getHinge();
                float swing = hinge.swingAt((float) first);
                pose.translation(hinge.originX(), hinge.originY(), hinge.originZ());
                if (swing != 0) pose.rotateX(swing * Mth.DEG_TO_RAD);
                pose.translate(
                        0F - hinge.originX(),
                        hinge.liftAt((float) first) - hinge.originY(),
                        0F - hinge.originZ());
            }
            default -> throw new IllegalArgumentException("door kind " + kind);
        }
    }

    private static void waterLeaf(Matrix4f pose, double first) {
        pose.translate(.375F, 0, 0)
                .rotateY(Mth.HALF_PI)
                .translate(-1.1875F, 0, 0)
                .rotateY((float) (-first * 120) * Mth.DEG_TO_RAD)
                .translate(1.1875F, 0, 0);
    }

    private static void clip(
            float[] clip,
            float sx,
            float sy,
            float sz,
            float nx,
            float ny,
            float nz,
            float threshold) {
        clip[0] = sx;
        clip[1] = sy;
        clip[2] = sz;
        clip[3] = nx;
        clip[4] = ny;
        clip[5] = nz;
        clip[6] = threshold;
    }
}
