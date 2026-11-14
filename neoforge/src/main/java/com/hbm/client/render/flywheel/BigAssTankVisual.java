/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render.flywheel;

import com.hbm.blockentity.machine.storage.BigAssTankBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.gui.FluidGauge;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class BigAssTankVisual extends AbstractBoundedBlockEntityVisual<BigAssTankBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final float PLANE_OFF = 5.9375F;
    private static final float PLANE_BOTTOM = 1.75F;
    private static final double PLANE_SPEED = 250D;
    private static final PackedQuadMesh FLUID_MESH = fluidMesh();
    private static final Model FALLBACK_FLUID =
            new SingleMeshModel(FLUID_MESH, fluidMaterial(ResourceManager.white_tex));
    private static final Map<Identifier, Model> FLUID_MODELS = new ConcurrentHashMap<>();
    private final HazardDiamondVisual hazards;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f fluidPose = new Matrix4f();
    private final Matrix4f hazardPose = new Matrix4f();
    private final Matrix4f hazardPlacement = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB rawBodyBounds;
    private ScrollInstance fluid;
    private Model fluidModel;
    private double lastHeight = Double.NaN;
    private float lastScroll = Float.NaN;
    private Fluid lastFluid;
    private NTMFluidProperty lastProperty;
    private boolean lastVisible;
    private boolean initialized;

    public BigAssTankVisual(
            VisualizationContext context, BigAssTankBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F);
        if (blockEntity.isTilted())
            basePose.translate(0F, -1F, 0F)
                    .rotateZ((10F) * Mth.DEG_TO_RAD)
                    .rotateY((5F) * Mth.DEG_TO_RAD);
        basePose.rotateY(
                (Facing.yaw(blockEntity.getBlockState().getValue(MultiblockCoreBlock.FACING), 270))
                        * Mth.DEG_TO_RAD);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.bigasstank, "Circle", basePose, pos));
        hazards = new HazardDiamondVisual(context, level, pos, 2);
        updateMovingParts(partialTick);
    }

    private static PackedQuadMesh fluidMesh() {
        float[] vertices = {
            -PLANE_OFF, 0F, -.25F, 0F, 0F, -1F, 0F, 0F,
            -PLANE_OFF, 1F, -.25F, 0F, 1F, -1F, 0F, 0F,
            -PLANE_OFF, 1F, .25F, 1F, 1F, -1F, 0F, 0F,
            -PLANE_OFF, 0F, .25F, 1F, 0F, -1F, 0F, 0F,
            PLANE_OFF, 0F, -.25F, 1F, 0F, 1F, 0F, 0F,
            PLANE_OFF, 1F, -.25F, 1F, 1F, 1F, 0F, 0F,
            PLANE_OFF, 1F, .25F, 0F, 1F, 1F, 0F, 0F,
            PLANE_OFF, 0F, .25F, 0F, 0F, 1F, 0F, 0F
        };
        return PackedQuadMesh.of(vertices, new int[] {-1, -1, -1, -1, -1, -1, -1, -1}, new int[8]);
    }

    private static Material fluidMaterial(Identifier texture) {
        return SimpleMaterial.builderOf(Materials.TRANSLUCENT)
                .texture(texture)
                .mipmap(false)
                .cutout(CutoutShaders.TINY)
                .light(LightShaders.SMOOTH)
                .useOverlay(false)
                .ambientOcclusion(false)
                .cardinalLightingMode(CardinalLightingMode.OFF)
                .transparency(Transparency.ORDER_INDEPENDENT)
                .writeMask(WriteMask.COLOR)
                .backfaceCulling(false)
                .build();
    }

    private static Model fluidModel(Identifier sheet) {
        return sheet == null
                ? FALLBACK_FLUID
                : FLUID_MODELS.computeIfAbsent(
                        sheet, texture -> new SingleMeshModel(FLUID_MESH, fluidMaterial(texture)));
    }

    public static void initModels() {}

    @Override
    public void frame(DynamicVisual.Context context) {
        updateMovingParts(context.partialTick());
    }

    @Override
    public void tick(TickableVisual.Context context) {
        scroll(false);
    }

    private void scroll(boolean fresh) {
        float from = (float) -(level.getGameTime() % PLANE_SPEED / PLANE_SPEED);
        if (fluid == null || !fresh && from == lastScroll) return;
        lastScroll = from;
        fluid.scroll(from, from - (float) (1D / PLANE_SPEED)).setChanged();
    }

    public void updateMovingParts(float partialTick) {
        Fluid type = blockEntity.tank.getTankType();
        var sheet = type == null ? null : FluidGauge.sheet(type);
        double height = blockEntity.tank.getFill() * 1.5D / blockEntity.tank.getMaxFill();
        boolean visible = sheet != null && height > 0D;
        NTMFluidProperty prop = type == null ? null : NTMFluidProperties.get(type);
        boolean poseChanged = !initialized || height != lastHeight || visible != lastVisible;
        boolean fluidChanged = !initialized || type != lastFluid;
        boolean hazardChanged = !initialized || prop != lastProperty;
        if (!poseChanged && !fluidChanged && !hazardChanged) return;
        if (fluidChanged) {
            Model model = fluidModel(sheet);
            if (model != fluidModel) {
                if (fluid != null) fluid.delete();
                fluidModel = model;
                fluid = instancerProvider().instancer(ScrollInstance.TYPE, model).createInstance();
                fluid.direction(1F, 0F);
                fluid.setVisible(false);
                scroll(true);
            }
        }
        if (poseChanged || fluidChanged) {
            lastHeight = height;
            lastVisible = visible;
            lastFluid = type;
            fluid.setVisible(visible);
            if (visible) {
                fluidPose
                        .set(basePose)
                        .translate(0F, PLANE_BOTTOM, 0F)
                        .scale(1F, (float) height, 1F);
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(fluidPose);
                fluid.setTransform(instancePose).light(0);
            }
        }
        if ((fluidChanged || poseChanged) && visible) {
            fluid.uvRegion(0F, 0F, .5F, (float) (-height * 2D * .5D)).setChanged();
        }

        if (hazardChanged) {
            lastProperty = prop;
            if (prop == null) {
                hazards.hide(0);
                hazards.hide(1);
                initialized = true;
                return;
            }
            hazardPose.set(basePose).rotateY((22.5F) * Mth.DEG_TO_RAD);
            for (int side = 0; side < 2; side++) {
                hazardPlacement.set(hazardPose);
                if (side == 1) hazardPlacement.rotateY((180F) * Mth.DEG_TO_RAD);
                hazardPlacement.translate(5.5F, 2F, 0F);
                hazards.update(
                        side,
                        hazardPlacement,
                        prop.nfpaHealth(),
                        prop.nfpaFlame(),
                        prop.nfpaReact(),
                        prop.symbol());
            }
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                        pos.getX() - 8,
                        pos.getY() - 3,
                        pos.getZ() - 8,
                        pos.getX() + 9,
                        pos.getY() + 6,
                        pos.getZ() + 9));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        fluid.delete();
        hazards.delete();
    }
}
