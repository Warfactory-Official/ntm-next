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

import com.hbm.blockentity.machine.HeatBoilerBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class HeatBoilerVisual extends AbstractBoundedBlockEntityVisual<HeatBoilerBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final Material BODY_MATERIAL = MeshPart.litCutout(ResourceManager.boiler_tex);
    private static final Material BURST_MATERIAL =
            SimpleMaterial.builderOf(BODY_MATERIAL).backfaceCulling(false).build();
    private static final MeshPart BODY_PART =
            MeshPart.obj(
                    ResourceManager.boiler.groups[ResourceManager.boiler.partId("Plane")],
                    ResourceManager.boiler.smoothing(),
                    BODY_MATERIAL);
    private static final MeshPart BURST_PART =
            MeshPart.obj(
                    ResourceManager.boiler_burst
                            .groups[ResourceManager.boiler_burst.partId("Plane")],
                    ResourceManager.boiler_burst.smoothing(),
                    BURST_MATERIAL);
    private final TransformedInstance body;
    private final TransformedInstance burst;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB anchorBounds;
    private boolean lastExploded;
    private boolean lastBreathing;
    private float lastScale = Float.NaN;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public HeatBoilerVisual(
            VisualizationContext context, HeatBoilerBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        anchorBounds = new AABB(pos);
        body =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BODY_PART.model())
                        .createInstance();
        burst =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BURST_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float yaw = Facing.yaw(MultiblockCoreBlock.coreFacing(blockEntity.getBlockState()), 180);
        boolean exploded = blockEntity.hasExploded;
        FluidTankNTM steam = blockEntity.tanks[1];
        boolean breathing = !exploded && steam.getFill() > steam.getMaxFill() * .9;
        float scale = 1F;
        if (breathing) {
            double sine = Math.sin(GameTime.now() / 50D % (Math.PI * 2D)) * .01D;
            scale = (float) (1D - sine);
        }
        if (initialized
                && exploded == lastExploded
                && breathing == lastBreathing
                && Float.compare(scale, lastScale) == 0) return;
        lastExploded = exploded;
        lastBreathing = breathing;
        lastScale = scale;
        initialized = true;
        localPose.identity().translate(.5F, 0F, .5F).rotateY((yaw) * Mth.DEG_TO_RAD);
        if (breathing) {
            localPose.scale(scale, (float) (2D - scale), scale);
        }
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        if (exploded) {
            body.setVisible(false);
            burst.setVisible(true);
            burst.setTransform(instancePose).light(0).setChanged();
        } else {
            burst.setVisible(false);
            body.setVisible(true);
            body.setTransform(instancePose).light(0).setChanged();
        }
        Model activeModel = exploded ? BURST_PART.model() : BODY_PART.model();
        LightBounds.resetBounds(lightBoundsAccumulator, anchorBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, activeModel, localPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                        pos.getX() - 1,
                        pos.getY(),
                        pos.getZ() - 1,
                        pos.getX() + 2,
                        pos.getY() + 4,
                        pos.getZ() + 2)
                .inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(body);
        consumer.accept(burst);
    }

    @Override
    protected void _delete() {
        body.delete();
        burst.delete();
    }
}
