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

import com.hbm.blockentity.ordnance.launch.MissileErectorBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.items.weapon.MissileItem.MissileFormFactor;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public final class LaunchPadLargeRenderer
        implements BlockEntityRenderer<MissileErectorBlockEntity, LaunchPadLargeRenderer.State>,
                ConcurrentRenderStateExtraction {

    private static final RenderType PLATFORM =
            RenderTypes.entityCutoutCull(ResourceManager.missile_erector_pad_tex);
    private static final double MISSILE_Y = 2D;

    private @Nullable Erectors erectors;

    private Erectors erectors() {
        Erectors resolved = erectors;
        if (resolved != null) return resolved;
        Erector[] rows = new Erector[MissileFormFactor.values().length];
        rows[MissileFormFactor.ABM.ordinal()] =
                row("ABM", 1.5D, 1.25D, ResourceManager.missile_erector_abm_tex);
        rows[MissileFormFactor.MICRO.ordinal()] =
                row("Micro", 1.5D, 1.25D, ResourceManager.missile_erector_micro_tex);
        rows[MissileFormFactor.V2.ordinal()] =
                row("V2", 1.75D, 1.25D, ResourceManager.missile_erector_v2_tex);
        rows[MissileFormFactor.STRONG.ordinal()] =
                row("Strong", 3D, 1.5D, ResourceManager.missile_erector_strong_tex);
        rows[MissileFormFactor.HUGE.ordinal()] =
                row("Huge", 3D, 1.5D, ResourceManager.missile_erector_huge_tex);
        rows[MissileFormFactor.ATLAS.ordinal()] =
                row("Atlas", 4D, 1.5D, ResourceManager.missile_erector_atlas_tex);
        rows[MissileFormFactor.OTHER.ordinal()] = rows[MissileFormFactor.ABM.ordinal()];
        resolved = new Erectors(rows, ResourceManager.missile_erector.partId("Pad"));
        erectors = resolved;
        return resolved;
    }

    private static Erector row(String prefix, double pivotZ, double pivotY, Identifier texture) {
        HFRWavefrontObject mesh = ResourceManager.missile_erector;
        return new Erector(
                mesh.partId(prefix + "_Pad"),
                mesh.partId(prefix + "_Erector"),
                mesh.partId(prefix + "_Pivot"),
                mesh.partId(prefix + "_Rope"),
                pivotZ,
                pivotY,
                RenderTypes.entityCutoutCull(texture));
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
            MissileErectorBlockEntity pad,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pad, state, partialTicks, camera, breakProgress);
        state.facing = MultiblockCoreBlock.coreFacing(pad.getBlockState());
        Erectors resolved = erectors();
        Erector[] rows = resolved.rows();
        state.erector =
                pad.formIndex >= 0 && pad.formIndex < rows.length ? rows[pad.formIndex] : null;
        state.platformPart = resolved.platformPart();
        state.erected = pad.standingMissile;
        state.readyToLoad = pad.deploymentAvailable;
        state.erectorAngle = Mth.lerp(partialTicks, pad.previousArmAngle, pad.armAngle);
        state.lift = Mth.lerp(partialTicks, pad.previousLiftHeight, pad.liftHeight);
        state.missile =
                pad.displayedMissile == null ? null : PadMissiles.table().get(pad.displayedMissile);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        poseStack.mulPose((Quaternionfc) Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));

        draw(poseStack, collector, light, PLATFORM, state.platformPart);

        Erector erector = state.erector;
        if (erector != null) {
            poseStack.pushPose();
            draw(poseStack, collector, light, erector.skin(), erector.pad());
            if (state.missile != null && state.erected) {
                draw(poseStack, collector, light, erector.skin(), erector.rope());
            }
            poseStack.translate(0D, erector.pivotY(), -erector.pivotZ());
            poseStack.mulPose((Quaternionfc) Axis.XP.rotationDegrees(-state.erectorAngle));
            poseStack.translate(0D, -erector.pivotY(), erector.pivotZ());
            draw(poseStack, collector, light, erector.skin(), erector.pivot());
            poseStack.translate(0D, state.lift, 0D);
            draw(poseStack, collector, light, erector.skin(), erector.arm());

            if (state.erected) {
                poseStack.popPose();
                poseStack.pushPose();
            }

            PadMissiles.Missile missile = state.missile;
            if (missile != null && (state.erected || state.readyToLoad)) {
                poseStack.translate(0D, MISSILE_Y, 0D);
                if (missile.scale() != 1F) {
                    poseStack.scale(missile.scale(), missile.scale(), missile.scale());
                }
                collector.submitCustomGeometry(
                        poseStack,
                        missile.type(),
                        (pose, buffer) -> missile.mesh().render(pose, buffer, light, -1));
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void draw(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            int part) {
        HFRWavefrontObject mesh = ResourceManager.missile_erector;
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> mesh.renderPart(pose, buffer, light, -1, part));
    }

    private record Erectors(Erector[] rows, int platformPart) {}

    private record Erector(
            int pad, int arm, int pivot, int rope, double pivotZ, double pivotY, RenderType skin) {}

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        @Nullable Erector erector;
        int platformPart;
        boolean erected;
        boolean readyToLoad;
        float erectorAngle;
        float lift;
        PadMissiles.@Nullable Missile missile;
    }
}
