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

import com.hbm.blockentity.machine.storage.FileCabinetBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FileCabinetVisual
        extends AbstractBoundedBlockEntityVisual<FileCabinetBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final float DRAWER_SLIDE = .6875F;
    private static final HFRWavefrontObject MODEL = ResourceManager.file_cabinet;
    private static final MeshPart[][] PARTS = {
        parts(MeshPart.litCutout(ResourceManager.file_cabinet_tex)),
        parts(MeshPart.litCutout(ResourceManager.file_cabinet_steel_tex))
    };
    private final MeshPart[] parts;
    private final AABB bodyBounds;
    private final TransformedInstance lower;
    private final TransformedInstance upper;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastLowerExtent = Float.NaN;
    private float lastUpperExtent = Float.NaN;

    public FileCabinetVisual(
            VisualizationContext context, FileCabinetBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        parts = PARTS[blockState.is(ModBlocks.FILING_CABINET_STEEL.get()) ? 1 : 0];
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(HorizontalDirectionalBlock.FACING), 180)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Cabinet", basePose, pos);
        lower =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, parts[0].model())
                        .createInstance();
        upper =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, parts[1].model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    private static MeshPart[] parts(Material material) {
        return new MeshPart[] {
            MeshPart.obj(MODEL.groups[MODEL.partId("LowerDrawer")], MODEL.smoothing(), material),
            MeshPart.obj(MODEL.groups[MODEL.partId("UpperDrawer")], MODEL.smoothing(), material)
        };
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float lowerExtent =
                blockEntity.prevLowerDrawer
                        + (blockEntity.lowerDrawer - blockEntity.prevLowerDrawer) * partialTick;
        float upperExtent =
                blockEntity.prevUpperDrawer
                        + (blockEntity.upperDrawer - blockEntity.prevUpperDrawer) * partialTick;
        if (lowerExtent == lastLowerExtent && upperExtent == lastUpperExtent) return;
        lastLowerExtent = lowerExtent;
        lastUpperExtent = upperExtent;

        localPose.set(basePose).translate(0F, 0F, DRAWER_SLIDE * lowerExtent);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        lower.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, parts[0].model(), localPose, pos);

        localPose.set(basePose).translate(0F, 0F, DRAWER_SLIDE * upperExtent);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        upper.setTransform(instancePose).light(0).setChanged();
        LightBounds.includeLightBounds(lightBoundsAccumulator, parts[1].model(), localPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(lower);
        consumer.accept(upper);
    }

    @Override
    protected void _delete() {
        lower.delete();
        upper.delete();
    }
}
