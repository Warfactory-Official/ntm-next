// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.RenderAssemblyMachine;
import com.hbm.client.render.RenderPrecAss;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachinePrecAss;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class PrecAssVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachinePrecAss>
        implements ShaderLightVisual {
    private static final int COUNT = 4;
    private static final int FRAME = 0;
    private static final int RING = 1;
    private static final int RING2 = 2;
    private static final float BASE_YAW = 90F;
    private static final HFRWavefrontObject MODEL = ResourceManager.assembly_machine;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.precass_tex);
    private static final int GROUP_1 = MODEL.partId("Frame");
    private static final int GROUP_2 = MODEL.partId("Ring");
    private static final int GROUP_3 = MODEL.partId("Ring2");
    private static final int GROUP_4 = MODEL.partId("ArmLower1");
    private static final int GROUP_5 = MODEL.partId("ArmUpper1");
    private static final int GROUP_6 = MODEL.partId("Head1");
    private static final int GROUP_7 = MODEL.partId("Spike1");
    private static final MeshPart PART_1 =
            MeshPart.obj(MODEL.groups[GROUP_1], MODEL.smoothing(), MATERIAL);
    private static final MeshPart PART_2 =
            MeshPart.obj(MODEL.groups[GROUP_2], MODEL.smoothing(), MATERIAL);
    private static final MeshPart PART_3 =
            MeshPart.obj(MODEL.groups[GROUP_3], MODEL.smoothing(), MATERIAL);
    private static final MeshPart PART_4 =
            MeshPart.obj(MODEL.groups[GROUP_4], MODEL.smoothing(), MATERIAL);
    private static final MeshPart PART_5 =
            MeshPart.obj(MODEL.groups[GROUP_5], MODEL.smoothing(), MATERIAL);
    private static final MeshPart PART_6 =
            MeshPart.obj(MODEL.groups[GROUP_6], MODEL.smoothing(), MATERIAL);
    private static final MeshPart PART_7 =
            MeshPart.obj(MODEL.groups[GROUP_7], MODEL.smoothing(), MATERIAL);
    private final TransformedInstance[] instances;
    private final AABB bodyBounds;
    private final Matrix4f[] local = new Matrix4f[3 + COUNT * 4];
    private final Matrix4f armBase = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final double[][] arms = new double[COUNT][4];
    private final double[][] lastArms = new double[COUNT][4];
    private double lastRing = Double.NaN;
    private boolean lastFrame;
    private boolean initialized;
    private final float yaw;
    private final WorldItem preview = new WorldItem(visualizationContext, level, pos);
    private final PoseStack previewPoses = new PoseStack();

    public PrecAssVisual(
            VisualizationContext context,
            BlockEntityMachinePrecAss blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        yaw = BASE_YAW + Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 0);
        for (int i = 0; i < local.length; i++) local[i] = new Matrix4f();
        local[FRAME].translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Base", local[FRAME], pos);
        for (double[] arm : lastArms) Arrays.fill(arm, Double.NaN);
        MeshPart[] parts = new MeshPart[local.length];
        parts[FRAME] = PART_1;
        parts[RING] = PART_2;
        parts[RING2] = PART_3;
        for (int arm = 0; arm < COUNT; arm++) {
            int first = 3 + arm * 4;
            parts[first] = PART_4;
            parts[first + 1] = PART_5;
            parts[first + 2] = PART_6;
            parts[first + 3] = PART_7;
        }
        instances = new TransformedInstance[parts.length];
        for (int i = 0; i < parts.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, parts[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
        updatePreview(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityMachinePrecAss be) {
        return !WorldItem.drawsFramed(RenderPrecAss.previewStack(be));
    }

    private static boolean same(double[] first, double[] second) {
        for (int i = 0; i < first.length; i++) if (first[i] != second[i]) return false;
        return true;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
        updatePreview(context.partialTick());
    }

    private void updatePreview(float partialTick) {
        ItemStack stack = RenderPrecAss.previewStack(blockEntity);
        FramedItem.Arm arm = preview.setFramed(stack, level);
        if (arm == null) return;
        previewPoses.setIdentity();
        previewPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        previewPoses.translate(.5, 0, .5);
        previewPoses.mulPose(Axis.YP.rotationDegrees(yaw));
        RenderAssemblyMachine.previewPose(previewPoses, arm, stack.getItem() instanceof BlockItem);
        preview.write(previewPoses.last().pose(), partialTick);
    }

    public void updateMovingParts(float partialTick) {
        double ring = Mth.lerp(partialTick, blockEntity.prevRing, blockEntity.ring);
        for (int i = 0; i < COUNT; i++) blockEntity.armPositions(i, partialTick, arms[i]);
        boolean ringChanged = !initialized || ring != lastRing;
        boolean frame = blockEntity.frame;
        if (!initialized || frame != lastFrame) {
            instances[FRAME].setVisible(frame);
            if (frame) write(FRAME, local[FRAME]);
            lastFrame = frame;
        }
        if (ringChanged) {
            local[RING].set(local[FRAME]).rotateY((float) ring * Mth.DEG_TO_RAD);
            local[RING2].set(local[RING]);
            write(RING, local[RING]);
            write(RING2, local[RING2]);
            lastRing = ring;
        }
        for (int arm = 0; arm < COUNT; arm++) {
            if (!initialized || ringChanged || !same(arms[arm], lastArms[arm])) {
                armBase.set(local[RING]).rotateY(-90F * arm * Mth.DEG_TO_RAD);
                int first = 3 + arm * 4;
                armPoses(armBase, arms[arm], first);
                for (int i = first; i < first + 4; i++) write(i, local[i]);
            }
            System.arraycopy(arms[arm], 0, lastArms[arm], 0, arms[arm].length);
        }
        initialized = true;
    }

    private void armPoses(Matrix4f start, double[] arm, int first) {
        local[first]
                .set(start)
                .translate(0F, 1.625F, .9375F)
                .rotateX((float) arm[0] * Mth.DEG_TO_RAD)
                .translate(0F, -1.625F, -.9375F);
        local[first + 1]
                .set(local[first])
                .translate(0F, 2.375F, .9375F)
                .rotateX((float) arm[1] * Mth.DEG_TO_RAD)
                .translate(0F, -2.375F, -.9375F);
        local[first + 2]
                .set(local[first + 1])
                .translate(0F, 2.375F, .4375F)
                .rotateX((float) arm[2] * Mth.DEG_TO_RAD)
                .translate(0F, -2.375F, -.4375F);
        local[first + 3].set(local[first + 2]).translate(0F, (float) arm[3], 0F);
    }

    private void write(int index, Matrix4f pose) {
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instances[index].setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 3,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
        preview.delete();
    }
}
