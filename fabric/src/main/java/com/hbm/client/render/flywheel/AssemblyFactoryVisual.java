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

import com.hbm.blockentity.machine.AssemblyFactoryBlockEntity;
import com.hbm.blockentity.machine.JointedArm;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.render.AssemblyFactoryRenderer;
import com.hbm.client.render.AssemblyMachineRenderer;
import com.hbm.client.render.FramedItem;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public final class AssemblyFactoryVisual
        extends AbstractBoundedBlockEntityVisual<AssemblyFactoryBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
    private static final float BASE_YAW = 90F;
    private static final double SAW_DOWN = -.375D;
    private static final float SPARK_WIDE = .1875F;
    private static final float SPARK_NARROW = 0F;
    private static final float SPARK_LENGTH = 1.25F;
    private static final float SPARK_EPSILON = .01F;
    private static final int SPARK_FADED = 0x00FFFFFF;
    private static final int SPARK_SOLID = 0xFFFFFFFF;
    private static final Identifier SPARK_TEXTURE =
            Identifier.fromNamespaceAndPath(
                    "hbm", "textures/block/models/machines/assembly_factory_sparks.png");
    private static final String[] PART_NAMES = {
        "Frame",
        "Slider1",
        "ArmLower1",
        "ArmUpper1",
        "Head1",
        "Striker1",
        "Slider2",
        "ArmLower2",
        "ArmUpper2",
        "Head2",
        "Striker2",
        "Blade2",
        "Slider3",
        "ArmLower3",
        "ArmUpper3",
        "Head3",
        "Striker3",
        "Slider4",
        "ArmLower4",
        "ArmUpper4",
        "Head4",
        "Striker4",
        "Blade4"
    };
    private static final HFRWavefrontObject MODEL = ResourceManager.assembly_factory;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.assembly_factory_tex);
    private static final MeshPart[] STATIC_PARTS = buildParts();
    private static final Material SPARK_MATERIAL =
            SimpleMaterial.builder()
                    .texture(SPARK_TEXTURE)
                    .mipmap(false)
                    .shaders(StandardMaterialShaders.DEFAULT)
                    .cutout(CutoutShaders.TINY)
                    .light(LightShaders.NONE)
                    .useLight(false)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.LEQUAL)
                    .build();
    private static final Model FIRST_SPARK_MODEL = sparkModel(SPARK_MATERIAL, SPARK_LENGTH);
    private static final Model SECOND_SPARK_MODEL = sparkModel(SPARK_MATERIAL, -SPARK_LENGTH);
    private static final int[] GROUP_FIRST = {1, 6, 12, 17};
    private static final ChainInstance.Chain[] CHAINS = buildChains();
    private final ChainInstance[] parts;
    private final UvTransformedInstance firstSpark;
    private final UvTransformedInstance secondSpark;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB rawBodyBounds;
    private final float[][] previous = new float[4][6];
    private final float[][] current = new float[4][6];
    private final double[] arm2 = new double[5];
    private final double[] arm4 = new double[5];
    private final Matrix4f sparkPose = new Matrix4f();
    private double lastSlide1 = Double.NaN;
    private double lastSlide2 = Double.NaN;
    private double lastSaw1 = Double.NaN;
    private double lastSaw2 = Double.NaN;
    private float lastSparkU = Float.NaN;
    private boolean lastSparkOneVisible;
    private boolean lastSparkTwoVisible;
    private boolean frameShown;
    private final float yaw;
    private final WorldItem[] previews = new WorldItem[AssemblyFactoryBlockEntity.MODULES];
    private final PoseStack previewPoses = new PoseStack();

    public AssemblyFactoryVisual(
            VisualizationContext context,
            AssemblyFactoryBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        var facing = MultiblockCoreBlock.coreFacing(blockEntity.getBlockState());
        var rawBodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD);
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Base", rawBodyLocal, pos));
        yaw = BASE_YAW + Facing.yaw(facing, 0);
        rootPose.translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        base.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(rootPose);
        parts = new ChainInstance[STATIC_PARTS.length];
        for (int i = 0; i < parts.length; i++) {
            parts[i] =
                    instancerProvider()
                            .instancer(ChainInstance.TYPE, STATIC_PARTS[i].model())
                            .createInstance();
        }
        firstSpark =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, FIRST_SPARK_MODEL)
                        .createInstance();
        secondSpark =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, SECOND_SPARK_MODEL)
                        .createInstance();
        firstSpark.setVisible(false);
        secondSpark.setVisible(false);
        for (int i = 0; i < previews.length; i++)
            previews[i] = new WorldItem(visualizationContext, level, pos);
        frameShown = blockEntity.frame;
        for (int group = 0; group < GROUP_FIRST.length; group++) readChannels(group);
        for (int i = 0; i < parts.length; i++) write(i, i != 0 || frameShown);
        updateFrame(partialTick);
    }

    private static ChainInstance.Chain[] buildChains() {
        var chains = new ChainInstance.Chain[PART_NAMES.length];
        chains[0] = new ChainInstance.Chain();
        for (int group = 0; group < GROUP_FIRST.length; group++) {
            boolean negative = group == 0 || group == 3;
            boolean blade = group % 2 == 1;
            for (int part = 0; part < (blade ? 6 : 5); part++)
                chains[GROUP_FIRST[group] + part] = arm(negative, part);
        }
        return chains;
    }

    private static ChainInstance.Chain arm(boolean negative, int part) {
        float sign = negative ? -1F : 1F;
        var chain =
                new ChainInstance.Chain().translate(negative ? .5F : -.5F, 0F, 0F).slideX(0, sign);
        if (part > 0) {
            chain.translate(0F, 1.625F, sign * .9375F)
                    .rotateX(1, sign * Mth.DEG_TO_RAD)
                    .translate(0F, -1.625F, -sign * .9375F);
        }
        if (part > 1) {
            chain.translate(0F, 2.375F, sign * .9375F)
                    .rotateX(2, sign * Mth.DEG_TO_RAD)
                    .translate(0F, -2.375F, -sign * .9375F);
        }
        if (part > 2) {
            chain.translate(0F, 2.375F, sign * .4375F)
                    .rotateX(3, sign * Mth.DEG_TO_RAD)
                    .translate(0F, -2.375F, -sign * .4375F);
        }
        if (part > 3) chain.slideY(4, 1F);
        if (part > 4) {
            chain.translate(0F, 1.625F, negative ? -.3125F : .3125F)
                    .rotateX(5, -sign * Mth.DEG_TO_RAD)
                    .translate(0F, -1.625F, negative ? .3125F : -.3125F);
        }
        return chain.end();
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(AssemblyFactoryBlockEntity be) {
        boolean inRange = AssemblyMachineRenderer.inPreviewRange(be.getBlockPos());
        for (int i = 0; i < AssemblyFactoryBlockEntity.MODULES; i++) {
            if (!WorldItem.drawsFramed(AssemblyFactoryRenderer.previewStack(be, i, inRange)))
                return true;
        }
        return false;
    }

    private static MeshPart[] buildParts() {
        var parts = new MeshPart[PART_NAMES.length];
        for (int i = 0; i < parts.length; i++)
            parts[i] =
                    MeshPart.obj(
                            MODEL.groups[MODEL.partId(PART_NAMES[i])],
                            MODEL.smoothing(),
                            BODY_MATERIAL);
        return parts;
    }

    private static Model sparkModel(Material material, float length) {
        float[] vertices = new float[8 * 8];
        int[] colors = new int[8];
        int[] lights = new int[8];
        spark(
                vertices,
                colors,
                lights,
                0,
                -SPARK_EPSILON,
                -SPARK_WIDE,
                length,
                .5F,
                0F,
                SPARK_FADED);
        spark(
                vertices,
                colors,
                lights,
                1,
                -SPARK_EPSILON,
                SPARK_WIDE,
                length,
                .5F,
                1F,
                SPARK_FADED);
        spark(vertices, colors, lights, 2, -SPARK_EPSILON, SPARK_NARROW, 0F, 1.5F, 1F, SPARK_SOLID);
        spark(
                vertices,
                colors,
                lights,
                3,
                -SPARK_EPSILON,
                -SPARK_NARROW,
                0F,
                1.5F,
                0F,
                SPARK_SOLID);
        spark(vertices, colors, lights, 4, SPARK_EPSILON, -SPARK_WIDE, length, 0F, 1F, SPARK_FADED);
        spark(vertices, colors, lights, 5, SPARK_EPSILON, SPARK_WIDE, length, 0F, 0F, SPARK_FADED);
        spark(vertices, colors, lights, 6, SPARK_EPSILON, SPARK_NARROW, 0F, 1F, 0F, SPARK_SOLID);
        spark(vertices, colors, lights, 7, SPARK_EPSILON, -SPARK_NARROW, 0F, 1F, 1F, SPARK_SOLID);
        return new SingleMeshModel(PackedQuadMesh.of(vertices, colors, lights), material);
    }

    private static void spark(
            float[] vertices,
            int[] colors,
            int[] lights,
            int index,
            float x,
            float y,
            float z,
            float u,
            float v,
            int color) {
        int at = index * 8;
        vertices[at] = x;
        vertices[at + 1] = y;
        vertices[at + 2] = z;
        vertices[at + 3] = u;
        vertices[at + 4] = v;
        vertices[at + 5] = x < 0F ? -1F : 1F;
        vertices[at + 6] = 0F;
        vertices[at + 7] = 0F;
        colors[index] = color;
        lights[index] = LightCoordsUtil.FULL_BRIGHT;
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updateFrame(context.partialTick());
    }

    @Override
    public void tick(TickableVisual.Context context) {
        boolean frame = blockEntity.frame;
        if (frame != frameShown) {
            frameShown = frame;
            write(0, frame);
        }
        for (int group = 0; group < GROUP_FIRST.length; group++) {
            if (!readChannels(group)) continue;
            for (int part = 0; part < (group % 2 == 1 ? 6 : 5); part++)
                write(GROUP_FIRST[group] + part, true);
        }
    }

    private boolean readChannels(int group) {
        var carriage = blockEntity.carriages[group / 2];
        JointedArm arm = group % 2 == 0 ? carriage.striker : carriage.saw;
        return set(previous[group], (float) carriage.prevSlider, arm.prevAngles)
                | set(current[group], (float) carriage.slider, arm.angles);
    }

    private static boolean set(float[] channels, float slider, double[] angles) {
        boolean changed = channels[0] != slider;
        channels[0] = slider;
        for (int i = 0; i < 5; i++) {
            float angle = (float) angles[i];
            changed |= channels[i + 1] != angle;
            channels[i + 1] = angle;
        }
        return changed;
    }

    private void write(int index, boolean visible) {
        ChainInstance instance = parts[index];
        instance.setVisible(visible);
        if (!visible) return;
        int group = GROUP_FIRST.length - 1;
        while (group > 0 && index < GROUP_FIRST[group]) group--;
        instance.chain(CHAINS[index]).channels(previous[group], current[group]);
        instance.setTransform(base);
        instance.light(0).setChanged();
    }

    private void updateFrame(float partialTick) {
        boolean inRange = AssemblyMachineRenderer.inPreviewRange(pos);
        writeSparks(partialTick, inRange);
        for (int i = 0; i < previews.length; i++) {
            ItemStack stack = AssemblyFactoryRenderer.previewStack(blockEntity, i, inRange);
            FramedItem.Arm arm = previews[i].setFramed(stack, level);
            if (arm == null) continue;
            previewPoses.setIdentity();
            previewPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
            previewPoses.translate(.5, 0, .5);
            previewPoses.mulPose((Quaternionfc) Axis.YP.rotationDegrees(yaw));
            AssemblyFactoryRenderer.previewPose(
                    previewPoses, i, arm, stack.getItem() instanceof BlockItem);
            previews[i].write(previewPoses.last().pose(), partialTick);
        }
    }

    private void writeSparks(float partialTick, boolean inRange) {
        double slide1 = blockEntity.carriages[0].getSlider(partialTick);
        double slide2 = blockEntity.carriages[1].getSlider(partialTick);
        blockEntity.carriages[0].saw.getPositions(partialTick, arm2);
        blockEntity.carriages[1].saw.getPositions(partialTick, arm4);
        boolean sparkOneVisible = inRange && arm2[3] <= SAW_DOWN;
        boolean sparkTwoVisible = inRange && arm4[3] <= SAW_DOWN;
        float sparkU = (float) ((blockEntity.getLevel().getGameTime() / 10D + partialTick) % 10D);
        boolean uvChanged = sparkU != lastSparkU;
        boolean sparkOneChanged =
                sparkOneVisible != lastSparkOneVisible
                        || sparkOneVisible
                                && (uvChanged || slide1 != lastSlide1 || arm2[2] != lastSaw1);
        boolean sparkTwoChanged =
                sparkTwoVisible != lastSparkTwoVisible
                        || sparkTwoVisible
                                && (uvChanged || slide2 != lastSlide2 || arm4[2] != lastSaw2);
        if (sparkOneChanged) {
            lastSparkOneVisible = sparkOneVisible;
            lastSlide1 = slide1;
            lastSaw1 = arm2[2];
            firstSpark.setVisible(sparkOneVisible);
            if (sparkOneVisible) {
                sparkPose
                        .set(rootPose)
                        .translate((float) (.5D + slide1), 1.0625F, (float) (-arm2[2] / 45D));
                writeSpark(firstSpark, sparkPose, sparkU);
            }
        }
        if (sparkTwoChanged) {
            lastSparkTwoVisible = sparkTwoVisible;
            lastSlide2 = slide2;
            lastSaw2 = arm4[2];
            secondSpark.setVisible(sparkTwoVisible);
            if (sparkTwoVisible) {
                sparkPose
                        .set(rootPose)
                        .translate((float) (-.5D - slide2), 1.0625F, (float) (arm4[2] / 45D));
                writeSpark(secondSpark, sparkPose, sparkU);
            }
        }
        if (sparkOneChanged || sparkTwoChanged) lastSparkU = sparkU;
    }

    private void writeSpark(UvTransformedInstance instance, Matrix4f local, float u) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.uvRegion(u, 0F, 1F, 1F)
                .setTransform(instancePose)
                .light(LightCoordsUtil.FULL_BRIGHT)
                .setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 2,
                                pos.getY(),
                                pos.getZ() - 2,
                                pos.getX() + 3,
                                pos.getY() + 3,
                                pos.getZ() + 3)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (ChainInstance part : parts) consumer.accept(part);
    }

    @Override
    protected void _delete() {
        for (var part : parts) part.delete();
        firstSpark.delete();
        secondSpark.delete();
        for (WorldItem preview : previews) preview.delete();
    }
}
