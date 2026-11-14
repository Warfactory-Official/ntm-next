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

import com.hbm.blockentity.machine.OreSlopperBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.render.OreSlopperRenderer;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public final class OreSlopperVisual extends AbstractBoundedBlockEntityVisual<OreSlopperBlockEntity>
        implements ShaderLightVisual, CulledDynamicVisual {
    private static final int SLIDER = 0,
            HYDRAULICS = 1,
            BUCKET = 2,
            BLADES_LEFT = 3,
            BLADES_RIGHT = 4,
            FAN = 5;
    private static final HFRWavefrontObject MODEL = ResourceManager.ore_slopper;
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.ore_slopper_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .backfaceCulling(true)
                    .build();
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[MODEL.partId("Slider")], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[MODEL.partId("Hydraulics")], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[MODEL.partId("Bucket")], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[MODEL.partId("BladesLeft")], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[MODEL.partId("BladesRight")], MODEL.smoothing(), MATERIAL),
        MeshPart.obj(MODEL.groups[MODEL.partId("Fan")], MODEL.smoothing(), MATERIAL)
    };
    private final AABB bodyBounds;
    private final TransformedInstance[] instances = new TransformedInstance[6];
    private final Matrix4f[] localPoses = new Matrix4f[6];
    private final Matrix4f instancePose = new Matrix4f();
    private final PoseStackBuilder poseBuilder = new PoseStackBuilder();
    private final float fixedYaw;
    private float lastSlide = Float.NaN, lastClamp1 = Float.NaN, lastClamp2 = Float.NaN;
    private float lastBlades = Float.NaN, lastFan = Float.NaN;
    private boolean initialized;
    private final WorldItem sample = new WorldItem(visualizationContext, level, pos);
    private final PoseStack samplePoses = new PoseStack();

    public OreSlopperVisual(
            VisualizationContext context, OreSlopperBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        fixedYaw = Facing.yaw(MultiblockCoreBlock.coreFacing(blockState), 0);
        bodyBounds =
                LightBounds.of(
                        MODEL,
                        "Base",
                        new Matrix4f().translation(.5F, 0F, .5F).rotateY(fixedYaw * Mth.DEG_TO_RAD),
                        pos);
        for (int i = 0; i < PARTS.length; i++) {
            localPoses[i] = new Matrix4f();
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        }
        writeFrame(partialTick);
        updateSample(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(OreSlopperBlockEntity be) {
        return be.animation == OreSlopperBlockEntity.SlopperAnimation.LIFTING
                && !WorldItem.draws(OreSlopperRenderer.sample(), ItemDisplayContext.NONE);
    }

    @Override
    public void frame(Context context) {
        writeFrame(context.partialTick());
        updateSample(context.partialTick());
    }

    private void updateSample(float partialTick) {
        boolean lifting = blockEntity.animation == OreSlopperBlockEntity.SlopperAnimation.LIFTING;
        sample.set(
                lifting ? OreSlopperRenderer.sample() : ItemStack.EMPTY, ItemDisplayContext.NONE);
        if (!lifting) return;
        samplePoses.setIdentity();
        samplePoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        samplePoses.mulPose(localPoses[BUCKET]);
        OreSlopperRenderer.samplePose(samplePoses);
        sample.write(samplePoses.last().pose(), partialTick);
    }

    private void writeFrame(float partialTick) {
        float slide = Mth.lerp(partialTick, blockEntity.prevSlider, blockEntity.slider);
        double extend = Mth.lerp(partialTick, blockEntity.prevBucket, blockEntity.bucket) * 1.5D;
        float clamp1 = (float) Mth.clamp(extend - 0.25D, 0D, 1.25D);
        float clamp2 = (float) Mth.clamp(extend, 0D, 1.25D);
        float blades = Mth.lerp(partialTick, blockEntity.prevBlades, blockEntity.blades);
        float fan = Mth.lerp(partialTick, blockEntity.prevFan, blockEntity.fan);
        boolean slideChanged =
                !initialized || slide != lastSlide || clamp1 != lastClamp1 || clamp2 != lastClamp2;
        boolean bladesChanged = !initialized || blades != lastBlades;
        boolean fanChanged = !initialized || fan != lastFan;
        if (!slideChanged && !bladesChanged && !fanChanged) return;
        PoseStackBuilder pose = poseBuilder;
        pose.poses.setIdentity();
        pose.poses.translate(.5D, 0D, .5D);
        pose.poses.rotate((Quaternionfc) Axis.YP.rotationDegrees(fixedYaw));
        pose.poses.pushPose();
        pose.poses.translate(0D, 0D, slide * -3D);
        pose.capture(SLIDER);
        pose.poses.translate(0D, -clamp1, 0D);
        pose.capture(HYDRAULICS);
        pose.poses.translate(0D, -clamp2, 0D);
        pose.capture(BUCKET);
        pose.poses.popPose();
        pose.rotate(.375D, 2.75D, 0D, blades, Axis.ZP, BLADES_LEFT);
        pose.rotate(-.375D, 2.75D, 0D, -blades, Axis.ZP, BLADES_RIGHT);
        pose.rotate(0D, 1.875D, -1D, -fan, Axis.XP, FAN);

        for (int i = 0; i < instances.length; i++) {
            if ((i <= BUCKET && !slideChanged)
                    || (i == BLADES_LEFT || i == BLADES_RIGHT) && !bladesChanged
                    || i == FAN && !fanChanged) continue;
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(localPoses[i]);
            instances[i].setTransform(instancePose).light(0).setChanged();
        }
        lastSlide = slide;
        lastClamp1 = clamp1;
        lastClamp2 = clamp2;
        lastBlades = blades;
        lastFan = fan;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                pos.getY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 7,
                                pos.getZ() + 4)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (var instance : instances) instance.delete();
        sample.delete();
    }

    private final class PoseStackBuilder {
        private final PoseStack poses = new PoseStack();

        private void capture(int part) {
            localPoses[part].set(poses.last().pose());
        }

        private void rotate(double x, double y, double z, float angle, Axis axis, int part) {
            poses.pushPose();
            poses.translate(x, y, z);
            poses.rotate((Quaternionfc) axis.rotationDegrees(angle));
            poses.translate(-x, -y, -z);
            capture(part);
            poses.popPose();
        }
    }
}
