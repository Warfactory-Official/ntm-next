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

import com.hbm.blockentity.machine.ExposureChamberBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class ExposureChamberRenderer
        implements BlockEntityRenderer<ExposureChamberBlockEntity, ExposureChamberRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final int MAGNETS = ResourceManager.exposure_chamber.partId("Magnets");
    private static final int CORE = ResourceManager.exposure_chamber.partId("Core");

    private static final int FLICKER_PERIOD = 8;
    private static final int FLICKER_CHANCE = 2;
    private static final int FLICKER_TINT = 0x80d0ff;
    private static final int FLICKER_PLAIN = 0xFFFFFF;
    private static final Vec3 INJECTOR = new Vec3(0, 0, 5);
    private static final Vec3 COLUMN = new Vec3(0, 1.5, 0);
    private static final Vec3 RING = new Vec3(0, 0, -1);

    private final HFRWavefrontObject model;
    private final RenderType baseType;
    private final RenderType coreType;

    public ExposureChamberRenderer() {
        this.model = ResourceManager.exposure_chamber;
        this.baseType = WorldRenderPipeline.oneSidedCutout(ResourceManager.exposure_chamber_tex);
        this.coreType = FlatCutout.of(ResourceManager.exposure_chamber_tex);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            ExposureChamberBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.yaw = Facing.yaw(be.getBlockState().getValue(MultiblockCoreBlock.FACING), 90);
        state.angle = Mth.lerp(partialTicks, be.previousAngle, be.angle);
        state.running = be.running;

        long time = be.getLevel().getGameTime();
        state.bob = Math.sin((time % (Math.PI * 16D) + partialTicks) * 0.125D) * 0.0625D;
        state.flickerSeed = time / FLICKER_PERIOD;
        state.tinted = time % FLICKER_PERIOD >= FLICKER_PERIOD / 2;
        state.beamStart = GameTime.now();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(s.angle));
        col.submitCustomGeometry(
                ps, baseType, (pose, buf) -> model.renderPart(pose, buf, light, -1, MAGNETS));
        ps.popPose();

        if (s.running) {
            ps.pushPose();
            ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(s.angle / 2F));
            ps.translate(0.0, s.bob, 0.0);
            col.submitCustomGeometry(
                    ps,
                    coreType,
                    (pose, buf) ->
                            model.renderPart(pose, buf, LightCoordsUtil.FULL_BRIGHT, -1, CORE));
            ps.popPose();

            submitBeams(s, ps, col);
        }

        ps.popPose();
    }

    private void submitBeams(State s, PoseStack ps, SubmitNodeCollector col) {
        Random rand = new Random(s.flickerSeed);
        int color = s.tinted ? FLICKER_TINT : FLICKER_PLAIN;
        int start = (int) (s.beamStart % 1000) / 50;
        rand.nextInt(FLICKER_CHANCE);

        if (rand.nextInt(FLICKER_CHANCE) == 0) injector(ps, col, 0, 3.675, color, start);
        if (rand.nextInt(FLICKER_CHANCE) == 0) injector(ps, col, 1.1875, 2.5, color, start);
        if (rand.nextInt(FLICKER_CHANCE) == 0) injector(ps, col, -1.1875, 2.5, color, start);

        ps.pushPose();
        ps.translate(0.0, 1.75, 0.0);
        BeamPronter.prontBeam(
                ps,
                col,
                COLUMN,
                EnumWaveType.RANDOM,
                EnumBeamType.LINE,
                FLICKER_TINT,
                FLICKER_PLAIN,
                start,
                10,
                0.125F,
                1,
                0);
        BeamPronter.prontBeam(
                ps,
                col,
                COLUMN,
                EnumWaveType.RANDOM,
                EnumBeamType.LINE,
                0x8080FF,
                FLICKER_PLAIN,
                (int) (s.beamStart + 5) / 50,
                10,
                0.125F,
                1,
                0);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0, 2.5, 0.0);
        int spin = (int) (s.beamStart % 360);
        BeamPronter.prontBeam(
                ps,
                col,
                RING,
                EnumWaveType.SPIRAL,
                EnumBeamType.LINE,
                0xFFFF80,
                FLICKER_PLAIN,
                spin,
                15,
                0.125F,
                1,
                0);
        BeamPronter.prontBeam(
                ps,
                col,
                RING,
                EnumWaveType.SPIRAL,
                EnumBeamType.LINE,
                0xFF8080,
                FLICKER_PLAIN,
                spin + 180,
                15,
                0.125F,
                1,
                0);
        ps.popPose();
    }

    private void injector(
            PoseStack ps, SubmitNodeCollector col, double x, double y, int color, int start) {
        ps.pushPose();
        ps.translate(x, y, -7.5);
        BeamPronter.prontBeam(
                ps,
                col,
                INJECTOR,
                EnumWaveType.RANDOM,
                EnumBeamType.LINE,
                color,
                FLICKER_PLAIN,
                start,
                15,
                0.125F,
                1,
                0);
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float angle;
        public boolean running;
        public double bob;
        public long flickerSeed;
        public boolean tinted;
        public long beamStart;
    }
}
