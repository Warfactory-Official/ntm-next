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

import com.hbm.blockentity.machine.IndustrialTurbineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class IndustrialTurbineVisual
        extends AbstractBoundedBlockEntityVisual<IndustrialTurbineBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final Material MATERIAL =
            MeshPart.litCutout(ResourceManager.industrial_turbine_tex);
    private static final MeshPart GAUGE_PART =
            MeshPart.obj(
                    ResourceManager.industrial_turbine
                            .groups[ResourceManager.industrial_turbine.partId("Gauge")],
                    ResourceManager.industrial_turbine.smoothing(),
                    MATERIAL);
    private static final MeshPart FLYWHEEL_PART =
            MeshPart.obj(
                    ResourceManager.industrial_turbine
                            .groups[ResourceManager.industrial_turbine.partId("Flywheel")],
                    ResourceManager.industrial_turbine.smoothing(),
                    MATERIAL);
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain()
                .translate(0F, 1.5F, 0F)
                .rotateZ(0, Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F)
                .end(),
        new ChainInstance.Chain()
                .translate(0F, 1.5F, 0F)
                .rotateZ(1, -Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F)
                .end()
    };

    private final ChainParts parts;
    private final AABB lightBounds;

    public IndustrialTurbineVisual(
            VisualizationContext context,
            IndustrialTurbineBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f basePose =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 180)
                                        * Mth.DEG_TO_RAD);
        parts =
                new ChainParts(
                        context,
                        visualPos,
                        basePose,
                        new Model[] {GAUGE_PART.model(), FLYWHEEL_PART.model()},
                        CHAINS,
                        2);
        AABB bodyBounds =
                LightBounds.of(ResourceManager.industrial_turbine, "Turbine", basePose, pos);
        double[] bounds = new double[6];
        LightBounds.resetBounds(
                bounds,
                bodyBounds.minmax(
                        new AABB(
                                        pos.getX() - 3,
                                        pos.getY(),
                                        pos.getZ() - 3,
                                        pos.getX() + 4,
                                        pos.getY() + 3,
                                        pos.getZ() + 4)
                                .inflate(1)));
        parts.includeLightBounds(bounds, pos);
        lightBounds = new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        updateGauge();
        updateRotor();
    }

    public static void initModels() {}

    private static int steamTier(@Nullable Fluid type) {
        if (type == NTMFluids.HOTSTEAM) return 1;
        if (type == NTMFluids.SUPERHOTSTEAM) return 2;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateGauge();
    }

    @Override
    public void tick(TickableVisual.Context context) {
        updateRotor();
    }

    private void updateGauge() {
        float gaugeAngle = 135F - steamTier(blockEntity.tanks[0].getTankType()) * 90F;
        parts.set(0, gaugeAngle, gaugeAngle);
        parts.write();
    }

    private void updateRotor() {
        parts.set(1, blockEntity.lastRotor, blockEntity.rotor);
        parts.write();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return lightBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        parts.collectCrumblingInstances(consumer);
    }

    @Override
    protected void _delete() {
        parts.delete();
    }
}
