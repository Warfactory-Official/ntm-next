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
import com.hbm.packet.layout.AshesLayout;
import com.hbm.packet.layout.BlackPowderLayout;
import com.hbm.packet.layout.DebugLetterLayout;
import com.hbm.packet.layout.DebugTextLayout;
import com.hbm.packet.layout.DroneLineLayout;
import com.hbm.packet.layout.GibletsLayout;
import com.hbm.packet.layout.SkeletonGibLayout;
import com.hbm.packet.layout.SkeletonizeLayout;
import com.hbm.packet.layout.SparkBurstLayout;
import com.hbm.packet.layout.SweatLayout;
import com.hbm.packet.layout.VomitLayout;
import com.hbm.packet.threading.EncodedPayload;
import com.hbm.particle.EffectKinds.GibletType;
import com.hbm.particle.EffectKinds.VomitMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class ParticleCreators {

    private ParticleCreators() {}

    public static void ashes(Level level, Entity toPulverize, int count, float scale) {
        send(
                level,
                toPulverize.getX(),
                toPulverize.getY(),
                toPulverize.getZ(),
                100,
                AshesLayout.encode(toPulverize.getId(), count, scale));
    }

    public static void skeleton(Level level, Entity toSkeletonize, float brightness) {
        send(
                level,
                toSkeletonize.getX(),
                toSkeletonize.getY(),
                toSkeletonize.getZ(),
                100,
                SkeletonizeLayout.encode(toSkeletonize.getId(), brightness));
    }

    public static void skeletonGib(Level level, Entity toSkeletonize, float force) {
        send(
                level,
                toSkeletonize.getX(),
                toSkeletonize.getY(),
                toSkeletonize.getZ(),
                100,
                SkeletonGibLayout.encode(toSkeletonize.getId(), force));
    }

    public static void vomit(Level level, Entity entity, VomitMode mode, int count) {
        send(
                level,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                25,
                VomitLayout.encode(entity.getId(), mode, count));
    }

    public static void sweat(Level level, Entity entity, BlockState state, int count) {
        sweat(level, entity, state, count, 25);
    }

    public static void sweat(Level level, Entity entity, BlockState state, int count, int range) {
        send(
                level,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                range,
                SweatLayout.encode(entity.getId(), Block.BLOCK_STATE_REGISTRY.getId(state), count));
    }

    public static void blackPowder(
            Level level,
            double x,
            double y,
            double z,
            double hx,
            double hy,
            double hz,
            int cloudCount,
            float cloudScale,
            float cloudSpeedMult,
            int sparkCount,
            float sparkSpeedMult) {
        send(
                level,
                x,
                y,
                z,
                200,
                BlackPowderLayout.encode(
                        x,
                        y,
                        z,
                        hx,
                        hy,
                        hz,
                        cloudCount,
                        cloudScale,
                        cloudSpeedMult,
                        sparkCount,
                        sparkSpeedMult));
    }

    public static void giblets(Level level, Entity entity, GibletType gibType, int countDivisor) {
        double y = entity.getY() + entity.getBbHeight() * 0.5;
        send(
                level,
                entity.getX(),
                y,
                entity.getZ(),
                150,
                GibletsLayout.encode(
                        entity.getX(), y, entity.getZ(), entity.getId(), gibType, countDivisor));
    }

    public static void sparks(
            Level level, double x, double y, double z, int count, boolean small, int range) {
        send(level, x, y, z, range, SparkBurstLayout.encode(x, y, z, count, small));
    }

    public static void debugText(
            Level level,
            double x,
            double y,
            double z,
            int color,
            float scale,
            String text,
            int range) {
        send(level, x, y, z, range, DebugTextLayout.encode(x, y, z, color, scale, text));
    }

    public static void droneLine(
            Level level,
            double x,
            double y,
            double z,
            double mx,
            double my,
            double mz,
            int color,
            int range) {
        send(level, x, y, z, range, DroneLineLayout.encode(x, y, z, mx, my, mz, color));
    }

    public static void fireworkLetter(
            Level level, double x, double y, double z, int color, char c) {
        send(level, x, y, z, 300, DebugLetterLayout.encode(x, y, z, color, c));
    }

    private static void send(
            Level level, double x, double y, double z, int range, EncodedPayload payload) {
        if (!(level instanceof ServerLevel server)) return;
        PacketWire.sendToAllAround(payload, new TargetPoint(server, x, y, z, range));
    }
}
