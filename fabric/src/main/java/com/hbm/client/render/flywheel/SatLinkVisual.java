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

import com.hbm.blockentity.machine.SatelliteLinkerBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SatLinkVisual
        extends AbstractBoundedBlockEntityVisual<SatelliteLinkerBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {

    private static final MeshPart ROTOR =
            MeshPart.obj(
                    ResourceManager.satlink.groups[ResourceManager.satlink.partId("Rotor")],
                    ResourceManager.satlink.smoothing(),
                    MeshPart.litCutout(ResourceManager.satlink_tex));
    private static final MeshPart DISH =
            MeshPart.obj(
                    ResourceManager.satlink.groups[ResourceManager.satlink.partId("Dish")],
                    ResourceManager.satlink.smoothing(),
                    MeshPart.litCutout(ResourceManager.satlink_tex));

    private final TransformedInstance rotor;
    private final TransformedInstance dish;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f rotorPose = new Matrix4f();
    private final Matrix4f dishPose = new Matrix4f();
    private final Matrix4f worldPose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB originBounds;
    private @Nullable AABB lastLightBounds;
    private float lastRot = Float.NaN;
    private float lastLift = Float.NaN;

    public SatLinkVisual(
            VisualizationContext context,
            SatelliteLinkerBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockEntity.getBlockState().getValue(MultiblockCoreBlock.FACING);
        Direction lateral = facing.getCounterClockWise();
        basePose.translation(.5F, 0F, .5F)
                .rotateY(180F * Mth.DEG_TO_RAD)
                .translate(
                        (facing.getStepX() + lateral.getStepX()) * .5F,
                        0F,
                        (facing.getStepZ() + lateral.getStepZ()) * .5F);
        originBounds =
                new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        rotor =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROTOR.model())
                        .createInstance();
        dish =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, DISH.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    private void updateMovingParts(float partialTick) {
        var rot = blockEntity.rotation(partialTick);
        var lift = blockEntity.elevation(partialTick);
        if (rot == lastRot && lift == lastLift && lastLightBounds != null) return;
        lastRot = rot;
        lastLift = lift;

        rotorPose.set(basePose).rotateY(rot * Mth.DEG_TO_RAD);
        dishPose.set(rotorPose)
                .translate(0F, 7.375F, 0F)
                .rotateZ(lift * Mth.DEG_TO_RAD)
                .translate(0F, -7.375F, 0F);
        worldPose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(rotorPose);
        rotor.setTransform(worldPose).light(0).setChanged();
        worldPose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(dishPose);
        dish.setTransform(worldPose).light(0).setChanged();

        LightBounds.resetBounds(lightBoundsAccumulator, originBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, ROTOR.model(), rotorPose, pos);
        LightBounds.includeLightBounds(lightBoundsAccumulator, DISH.model(), dishPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                pos.getX() - 2,
                pos.getY(),
                pos.getZ() - 2,
                pos.getX() + 3,
                pos.getY() + 10,
                pos.getZ() + 3);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotor);
        consumer.accept(dish);
    }

    @Override
    protected void _delete() {
        rotor.delete();
        dish.delete();
    }
}
