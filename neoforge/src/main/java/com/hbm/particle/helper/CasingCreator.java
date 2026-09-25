// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.particle.helper;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.CasingEjectPayload;
import com.hbm.packet.toclient.GunCasingEjectPayload;
import com.hbm.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CasingCreator {

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

        Vec3 motion =
                new Vec3(sideMotion, heightMotion, frontMotion)
                        .xRot(-pitch / 180F * (float) Math.PI)
                        .yRot(-yaw / 180F * (float) Math.PI);

        double mX = motion.x + level.getRandom().nextGaussian() * motionVariance;
        double mY = motion.y + level.getRandom().nextGaussian() * motionVariance;
        double mZ = motion.z + level.getRandom().nextGaussian() * motionVariance;

        Services.NETWORK.sendToAllAround(
                new CasingEjectPayload(
                        x, y, z, mX, mY, mZ, yaw, pitch, mPitch, mYaw, casing, smoking, smokeLife,
                        smokeLift, nodeLife),
                new TargetPoint(server, x, y, z, 50));
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

        if (player.isShiftKeyDown()) heightOffset -= 0.075F;

        Vec3 offset =
                new Vec3(sideOffset, heightOffset, frontOffset)
                        .xRot(-player.getXRot() / 180F * (float) Math.PI)
                        .yRot(-player.getYRot() / 180F * (float) Math.PI);

        double x = player.getX() + offset.x;
        double y = player.getY() + player.getEyeHeight() + offset.y;
        double z = player.getZ() + offset.z;

        Vec3 motion =
                new Vec3(sideMotion, heightMotion, frontMotion)
                        .xRot(-player.getXRot() / 180F * (float) Math.PI)
                        .yRot(-player.getYRot() / 180F * (float) Math.PI);

        double mX =
                player.getDeltaMovement().x
                        + motion.x
                        + player.getRandom().nextGaussian() * motionVariance;
        double mY =
                player.getDeltaMovement().y
                        + motion.y
                        + player.getRandom().nextGaussian() * motionVariance;
        double mZ =
                player.getDeltaMovement().z
                        + motion.z
                        + player.getRandom().nextGaussian() * motionVariance;

        if (player instanceof Player p && p.getAbilities().flying) mY -= 0.04D;

        if (legacyFrontOffset == frontOffset) {
            Services.NETWORK.sendToAllAround(
                    new CasingEjectPayload(
                            x,
                            y,
                            z,
                            mX,
                            mY,
                            mZ,
                            player.getYRot(),
                            player.getXRot(),
                            mPitch,
                            mYaw,
                            casing,
                            smoking,
                            smokeLife,
                            smokeLift,
                            nodeLife),
                    new TargetPoint(server, x, y, z, 50));
        } else {
            Vec3 legacyOffset =
                    new Vec3(sideOffset, heightOffset, legacyFrontOffset)
                            .xRot(-player.getXRot() / 180F * (float) Math.PI)
                            .yRot(-player.getYRot() / 180F * (float) Math.PI);
            Services.NETWORK.sendToAllAround(
                    new GunCasingEjectPayload(
                            x,
                            y,
                            z,
                            mX,
                            mY,
                            mZ,
                            player.getYRot(),
                            player.getXRot(),
                            mPitch,
                            mYaw,
                            casing,
                            smoking,
                            smokeLife,
                            smokeLift,
                            nodeLife,
                            false),
                    new TargetPoint(server, x, y, z, 50));
            double legacyX = player.getX() + legacyOffset.x;
            double legacyY = player.getY() + player.getEyeHeight() + legacyOffset.y;
            double legacyZ = player.getZ() + legacyOffset.z;
            Services.NETWORK.sendToAllAround(
                    new GunCasingEjectPayload(
                            legacyX,
                            legacyY,
                            legacyZ,
                            mX,
                            mY,
                            mZ,
                            player.getYRot(),
                            player.getXRot(),
                            mPitch,
                            mYaw,
                            casing,
                            smoking,
                            smokeLife,
                            smokeLift,
                            nodeLife,
                            true),
                    new TargetPoint(server, legacyX, legacyY, legacyZ, 50));
        }
    }
}
