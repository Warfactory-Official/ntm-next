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

package com.hbm.client.render;

import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.BlendFactor;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public final class AssemblyMarkers {
    private static final Long2ObjectOpenHashMap<Marker> MARKERS = new Long2ObjectOpenHashMap<>();
    private static final Marker[] EMPTY = new Marker[0];
    private static final State EMPTY_STATE = new State(EMPTY, Vec3.ZERO);
    private static @Nullable ClientLevel markerLevel;
    private static final float LABEL_SCALE = 0.016666668F * 1.6F;
    static final RenderPipeline MARKER_LINES_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(ParticleRenderTypes.LINES_NO_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_assembly_marker_lines")
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA)))
                            .withDepthStencilState(
                                    new DepthStencilState(CompareOp.ALWAYS_PASS, false)));
    private static final RenderType MARKER_LINES =
            RenderType.create(
                    "ntm_assembly_marker_lines",
                    RenderSetup.builder(MARKER_LINES_PIPELINE).createRenderSetup());

    private AssemblyMarkers() {}

    public static void clear() {
        MARKERS.clear();
        markerLevel = null;
    }

    private static void updateLevel() {
        ClientLevel level = Minecraft.getInstance().level;
        if (markerLevel != level) {
            MARKERS.clear();
            markerLevel = level;
        }
    }

    public static void queue(Marker marker) {
        updateLevel();
        if (markerLevel != null) MARKERS.put(marker.pos.asLong(), marker);
    }

    public static State extract(Vec3 cameraPosition, float partialTick) {
        updateLevel();
        if (MARKERS.isEmpty()) return EMPTY_STATE;
        long now = GameTime.now();
        var alive = new ArrayList<Marker>(MARKERS.size());
        var it = MARKERS.values().iterator();
        while (it.hasNext()) {
            Marker marker = it.next();
            double dx = marker.pos.getX() + 0.5 - cameraPosition.x;
            double dy = marker.pos.getY() + 0.5 - cameraPosition.y;
            double dz = marker.pos.getZ() + 0.5 - cameraPosition.z;
            if ((marker.expireAt > 0 && now > marker.expireAt)
                    || (marker.maxDist > 0 && Mth.length(dx, dy, dz) > marker.maxDist)) {
                it.remove();
            } else {
                alive.add(marker);
            }
        }
        return alive.isEmpty()
                ? EMPTY_STATE
                : new State(
                        alive.toArray(EMPTY),
                        Minecraft.getInstance().player.getViewVector(partialTick));
    }

    public static void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera,
            Font font) {
        for (Marker marker : state.markers) {
            poseStack.pushPose();
            poseStack.translate(
                    marker.pos.getX() - camera.pos.x,
                    marker.pos.getY() - camera.pos.y,
                    marker.pos.getZ() - camera.pos.z);
            collector.submitShapeOutline(
                    poseStack,
                    Shapes.block(),
                    MARKER_LINES,
                    ARGB.opaque(marker.color),
                    1.0F,
                    false);
            poseStack.popPose();

            double vx = marker.pos.getX() + 0.5 - camera.pos.x;
            double vy = marker.pos.getY() + 0.5 - camera.pos.y;
            double vz = marker.pos.getZ() + 0.5 - camera.pos.z;
            double distance = Mth.length(vx, vy, vz);
            if (distance < 1.0E-4 || distance > 100.0) continue;
            double mult = Math.min(distance, 16.0) / distance;
            Component label = marker.label;
            boolean empty = label.getString().isEmpty();
            if (Math.abs(state.look.x - vx / distance)
                            + Math.abs(state.look.y - vy / distance)
                            + Math.abs(state.look.z - vz / distance)
                    < 0.15) {
                label =
                        empty
                                ? Component.translatable("marker.hbm.distance", (int) distance)
                                : Component.translatable(
                                        "marker.hbm.label_distance", label, (int) distance);
            } else if (empty) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(vx * mult, vy * mult + 1.0, vz * mult);
            poseStack.rotate((Quaternionfc) camera.orientation);
            poseStack.scale(LABEL_SCALE, -LABEL_SCALE, LABEL_SCALE);
            FormattedCharSequence text = label.getVisualOrderText();
            int x = -font.width(text) / 2;
            collector.submitText(
                    poseStack,
                    x,
                    0,
                    text,
                    false,
                    Font.DisplayMode.SEE_THROUGH,
                    LightCoordsUtil.FULL_BRIGHT,
                    ARGB.opaque(marker.color),
                    0x3f000000,
                    0);
            collector.submitText(
                    poseStack,
                    x,
                    0,
                    text,
                    false,
                    Font.DisplayMode.NORMAL,
                    LightCoordsUtil.FULL_BRIGHT,
                    ARGB.opaque(marker.color),
                    0,
                    0);
            poseStack.popPose();
        }
    }

    public record Marker(BlockPos pos, Component label, long expireAt, double maxDist, int color) {}

    public record State(Marker[] markers, Vec3 look) {}
}
