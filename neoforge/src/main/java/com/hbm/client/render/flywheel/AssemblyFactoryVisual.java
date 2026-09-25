// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.RenderAssemblyFactory;
import com.hbm.client.render.RenderAssemblyMachine;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineAssemblyFactory;
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
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class AssemblyFactoryVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineAssemblyFactory>
        implements ShaderLightVisual {
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
    private final PartVisual[] parts;
    private final UvTransformedInstance firstSpark;
    private final UvTransformedInstance secondSpark;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB rawBodyBounds;
    private final double[] arm1 = new double[5];
    private final double[] arm2 = new double[5];
    private final double[] arm3 = new double[5];
    private final double[] arm4 = new double[5];
    private final double[] lastArm1 = new double[5];
    private final double[] lastArm2 = new double[5];
    private final double[] lastArm3 = new double[5];
    private final double[] lastArm4 = new double[5];
    private final Matrix4f armPose = new Matrix4f();
    private final Matrix4f sparkPose = new Matrix4f();
    private double lastSlide1 = Double.NaN;
    private double lastSlide2 = Double.NaN;
    private float lastSparkU = Float.NaN;
    private boolean lastSparkOneVisible;
    private boolean lastSparkTwoVisible;
    private boolean lastFrame;
    private boolean initialized;
    private final float yaw;
    private final WorldItem[] previews = new WorldItem[BlockEntityMachineAssemblyFactory.MODULES];
    private final PoseStack previewPoses = new PoseStack();

    public AssemblyFactoryVisual(
            VisualizationContext context,
            BlockEntityMachineAssemblyFactory blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        var facing = BlockMultiblockCore.coreFacing(blockEntity.getBlockState());
        var rawBodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(Facing.yaw(facing, 90) * Mth.DEG_TO_RAD);
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Base", rawBodyLocal, pos));
        yaw = BASE_YAW + Facing.yaw(facing, 0);
        rootPose.translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        parts = new PartVisual[STATIC_PARTS.length];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new PartVisual(STATIC_PARTS[i]);
        }
        firstSpark =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, FIRST_SPARK_MODEL)
                        .createInstance();
        secondSpark =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, SECOND_SPARK_MODEL)
                        .createInstance();
        for (int i = 0; i < previews.length; i++)
            previews[i] = new WorldItem(visualizationContext, level, pos);
        updateFrame(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityMachineAssemblyFactory be) {
        boolean inRange = RenderAssemblyMachine.inPreviewRange(be.getBlockPos());
        for (int i = 0; i < BlockEntityMachineAssemblyFactory.MODULES; i++) {
            if (!WorldItem.drawsFramed(RenderAssemblyFactory.previewStack(be, i, inRange)))
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

    private static boolean same(double[] first, double[] second) {
        for (int i = 0; i < first.length; i++) if (first[i] != second[i]) return false;
        return true;
    }

    private static void copy(double[] source, double[] target) {
        System.arraycopy(source, 0, target, 0, source.length);
    }

    @Override
    protected void frame(Context context) {
        updateFrame(context.partialTick());
    }

    private void updateFrame(float partialTick) {
        boolean inRange = RenderAssemblyMachine.inPreviewRange(pos);
        writeFrame(partialTick, inRange);
        for (int i = 0; i < previews.length; i++) {
            ItemStack stack = RenderAssemblyFactory.previewStack(blockEntity, i, inRange);
            FramedItem.Arm arm = previews[i].setFramed(stack, level);
            if (arm == null) continue;
            previewPoses.setIdentity();
            previewPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
            previewPoses.translate(.5, 0, .5);
            previewPoses.mulPose(Axis.YP.rotationDegrees(yaw));
            RenderAssemblyFactory.previewPose(
                    previewPoses, i, arm, stack.getItem() instanceof BlockItem);
            previews[i].write(previewPoses.last().pose(), partialTick);
        }
    }

    private void writeFrame(float partialTick, boolean inRange) {
        double slide1 = blockEntity.carriages[0].getSlider(partialTick);
        double slide2 = blockEntity.carriages[1].getSlider(partialTick);
        blockEntity.carriages[0].striker.getPositions(partialTick, arm1);
        blockEntity.carriages[0].saw.getPositions(partialTick, arm2);
        blockEntity.carriages[1].striker.getPositions(partialTick, arm3);
        blockEntity.carriages[1].saw.getPositions(partialTick, arm4);
        boolean sparkOneVisible = inRange && arm2[3] <= SAW_DOWN;
        boolean sparkTwoVisible = inRange && arm4[3] <= SAW_DOWN;
        float sparkU = (float) ((blockEntity.getLevel().getGameTime() / 10D + partialTick) % 10D);
        boolean frameChanged = !initialized || blockEntity.frame != lastFrame;
        boolean firstChanged =
                !initialized
                        || slide1 != lastSlide1
                        || !same(arm1, lastArm1)
                        || !same(arm2, lastArm2);
        boolean secondChanged =
                !initialized
                        || slide2 != lastSlide2
                        || !same(arm3, lastArm3)
                        || !same(arm4, lastArm4);
        boolean uvChanged = sparkU != lastSparkU;
        boolean sparkOneChanged =
                firstChanged
                        || sparkOneVisible != lastSparkOneVisible
                        || sparkOneVisible && uvChanged;
        boolean sparkTwoChanged =
                secondChanged
                        || sparkTwoVisible != lastSparkTwoVisible
                        || sparkTwoVisible && uvChanged;
        if (frameChanged) {
            lastFrame = blockEntity.frame;
            parts[0].write(blockEntity.frame, rootPose);
        }
        if (firstChanged) {
            lastSlide1 = slide1;
            copy(arm1, lastArm1);
            copy(arm2, lastArm2);
            armPoses(.5D - slide1, arm1, 1, true, false);
            armPoses(-.5D + slide1, arm2, 6, false, true);
        }
        if (secondChanged) {
            lastSlide2 = slide2;
            copy(arm3, lastArm3);
            copy(arm4, lastArm4);
            armPoses(-.5D + slide2, arm3, 12, false, false);
            armPoses(.5D - slide2, arm4, 17, true, true);
        }
        if (sparkOneChanged) {
            lastSparkOneVisible = sparkOneVisible;
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
            secondSpark.setVisible(sparkTwoVisible);
            if (sparkTwoVisible) {
                sparkPose
                        .set(rootPose)
                        .translate((float) (-.5D - slide2), 1.0625F, (float) (arm4[2] / 45D));
                writeSpark(secondSpark, sparkPose, sparkU);
            }
        }
        if (sparkOneChanged || sparkTwoChanged) lastSparkU = sparkU;
        initialized = true;
    }

    private void writeSpark(UvTransformedInstance instance, Matrix4f local, float u) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.uvRegion(u, 0F, 1F, 1F)
                .setTransform(instancePose)
                .light(LightCoordsUtil.FULL_BRIGHT)
                .setChanged();
    }

    private void armPoses(double slider, double[] arm, int first, boolean negative, boolean blade) {
        Matrix4f pose = armPose.set(rootPose).translate((float) slider, 0F, 0F);
        parts[first].write(true, pose);
        float sign = negative ? -1F : 1F;
        pose.translate(0F, 1.625F, sign * .9375F)
                .rotateX(sign * (float) arm[0] * Mth.DEG_TO_RAD)
                .translate(0F, -1.625F, -sign * .9375F);
        parts[first + 1].write(true, pose);
        pose.translate(0F, 2.375F, sign * .9375F)
                .rotateX(sign * (float) arm[1] * Mth.DEG_TO_RAD)
                .translate(0F, -2.375F, -sign * .9375F);
        parts[first + 2].write(true, pose);
        pose.translate(0F, 2.375F, sign * .4375F)
                .rotateX(sign * (float) arm[2] * Mth.DEG_TO_RAD)
                .translate(0F, -2.375F, -sign * .4375F);
        parts[first + 3].write(true, pose);
        pose.translate(0F, (float) arm[3], 0F);
        parts[first + 4].write(true, pose);
        if (!blade) return;
        pose.translate(0F, 1.625F, negative ? -.3125F : .3125F)
                .rotateX((negative ? 1F : -1F) * (float) arm[4] * Mth.DEG_TO_RAD)
                .translate(0F, -1.625F, negative ? .3125F : -.3125F);
        parts[first + 5].write(true, pose);
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
        for (var part : parts) part.collect(consumer);
    }

    @Override
    protected void _delete() {
        for (var part : parts) part.delete();
        firstSpark.delete();
        secondSpark.delete();
        for (WorldItem preview : previews) preview.delete();
    }

    private final class PartVisual {
        private final TransformedInstance instance;

        private PartVisual(MeshPart part) {
            instance =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, part.model())
                            .createInstance();
        }

        private void write(boolean shown, Matrix4f local) {
            instance.setVisible(shown);
            if (!shown) return;
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(local);
            instance.setTransform(instancePose).light(0).setChanged();
        }

        private void collect(Consumer<Instance> consumer) {
            consumer.accept(instance);
        }

        private void delete() {
            instance.delete();
        }
    }
}
