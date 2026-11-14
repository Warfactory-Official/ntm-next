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

import com.hbm.blockentity.machine.SolarMirrorBlockEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.GroupObject;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SolarMirrorVisual
        extends AbstractBoundedBlockEntityVisual<SolarMirrorBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final Material MIRROR_MATERIAL =
            SimpleMaterial.builderOf(MeshPart.litCutout(ResourceManager.solar_mirror_tex))
                    .cutout(CutoutShaders.HALF)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(true)
                    .build();
    private static final MeshPart IDLE_PART = effectPart(false);
    private static final MeshPart AIMED_PART = effectPart(true);
    private final TransformedInstance aimed;
    private final TransformedInstance idle;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB rawBodyBounds;
    private int lastTargetX = Integer.MIN_VALUE;
    private int lastTargetY = Integer.MIN_VALUE;
    private int lastTargetZ = Integer.MIN_VALUE;
    private boolean lastAimed;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public SolarMirrorVisual(
            VisualizationContext context, SolarMirrorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal = new Matrix4f().translation(.5F, 0F, .5F);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.solar_mirror, "Base", rawBodyLocal, pos));
        idle =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, IDLE_PART.model())
                        .createInstance();
        aimed =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, AIMED_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    private static MeshPart effectPart(boolean aimed) {
        GroupObject group =
                ResourceManager.solar_mirror.groups[ResourceManager.solar_mirror.partId("Mirror")];
        return new MeshPart(new SingleMeshModel(mesh(group.quads(false), aimed), MIRROR_MATERIAL));
    }

    public static void initModels() {}

    private static PackedQuadMesh mesh(float[] data, boolean aimed) {
        int[] colors = new int[data.length / GroupObject.STRIDE];
        for (int face = 0; face < data.length; face += GroupObject.QUAD) {
            float nx = data[face + 5], ny = data[face + 6], nz = data[face + 7];
            float brightness =
                    aimed
                            ? (ny + 1F) * .65F
                            : (ny * .3F + .7F) - Math.abs(nx) * .1F + Math.abs(nz) * .1F;
            brightness = Math.max(.45F, Math.min(1F, brightness));
            int channel = (int) (brightness * 255F);
            Arrays.fill(
                    colors,
                    face / GroupObject.STRIDE,
                    face / GroupObject.STRIDE + 4,
                    ARGB.color(255, channel, channel, channel));
        }
        return PackedQuadMesh.of(data, colors, null);
    }

    @Override
    public void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int targetX = blockEntity.targetX();
        int targetY = blockEntity.targetY();
        int targetZ = blockEntity.targetZ();
        boolean isAimed = targetY > pos.getY();
        if (initialized
                && targetX == lastTargetX
                && targetY == lastTargetY
                && targetZ == lastTargetZ
                && isAimed == lastAimed) return;
        lastTargetX = targetX;
        lastTargetY = targetY;
        lastTargetZ = targetZ;
        lastAimed = isAimed;
        initialized = true;
        if (isAimed) {
            double dx = (double) targetX - pos.getX();
            double dy = (double) targetY - pos.getY();
            double dz = (double) targetZ - pos.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            float pitch = (float) (Math.toDegrees(-Math.asin(dy / distance)) + 90D);
            float yaw = (float) (Math.toDegrees(-Math.atan2(dz, dx)) + 180D);
            localPose
                    .identity()
                    .translate(.5F, 0F, .5F)
                    .translate(0F, 1F, 0F)
                    .rotateY((yaw) * Mth.DEG_TO_RAD)
                    .rotateZ((pitch) * Mth.DEG_TO_RAD)
                    .translate(0F, -1F, 0F);
        } else localPose.identity().translate(.5F, 0F, .5F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        TransformedInstance shown = isAimed ? aimed : idle;
        (isAimed ? idle : aimed).setVisible(false);
        shown.setVisible(true);
        shown.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator, (isAimed ? AIMED_PART : IDLE_PART).model(), localPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).expandTowards(0, 1, 0).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(aimed);
        consumer.accept(idle);
    }

    @Override
    protected void _delete() {
        aimed.delete();
        idle.delete();
    }
}
