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

import com.hbm.blockentity.machine.ElectricPumpBlockEntity;
import com.hbm.blockentity.machine.PumpBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class PumpRenderer
        implements BlockEntityRenderer<PumpBlockEntity, PumpRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROTOR = ResourceManager.pump.partId("Rotor");
    private static final int ARMS = ResourceManager.pump.partId("Arms");
    private static final int PISTON = ResourceManager.pump.partId("Piston");

    private final RenderType steamType;
    private final RenderType electricType;

    public PumpRenderer() {
        this.steamType = WorldRenderPipeline.oneSidedCutout(ResourceManager.pump_steam_tex);
        this.electricType = WorldRenderPipeline.oneSidedCutout(ResourceManager.pump_electric_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 270);
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
            PumpBlockEntity be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(MultiblockCoreBlock.FACING);
        state.rotation = Mth.lerp(pt, be.lastRotor, be.rotor);
        state.electric = be instanceof ElectricPumpBlockEntity;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        double rot = s.rotation;
        RenderType bodyType = s.electric ? electricType : steamType;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(facingYaw(s.facing)));

        ps.pushPose();
        ps.translate(0, 2.25, 0);
        ps.rotate((Quaternionfc) Axis.ZP.rotationDegrees((float) (rot - 90)));
        ps.translate(0, -2.25, 0);
        part(col, ps, bodyType, light, ROTOR);
        ps.popPose();

        double sin = Math.sin(Math.toRadians(rot)) * 0.5 - 0.5;
        double cos = Math.cos(Math.toRadians(rot)) * 0.5;
        double ang = Math.acos(cos / 2.0);
        double cath = Math.sqrt(1 + (cos * cos) / 2);

        ps.pushPose();
        ps.translate(0, 1 - cath + sin, 0);
        ps.translate(0, 4.75, 0);
        ps.rotate((Quaternionfc) Axis.ZN.rotationDegrees((float) (Math.toDegrees(ang) - 90)));
        ps.translate(0, -4.75, 0);
        part(col, ps, bodyType, light, ARMS);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 1 - cath + sin, 0);
        part(col, ps, bodyType, light, PISTON);
        ps.popPose();

        ps.popPose();
    }

    private void part(
            SubmitNodeCollector col, PoseStack ps, RenderType bodyType, int light, int name) {
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buffer) -> ResourceManager.pump.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.WEST;
        public float rotation;
        public boolean electric;
    }
}
