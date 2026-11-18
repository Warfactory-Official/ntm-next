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

package com.hbm.render.item.weapon.sedna;

import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.AnimatedGunItemRenderer;
import com.hbm.render.item.weapon.sedna.GunAnimationChannels;
import com.hbm.render.item.weapon.sedna.GunRenderDefinition;
import com.hbm.sound.ModSounds;
import com.hbm.util.EntityTargeting;
import com.hbm.util.GameTime;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Quaternionfc;

public class FollyItemRenderer extends AnimatedGunItemRenderer {
    public static long timeAiming;
    public static boolean jingle = false;
    public static boolean wasAiming = false;

    public FollyItemRenderer() {
        super(
                GunRenderDefinition.on(Rig.RIG)
                        .shift(0, 0, 0.875)
                        .aim(
                                -2.5F * 0.8F,
                                -1.5F * 0.8F,
                                2.75F * 0.8F,
                                -2 * 0.75F,
                                -1 * 0.75F,
                                2.25F * 0.8F)
                        .body()
                        .size(0.75)
                        .mesh(ResourceManager.folly)
                        .skin(ResourceManager.folly_tex)
                        .hinge(0, 1, -4, Axis.XN, Rig.DRAW)
                        .hinge(0, -2, -2, Axis.XP, Rig.LOAD)
                        .part("Cannon")
                        .group(barrel -> barrel.move(Rig.KICK).part("Barrel"))
                        .group(shell -> shell.move(Rig.SHELL).part("Shell"))
                        .group(
                                breech ->
                                        breech.move(Rig.BREECH)
                                                .part("Breech")
                                                .hinge(0, 1, 0, Axis.ZP, Rig.SCREW)
                                                .part("Cog"))
                        .custom(FollyItemRenderer::scope, null)
                        .build());
    }

    private static void scope(GunRenderDefinition.Drawing d) {
        boolean aiming = d.steadyAim();
        if (aiming & !wasAiming) timeAiming = GameTime.millis();
        if (aiming) {
            LocalPlayer player = Minecraft.getInstance().player;
            Component splash = getBootSplash();
            if (!jingle && splash != null) {
                player.level()
                        .playLocalSound(
                                player,
                                ModSounds
                                        .GUN_VYLET_PONY_CUTIEMARKS_AND_THE_THINGS_THAT_BIND_US_INTRO_JINGLE
                                        .get(),
                                SoundSource.PLAYERS,
                                0.5F,
                                1F);
                jingle = true;
            }
            Font font = Minecraft.getInstance().font;
            float variance = 0.85F + player.getRandom().nextFloat() * 0.15F;
            int color = ARGB.colorFromFloat(1F, variance, variance * 0.5F, 0F);
            var pose = d.pose();
            var collector = d.collector();
            if (GameTime.now() - timeAiming > 5000 && d.get(Rig.LOAD) == 0) {
                String msg =
                        GunRenderDefinition.magazine(d).getAmount(d.stack(), d.inventory()) > 0
                                ? "+"
                                : "No ammo";
                pose.pushPose();
                float size = 0.01F;
                pose.translate(
                        (font.width(msg) / 2) * size + 2, 1F + font.lineHeight * size / 2F, -2.75F);
                pose.scale(size, -size, size);
                pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(180));
                collector.submitText(
                        pose,
                        0,
                        0,
                        Component.literal(msg).getVisualOrderText(),
                        false,
                        Font.DisplayMode.NORMAL,
                        FULL_BRIGHT,
                        color,
                        0,
                        0);
                pose.popPose();
            }
            if (splash != null) {
                pose.pushPose();
                float size = 0.02F;
                pose.translate(
                        (font.width(splash) / 2) * size + 2,
                        1F + font.lineHeight * size / 2F,
                        -2.75F);
                pose.scale(size, -size, size);
                pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(180));
                collector.submitText(
                        pose,
                        0,
                        0,
                        splash.getVisualOrderText(),
                        false,
                        Font.DisplayMode.NORMAL,
                        FULL_BRIGHT,
                        color,
                        0,
                        0);
                pose.popPose();
            }
            List<Component> tty = getTTY();
            if (!tty.isEmpty()) {
                pose.pushPose();
                float size = 0.005F;
                pose.translate(2.5F, 1.375F, -2.75F);
                pose.scale(size, -size, size);
                pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(180));
                for (Component line : tty) {
                    collector.submitText(
                            pose,
                            0,
                            0,
                            line.getVisualOrderText(),
                            false,
                            Font.DisplayMode.NORMAL,
                            FULL_BRIGHT,
                            color,
                            0,
                            0);
                    pose.translate(0, font.lineHeight + 2, 0);
                }
                pose.popPose();
            }
        } else {
            jingle = false;
        }
        wasAiming = aiming;
    }

    public static Component getBootSplash() {
        long now = GameTime.now();
        if (timeAiming + 5000 < now) return null;
        if (timeAiming + 3000 > now) return null;
        int splashIndex = (int) ((now - timeAiming - 3000) * 35 / 2000) - 10;
        char[] letters = "VStarOS".toCharArray();
        MutableComponent splash = Component.empty();
        for (int i = 0; i < letters.length; i++) {
            ChatFormatting color;
            if (i < splashIndex - 1) color = ChatFormatting.LIGHT_PURPLE;
            else if (i == splashIndex - 1) color = ChatFormatting.AQUA;
            else if (i == splashIndex) color = ChatFormatting.WHITE;
            else if (i == splashIndex + 1) color = ChatFormatting.AQUA;
            else if (i == splashIndex + 2) color = ChatFormatting.LIGHT_PURPLE;
            else color = ChatFormatting.BLACK;
            splash.append(Component.literal(String.valueOf(letters[i])).withStyle(color));
        }
        return splash;
    }

    public static List<Component> getTTY() {
        List<Component> tty = new ArrayList<>();
        long now = GameTime.now();
        int time = (int) (now - timeAiming);
        if (time < 3000) {
            if (time > 250)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.postSuccessfulCode0")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 500)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.8388608Bytes")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 500)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.5187427Bytes")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 750)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.reticulatingSplines")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 1500)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.noKeyboardFound")
                                .withStyle(ChatFormatting.GREEN));
            if (time > 2000)
                tty.add(
                        Component.translatable("desc.gui.renderFolly.bootingFromDevSda1")
                                .withStyle(ChatFormatting.GREEN));
        }
        if (time > 5000) {
            LocalPlayer player = Minecraft.getInstance().player;
            HitResult mop = EntityTargeting.getMouseOver(player, 250);
            String target = "Target: ";
            if (mop == null || mop.getType() == HitResult.Type.MISS) target += "N/A";
            if (mop instanceof BlockHitResult blockHit && mop.getType() == HitResult.Type.BLOCK)
                target +=
                        blockHit.getBlockPos().getX()
                                + "/"
                                + blockHit.getBlockPos().getY()
                                + "/"
                                + blockHit.getBlockPos().getZ();
            if (mop instanceof EntityHitResult entityHit)
                target += entityHit.getEntity().getName().getString();
            tty.add(Component.literal(target).withStyle(ChatFormatting.GREEN));
            tty.add(
                    Component.translatable(
                                    "desc.gui.renderFolly.angle",
                                    ((int) (-player.getXRot() * 100) / 100D))
                            .withStyle(ChatFormatting.GREEN));
        }
        return tty;
    }

    @Override
    protected float getTurnMagnitude(ItemStack stack) {
        return GunBaseNTItem.getIsAiming(stack) ? 2F : 2.5F;
    }

    private static final class Rig {
        static final GunAnimationChannels RIG = new GunAnimationChannels();
        static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
        static final GunAnimationChannels.Vec3 KICK = RIG.vec3("RECOIL");
        static final GunAnimationChannels.Scalar LOAD = RIG.x("LOAD");
        static final GunAnimationChannels.Vec3 SHELL = RIG.vec3("SHELL");
        static final GunAnimationChannels.Scalar SCREW = RIG.z("SCREW");
        static final GunAnimationChannels.Vec3 BREECH = RIG.vec3("BREECH");
    }
}
