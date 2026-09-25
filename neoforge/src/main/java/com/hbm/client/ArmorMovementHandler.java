// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.items.armor.ArmorDash;
import com.hbm.items.armor.ArmorSuitEffects.Jetpack;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ItemWings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class ArmorMovementHandler {

    private ArmorMovementHandler() {}

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.isPaused()) return;

        ArmorSuitEffects.Jetpack kind = ArmorSuitEffects.jetpackKind(player);
        if (kind != null) jetpack(player, kind);
        ItemWings.Kind wings = ArmorSuitEffects.wingsKind(player);
        if (wings != null) wings(player, wings);
        if (ArmorSuitEffects.hasEnvsuitSwim(player)) envsuitSwim(player);
        ArmorDash.tick(player, forwardInput(), strafeInput());
    }

    private static void jetpack(LocalPlayer player, Jetpack kind) {
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        Vec3 motion = player.getDeltaMovement();

        if (props.isJetpackActive() && !(kind == Jetpack.BRAKE && player.isShiftKeyDown())) {

            if (motion.y < kind.ceiling)
                player.setDeltaMovement(motion.x, motion.y + kind.thrust, motion.z);
            if (kind.lookThrust == 0D) player.fallDistance = 0;

            if (kind.lookThrust > 0D && player.getDeltaMovement().length() < kind.speedCap) {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(player.getDeltaMovement().add(look.scale(kind.lookThrust)));
                if (look.y > 0D) player.fallDistance = 0;
            }
            return;
        }

        if (kind == Jetpack.BRAKE) {
            brakeHover(player, motion, props);
            return;
        }

        if (kind == Jetpack.BJ) {
            glide(player, motion, 0.4D);
            return;
        }

        if (kind != Jetpack.DNS) return;

        if (!player.isShiftKeyDown() && !player.onGround() && props.enableBackpack) {
            player.fallDistance = 0;
            double dy =
                    motion.y < -1D
                            ? motion.y + 0.4D
                            : motion.y < -0.1D ? motion.y + 0.2D : motion.y < 0D ? 0D : motion.y;
            double dx = motion.x * 1.05D;
            double dz = motion.z * 1.05D;
            float forward = forwardInput();
            if (forward != 0F) {
                Vec3 look = player.getLookAngle();
                dx += look.x * 0.25D * forward;
                dz += look.z * 0.25D * forward;
            }
            player.setDeltaMovement(dx, dy, dz);
        }

        if (player.isShiftKeyDown() && !player.onGround()) {
            Vec3 current = player.getDeltaMovement();
            player.setDeltaMovement(current.x, current.y - 0.1D, current.z);
        }
    }

    private static void brakeHover(LocalPlayer player, Vec3 motion, HbmPlayerProps props) {
        if (player.isShiftKeyDown() && !props.isJetpackActive()
                || player.onGround()
                || !props.enableBackpack) return;
        player.fallDistance = 0;
        double dy =
                motion.y < -1D
                        ? motion.y + 0.2D
                        : motion.y < -0.1D ? motion.y + 0.1D : motion.y < 0D ? 0D : motion.y;
        player.setDeltaMovement(motion.x * 1.025D, dy, motion.z * 1.025D);
    }

    private static void glide(LocalPlayer player, Vec3 motion, double share) {
        if (!player.isShiftKeyDown() || motion.y >= -0.08D) return;
        double mo = motion.y * -share;
        Vec3 look = player.getLookAngle().scale(mo);
        player.setDeltaMovement(motion.x + look.x, motion.y + mo + look.y, motion.z + look.z);
    }

    private static void wings(LocalPlayer player, ItemWings.Kind kind) {
        if (player.onGround()) return;
        player.fallDistance = 0;
        Vec3 motion = player.getDeltaMovement();

        if (kind == ItemWings.Kind.LIMP) {
            if (motion.y < -0.4D) player.setDeltaMovement(motion.x, -0.4D, motion.z);
            glide(player, player.getDeltaMovement(), 0.2D);
            return;
        }

        HbmPlayerProps props = HbmPlayerProps.getData(player);
        double dy = motion.y;
        if (props.isJetpackActive()) {
            if (player.isShiftKeyDown()) {
                dy =
                        dy < -1D
                                ? dy + 0.4D
                                : dy < -0.1D
                                        ? dy + 0.2D
                                        : dy < 0D
                                                ? 0D
                                                : dy > 1D ? dy - 0.4D : dy > 0.1D ? dy - 0.2D : 0D;
            } else {
                dy = dy < 0.6D ? dy + 0.2D : 0.8D;
            }
        } else if (props.enableBackpack && !player.isShiftKeyDown()) {
            dy = dy < -1D ? dy + 0.4D : dy < -0.1D ? dy + 0.2D : dy < 0D ? 0D : dy;
        }

        double dx = motion.x;
        double dz = motion.z;
        if (props.enableBackpack) {
            Vec3 look = player.getLookAngle().horizontal().normalize();
            double mod = player.isSprinting() ? 1D : 0.25D;

            if (player.zza != 0F) {
                dx += look.x * 0.35D * player.zza * mod;
                dz += look.z * 0.35D * player.zza * mod;
            }
            if (player.xxa != 0F) {
                Vec3 side = look.yRot((float) Math.PI * 0.5F);
                dx += side.x * 0.15D * player.xxa * mod;
                dz += side.z * 0.15D * player.xxa * mod;
            }
        }
        player.setDeltaMovement(dx, dy, dz);
    }

    private static void envsuitSwim(LocalPlayer player) {
        if (!player.isInWater()) return;
        float forward = forwardInput();
        if (forward == 0F) return;
        player.setDeltaMovement(
                player.getDeltaMovement().add(player.getLookAngle().scale(0.1D * forward)));
    }

    private static float forwardInput() {
        Options options = Minecraft.getInstance().options;
        float forward = 0F;
        if (options.keyUp.isDown()) forward += 1F;
        if (options.keyDown.isDown()) forward -= 1F;
        return forward;
    }

    private static float strafeInput() {
        Options options = Minecraft.getInstance().options;
        float strafe = 0F;
        if (options.keyLeft.isDown()) strafe += 1F;
        if (options.keyRight.isDown()) strafe -= 1F;
        return strafe;
    }
}
