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

import com.hbm.blockentity.machine.fusion.FusionMHDTBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FusionMHDTVisual extends AbstractBoundedBlockEntityVisual<FusionMHDTBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final MeshPart COILS_PART =
            MeshPart.obj(
                    ResourceManager.fusion_mhdt.groups[ResourceManager.fusion_mhdt.partId("Coils")],
                    ResourceManager.fusion_mhdt.smoothing(),
                    MeshPart.litCutout(ResourceManager.fusion_mhdt_tex));

    private final TransformedInstance coils;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB rawBodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastRotor = Float.NaN;

    public FusionMHDTVisual(
            VisualizationContext context, FusionMHDTBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.fusion_mhdt, "Turbine", basePose, pos));
        coils =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, COILS_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float rotor = Mth.lerp(partialTick, blockEntity.prevRotor, blockEntity.rotor) % 15F;
        if (rotor == lastRotor) return;
        localPose
                .set(basePose)
                .translate(0F, 1.5F, 0F)
                .rotateX((rotor) * Mth.DEG_TO_RAD)
                .translate(0F, -1.5F, 0F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        coils.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBounds, rawBodyBounds);
        LightBounds.includeLightBounds(lightBounds, COILS_PART.model(), localPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastRotor = rotor;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).inflate(7).expandTowards(0, 4, 0).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(coils);
    }

    @Override
    protected void _delete() {
        coils.delete();
    }
}
