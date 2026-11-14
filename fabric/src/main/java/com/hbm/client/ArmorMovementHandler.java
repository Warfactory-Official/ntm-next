/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.client;

import com.hbm.api.environment.Environment;
import com.hbm.api.environment.EnvironmentProvider;
import com.hbm.extprop.PlayerState;
import com.hbm.items.armor.ArmorDash;
import com.hbm.items.armor.ArmorFlight;
import com.hbm.items.armor.ArmorSuitEffects.Jetpack;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.armor.WingsItem;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ArmorFlightLayout;
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
        WingsItem.Kind wings = ArmorSuitEffects.wingsKind(player);
        PlayerState props = PlayerState.getOrCreate(player);
        ArmorFlight.Mode mode = ArmorFlight.mode(player, kind, wings);
        int intent = mode.ordinal();
        if (kind != null) jetpack(player, kind);
        if (wings != null) wings(player, wings);
        boolean capped =
                !player.isFallFlying()
                        && player.getDeltaMovement().y < -0.25D
                        && ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.EUPHEMIUM);
        if (capped) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, -0.25D, motion.z);
            player.fallDistance = 0;
        }
        if (props.flightIntent != intent || props.fallCapped != capped) {
            props.flightIntent = intent;
            props.fallCapped = capped;
            PacketWire.sendToServer(ArmorFlightLayout.encode(mode, capped));
        }
        if (ArmorSuitEffects.hasEnvsuitSwim(player)) envsuitSwim(player);
        ArmorDash.tick(player, forwardInput(), strafeInput());
    }

    private static void jetpack(LocalPlayer player, Jetpack kind) {
        PlayerState props = PlayerState.getOrCreate(player);
        Vec3 motion = player.getDeltaMovement();
        float gravity = Environment.gravity(player);

        if (props.jetpackActive() && !(kind == Jetpack.BRAKE && player.isShiftKeyDown())) {
            if (gravity == 0F && (kind == Jetpack.FLY || kind == Jetpack.BRAKE)) {
                player.setDeltaMovement(motion.add(player.getLookAngle().scale(0.05D)));
            } else if (motion.y < kind.ceiling) {
                player.setDeltaMovement(
                        motion.x, motion.y + kind.thrust * kind.lift(gravity), motion.z);
            }
            if (kind.lookThrust == 0D) player.fallDistance = 0;
            if (kind.lookThrust > 0D && player.getDeltaMovement().length() < kind.speedCap) {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(player.getDeltaMovement().add(look.scale(kind.lookThrust)));
                if (look.y > 0D) player.fallDistance = 0;
            }
            if (kind == Jetpack.DNS && player.isShiftKeyDown() && !player.onGround()) {
                Vec3 current = player.getDeltaMovement();
                player.setDeltaMovement(current.x, current.y - 0.1D, current.z);
            }
            return;
        }

        if (player.isFallFlying() && !props.jetpackActive()) return;

        if (kind == Jetpack.BRAKE) {
            brakeHover(player, motion, props, gravity);
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

    private static void brakeHover(
            LocalPlayer player, Vec3 motion, PlayerState props, float gravity) {
        if (player.isShiftKeyDown() && !props.jetpackActive()
                || player.onGround()
                || !props.enableBackpack
                || !(gravity > 0F)) return;
        player.fallDistance = 0;
        float thrust = Math.max(gravity / EnvironmentProvider.STANDARD_GRAVITY, 1);
        double dy =
                motion.y < -1 * thrust
                        ? motion.y + 0.2D * thrust
                        : motion.y < -0.1 * thrust
                                ? motion.y + 0.1D * thrust
                                : motion.y < 0D ? 0D : motion.y;
        player.setDeltaMovement(motion.x * 1.025D, dy, motion.z * 1.025D);
    }

    private static void glide(LocalPlayer player, Vec3 motion, double share) {
        if (!player.isShiftKeyDown() || motion.y >= -0.08D) return;
        double mo = motion.y * -share;
        Vec3 look = player.getLookAngle().scale(mo);
        player.setDeltaMovement(motion.x + look.x, motion.y + mo + look.y, motion.z + look.z);
    }

    private static void wings(LocalPlayer player, WingsItem.Kind kind) {
        if (player.onGround()) return;
        if (player.isFallFlying()
                && (kind == WingsItem.Kind.LIMP
                        || !PlayerState.getOrCreate(player).jetpackActive())) return;
        player.fallDistance = 0;
        Vec3 motion = player.getDeltaMovement();

        if (kind == WingsItem.Kind.LIMP) {
            if (motion.y < -0.4D) player.setDeltaMovement(motion.x, -0.4D, motion.z);
            glide(player, player.getDeltaMovement(), 0.2D);
            return;
        }

        PlayerState props = PlayerState.getOrCreate(player);
        double dy = motion.y;
        if (props.jetpackActive()) {
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
        } else if (props.enableBackpack) {
            float gravity = Environment.gravity(player);
            if (gravity > 1.5F) {
                if (Environment.installed()) return;
            } else {
                if (gravity != 0 && gravity < 0.2F) gravity = 0.2F;
                dy /= 0.98F;
                dy += gravity / 20F;
                dy -= EnvironmentProvider.STANDARD_GRAVITY / 20F;
                dy *= 0.98F;
            }
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
