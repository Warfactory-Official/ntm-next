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

package com.hbm.particle.helper;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.CasingLayout;
import com.hbm.packet.layout.GunCasingLayout;
import com.hbm.packet.threading.ThreadedPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CasingCreator {

    private static final double RANGE = 50D;
    private static final float CROUCH_DROP = 0.075F;
    private static final double FLIGHT_SAG = 0.04D;

    public static void composeEffect(
            Level level,
            LivingEntity player,
            double frontOffset,
            double heightOffset,
            double sideOffset,
            double frontMotion,
            double heightMotion,
            double sideMotion,
            double motionVariance,
            String casing) {
        composeEffect(
                level,
                player,
                frontOffset,
                heightOffset,
                sideOffset,
                frontMotion,
                heightMotion,
                sideMotion,
                motionVariance,
                5F,
                10F,
                casing,
                false,
                0,
                0,
                0);
    }

    public static void composeEffect(
            Level level,
            LivingEntity player,
            double frontOffset,
            double heightOffset,
            double sideOffset,
            double frontMotion,
            double heightMotion,
            double sideMotion,
            double motionVariance,
            float multPitch,
            float multYaw,
            String casing) {
        composeEffect(
                level,
                player,
                frontOffset,
                heightOffset,
                sideOffset,
                frontMotion,
                heightMotion,
                sideMotion,
                motionVariance,
                multPitch,
                multYaw,
                casing,
                false,
                0,
                0,
                0);
    }

    public static void composeEffect(
            Level level,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            double frontMotion,
            double heightMotion,
            double sideMotion,
            double motionVariance,
            float mPitch,
            float mYaw,
            String casing,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife) {
        if (!(level instanceof ServerLevel server)) return;
        var kick =
                jitter(
                        look(sideMotion, heightMotion, frontMotion, pitch, yaw),
                        level.getRandom(),
                        motionVariance);
        broadcast(
                server,
                x,
                y,
                z,
                CasingLayout.encode(
                        x, y, z, kick.x, kick.y, kick.z, yaw, pitch, mPitch, mYaw, casing, smoking,
                        smokeLife, smokeLift, nodeLife));
    }

    public static void composeEffect(
            Level level,
            LivingEntity player,
            double frontOffset,
            double heightOffset,
            double sideOffset,
            double frontMotion,
            double heightMotion,
            double sideMotion,
            double motionVariance,
            float mPitch,
            float mYaw,
            String casing,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife) {
        composeEffect(
                level,
                player,
                frontOffset,
                heightOffset,
                sideOffset,
                frontMotion,
                heightMotion,
                sideMotion,
                motionVariance,
                mPitch,
                mYaw,
                casing,
                smoking,
                smokeLife,
                smokeLift,
                nodeLife,
                frontOffset);
    }

    public static void composeEffect(
            Level level,
            LivingEntity player,
            double frontOffset,
            double heightOffset,
            double sideOffset,
            double frontMotion,
            double heightMotion,
            double sideMotion,
            double motionVariance,
            float mPitch,
            float mYaw,
            String casing,
            boolean smoking,
            int smokeLife,
            double smokeLift,
            int nodeLife,
            double legacyFrontOffset) {
        if (!(level instanceof ServerLevel server)) return;
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        double height = player.isShiftKeyDown() ? heightOffset - CROUCH_DROP : heightOffset;
        var eyes = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        var at = eyes.add(look(sideOffset, height, frontOffset, pitch, yaw));
        var kick =
                jitter(
                        player.getDeltaMovement()
                                .add(look(sideMotion, heightMotion, frontMotion, pitch, yaw)),
                        player.getRandom(),
                        motionVariance);
        double kickY =
                player instanceof Player p && p.getAbilities().flying
                        ? kick.y - FLIGHT_SAG
                        : kick.y;

        if (legacyFrontOffset == frontOffset) {
            broadcast(
                    server,
                    at.x,
                    at.y,
                    at.z,
                    CasingLayout.encode(
                            at.x, at.y, at.z, kick.x, kickY, kick.z, yaw, pitch, mPitch, mYaw,
                            casing, smoking, smokeLife, smokeLift, nodeLife));
            return;
        }
        var legacyAt = eyes.add(look(sideOffset, height, legacyFrontOffset, pitch, yaw));
        for (var legacy : new boolean[] {false, true}) {
            var from = legacy ? legacyAt : at;
            broadcast(
                    server,
                    from.x,
                    from.y,
                    from.z,
                    GunCasingLayout.encode(
                            from.x, from.y, from.z, kick.x, kickY, kick.z, yaw, pitch, mPitch, mYaw,
                            casing, smoking, smokeLife, smokeLift, nodeLife, legacy));
        }
    }

    private static Vec3 look(double side, double up, double front, float pitch, float yaw) {
        return new Vec3(side, up, front)
                .xRot(-pitch / 180F * (float) Math.PI)
                .yRot(-yaw / 180F * (float) Math.PI);
    }

    private static Vec3 jitter(Vec3 motion, RandomSource random, double variance) {
        double x = motion.x + random.nextGaussian() * variance;
        double y = motion.y + random.nextGaussian() * variance;
        return new Vec3(x, y, motion.z + random.nextGaussian() * variance);
    }

    private static void broadcast(
            ServerLevel server, double x, double y, double z, ThreadedPayload payload) {
        PacketWire.sendToAllAround(payload, new TargetPoint(server, x, y, z, RANGE));
    }
}
