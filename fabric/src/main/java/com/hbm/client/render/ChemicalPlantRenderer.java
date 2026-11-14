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

import com.hbm.blockentity.processing.ChemicalPlantBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.recipes.loader.GenericRecipe;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class ChemicalPlantRenderer
        implements BlockEntityRenderer<ChemicalPlantBlockEntity, ChemicalPlantRenderer.State>,
                ConcurrentRenderStateExtraction {
    private static final int FRAME = ResourceManager.chemical_plant.partId("Frame");
    private static final int SLIDER = ResourceManager.chemical_plant.partId("Slider");
    private static final int SPINNER = ResourceManager.chemical_plant.partId("Spinner");
    private static final int FLUID = ResourceManager.chemical_plant.partId("Fluid");

    private static final float BASE_YAW = 90F;
    private static final int NO_TYPE_COLOR = 0x888888;

    private final HFRWavefrontObject model;
    private final RenderType baseType;
    private final RenderType fluidType =
            FlatTranslucent.culled(ResourceManager.chemical_plant_fluid_tex);

    public ChemicalPlantRenderer() {
        this.model = ResourceManager.chemical_plant;
        this.baseType = RenderTypes.entityCutoutCull(ResourceManager.chemical_plant_tex);
    }

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 0);
    }

    private static int sumChannels(FluidStackNTM[] stacks, float[] rgb) {
        for (FluidStackNTM stack : stacks) {
            NTMFluidProperty prop = NTMFluidProperties.get(stack.type());
            int argb = prop != null ? prop.colorARGB() : NO_TYPE_COLOR;
            rgb[0] += ARGB.red(argb);
            rgb[1] += ARGB.green(argb);
            rgb[2] += ARGB.blue(argb);
        }
        return stacks.length;
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
            ChemicalPlantBlockEntity be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(be.getBlockState().getValue(MultiblockCoreBlock.FACING));
        state.anim = Mth.lerp(partialTicks, be.prevAnim, be.anim);
        state.frame = be.frame;

        state.fluidColor = 0;
        state.fluidU = 0F;
        state.fluidV = 0F;
        GenericRecipe recipe = be.isProgressing ? be.module.getRecipe() : null;
        if (recipe == null) return;

        float[] rgb = new float[3];
        int colors = sumChannels(recipe.outputFluid, rgb);
        if (colors == 0) colors = sumChannels(recipe.inputFluid, rgb);
        if (colors == 0) return;

        state.fluidColor =
                ARGB.colorFromFloat(
                        0.5F,
                        rgb[0] / 255F / colors,
                        rgb[1] / 255F / colors,
                        rgb[2] / 255F / colors);
        state.fluidU = (float) (-state.anim / 100F);
        state.fluidV = (float) (sps(state.anim * 0.1) * 0.1 - 0.25);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees(BASE_YAW + s.yaw));

        if (s.frame) part(col, ps, FRAME, light);

        ps.pushPose();
        ps.translate((float) (sps(s.anim * 0.125) * 0.375), 0F, 0F);
        part(col, ps, SLIDER, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.rotate((Quaternionfc) Axis.YP.rotationDegrees((float) ((s.anim * 15) % 360D)));
        ps.translate(-0.5, 0.0, -0.5);
        part(col, ps, SPINNER, light);
        ps.popPose();

        if (s.fluidColor != 0) {
            int color = s.fluidColor;
            float u = s.fluidU, v = s.fluidV;
            col.submitCustomGeometry(
                    ps,
                    fluidType,
                    (pose, buffer) ->
                            model.renderPart(pose, buffer, light, color, FLUID, 1F, 1F, u, v));
        }

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int name, int light) {
        col.submitCustomGeometry(
                ps, baseType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public boolean frame;
        public double anim;
        public int fluidColor;
        public float fluidU;
        public float fluidV;
    }
}
