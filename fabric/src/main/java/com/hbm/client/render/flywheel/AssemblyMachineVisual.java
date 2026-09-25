// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.RenderAssemblyMachine;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineAssemblyMachine;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class AssemblyMachineVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineAssemblyMachine>
        implements ShaderLightVisual {
    private static final float BASE_YAW = 90F;
    private static final int FRAME = ResourceManager.assembly_machine.partId("Frame");
    private static final int RING = ResourceManager.assembly_machine.partId("Ring");
    private static final int ARM_LOWER1 = ResourceManager.assembly_machine.partId("ArmLower1");
    private static final int ARM_UPPER1 = ResourceManager.assembly_machine.partId("ArmUpper1");
    private static final int HEAD1 = ResourceManager.assembly_machine.partId("Head1");
    private static final int SPIKE1 = ResourceManager.assembly_machine.partId("Spike1");
    private static final int ARM_LOWER2 = ResourceManager.assembly_machine.partId("ArmLower2");
    private static final int ARM_UPPER2 = ResourceManager.assembly_machine.partId("ArmUpper2");
    private static final int HEAD2 = ResourceManager.assembly_machine.partId("Head2");
    private static final int SPIKE2 = ResourceManager.assembly_machine.partId("Spike2");
    private static final Material MATERIAL =
            MeshPart.litCutout(ResourceManager.assembly_machine_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[FRAME],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[RING],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[ARM_LOWER1],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[ARM_UPPER1],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[HEAD1],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[SPIKE1],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[ARM_LOWER2],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[ARM_UPPER2],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[HEAD2],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.assembly_machine.groups[SPIKE2],
                ResourceManager.assembly_machine.smoothing(),
                MATERIAL)
    };
    private final TransformedInstance[] instances;
    private final AABB bodyBounds;
    private final Matrix4f[] local = {
        new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f(),
        new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
    };
    private final Matrix4f world = new Matrix4f();
    private final double[] arm1 = new double[4];
    private final double[] arm2 = new double[4];
    private final double[] lastArm1 = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    private final double[] lastArm2 = {Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    private double lastRing = Double.NaN;
    private boolean lastFrame;
    private boolean initialized;
    private final float yaw;
    private final WorldItem preview = new WorldItem(visualizationContext, level, pos);
    private final PoseStack previewPoses = new PoseStack();

    public AssemblyMachineVisual(
            VisualizationContext context,
            BlockEntityMachineAssemblyMachine blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        yaw = BASE_YAW + Facing.yaw(BlockMultiblockCore.coreFacing(blockEntity.getBlockState()), 0);
        Matrix4f bodyLocal = new Matrix4f().translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.assembly_machine, "Base", bodyLocal, pos);
        local[0].set(bodyLocal);
        instances = new TransformedInstance[PARTS.length];
        for (int i = 0; i < PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
        updatePreview(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityMachineAssemblyMachine be) {
        return !WorldItem.drawsFramed(RenderAssemblyMachine.previewStack(be));
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
        ItemStack stack = RenderAssemblyMachine.previewStack(blockEntity);
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
        blockEntity.arms[0].getPositions(partialTick, arm1);
        blockEntity.arms[1].getPositions(partialTick, arm2);
        boolean ringChanged = !initialized || ring != lastRing;
        boolean arm1Changed = !initialized || !same(arm1, lastArm1);
        boolean arm2Changed = !initialized || !same(arm2, lastArm2);
        boolean frame = blockEntity.frame;
        if (!initialized || frame != lastFrame) {
            write(0, local[0], frame);
            lastFrame = frame;
        }
        if (ringChanged || arm1Changed) {
            local[1].set(local[0]).rotateY((float) ring * Mth.DEG_TO_RAD);
            armPoses(local[1], arm1, 2, false);
            for (int i = 1; i < 6; i++) write(i, local[i], true);
            lastRing = ring;
        }
        if (ringChanged || arm2Changed) {
            armPoses(local[1], arm2, 6, true);
            for (int i = 6; i < instances.length; i++) write(i, local[i], true);
        }
        System.arraycopy(arm1, 0, lastArm1, 0, arm1.length);
        System.arraycopy(arm2, 0, lastArm2, 0, arm2.length);
        initialized = true;
    }

    private void armPoses(Matrix4f start, double[] arm, int first, boolean second) {
        float sign = second ? -1F : 1F;
        float pivot = second ? -.9375F : .9375F;
        local[first]
                .set(start)
                .translate(0F, 1.625F, pivot)
                .rotateX((float) arm[0] * sign * Mth.DEG_TO_RAD)
                .translate(0F, -1.625F, -pivot);
        local[first + 1]
                .set(local[first])
                .translate(0F, 2.375F, pivot)
                .rotateX((float) arm[1] * sign * Mth.DEG_TO_RAD)
                .translate(0F, -2.375F, -pivot);
        float headPivot = second ? -.4375F : .4375F;
        local[first + 2]
                .set(local[first + 1])
                .translate(0F, 2.375F, headPivot)
                .rotateX((float) arm[2] * sign * Mth.DEG_TO_RAD)
                .translate(0F, -2.375F, -headPivot);
        local[first + 3].set(local[first + 2]).translate(0F, (float) arm[3], 0F);
    }

    private void write(int index, Matrix4f pose, boolean visible) {
        TransformedInstance instance = instances[index];
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(world).light(0).setChanged();
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
