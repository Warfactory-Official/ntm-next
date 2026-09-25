// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle.helper;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.threading.ThreadedPayload;
import com.hbm.packet.toclient.AshesPayload;
import com.hbm.packet.toclient.BlackPowderPayload;
import com.hbm.packet.toclient.DebugParticlePayload;
import com.hbm.packet.toclient.GibletPayload;
import com.hbm.packet.toclient.SkeletonPayload;
import com.hbm.packet.toclient.SparkBurstPayload;
import com.hbm.packet.toclient.SweatPayload;
import com.hbm.packet.toclient.VomitPayload;
import com.hbm.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
                new AshesPayload(
                        toPulverize.getX(),
                        toPulverize.getY(),
                        toPulverize.getZ(),
                        toPulverize.getId(),
                        count,
                        scale));
    }

    public static void skeleton(Level level, Entity toSkeletonize, float brightness) {
        send(
                level,
                toSkeletonize.getX(),
                toSkeletonize.getY(),
                toSkeletonize.getZ(),
                100,
                SkeletonPayload.skeletonize(
                        toSkeletonize.getX(),
                        toSkeletonize.getY(),
                        toSkeletonize.getZ(),
                        toSkeletonize.getId(),
                        brightness));
    }

    public static void skeletonGib(Level level, Entity toSkeletonize, float force) {
        send(
                level,
                toSkeletonize.getX(),
                toSkeletonize.getY(),
                toSkeletonize.getZ(),
                100,
                SkeletonPayload.gib(
                        toSkeletonize.getX(),
                        toSkeletonize.getY(),
                        toSkeletonize.getZ(),
                        toSkeletonize.getId(),
                        force));
    }

    public static void vomit(Level level, Entity entity, byte mode, int count) {
        send(
                level,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                25,
                new VomitPayload(entity.getId(), mode, count));
    }

    public static void sweat(Level level, Entity entity, BlockState state, int count) {
        send(
                level,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                25,
                new SweatPayload(entity.getId(), state, count));
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
                new BlackPowderPayload(
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

    public static void giblets(Level level, Entity entity, int gibType, int countDivisor) {
        double y = entity.getY() + entity.getBbHeight() * 0.5;
        send(
                level,
                entity.getX(),
                y,
                entity.getZ(),
                150,
                new GibletPayload(
                        entity.getX(), y, entity.getZ(), entity.getId(), gibType, countDivisor));
    }

    public static void sparks(
            Level level, double x, double y, double z, int count, boolean small, int range) {
        send(level, x, y, z, range, new SparkBurstPayload(x, y, z, count, small));
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
        send(level, x, y, z, range, DebugParticlePayload.text(x, y, z, color, scale, text));
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
        send(level, x, y, z, range, DebugParticlePayload.droneLine(x, y, z, mx, my, mz, color));
    }

    public static void fireworkLetter(
            Level level, double x, double y, double z, int color, char c) {
        send(level, x, y, z, 300, DebugParticlePayload.letter(x, y, z, color, c));
    }

    private static void send(
            Level level, double x, double y, double z, int range, ThreadedPayload payload) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(payload, new TargetPoint(server, x, y, z, range));
    }
}
