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

import com.hbm.blockentity.machine.JointedArm;
import com.hbm.blockentity.processing.AssemblyMachineBlockEntity;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.render.AssemblyMachineRenderer;
import com.hbm.client.render.FramedItem;
import com.hbm.main.ResourceManager;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBoundedBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.CulledDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

public final class AssemblyMachineVisual
        extends AbstractBoundedBlockEntityVisual<AssemblyMachineBlockEntity>
        implements ShaderLightVisual, SimpleTickableVisual, CulledDynamicVisual {
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
    private static final ChainInstance.Chain[] CHAINS = {
        new ChainInstance.Chain(),
        new ChainInstance.Chain().rotateY(0, Mth.DEG_TO_RAD),
        arm(false, 1),
        arm(false, 2),
        arm(false, 3),
        arm(false, 4),
        arm(true, 1),
        arm(true, 2),
        arm(true, 3),
        arm(true, 4)
    };
    private final ChainInstance[] instances;
    private final AABB bodyBounds;
    private final float[][] previous = new float[2][5];
    private final float[][] current = new float[2][5];
    private boolean frameShown;
    private final float yaw;
    private final WorldItem preview = new WorldItem(visualizationContext, level, pos);
    private final PoseStack previewPoses = new PoseStack();

    public AssemblyMachineVisual(
            VisualizationContext context,
            AssemblyMachineBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        yaw = BASE_YAW + Facing.yaw(MultiblockCoreBlock.coreFacing(blockEntity.getBlockState()), 0);
        Matrix4f bodyLocal = new Matrix4f().translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.assembly_machine, "Base", bodyLocal, pos);
        var base =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(bodyLocal);
        instances = new ChainInstance[PARTS.length];
        for (int i = 0; i < PARTS.length; i++) {
            var instance =
                    instancerProvider()
                            .instancer(ChainInstance.TYPE, PARTS[i].model())
                            .createInstance();
            instance.chain(CHAINS[i]).setTransform(base).light(0);
            instances[i] = instance;
        }
        frameShown = blockEntity.frame;
        readChannels();
        for (int i = 0; i < PARTS.length; i++) write(i);
        instances[0].setVisible(frameShown);
        updatePreview(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(AssemblyMachineBlockEntity be) {
        return !WorldItem.drawsFramed(AssemblyMachineRenderer.previewStack(be));
    }

    private static ChainInstance.Chain arm(boolean second, int joints) {
        float sign = second ? -1F : 1F;
        float pivot = second ? -.9375F : .9375F;
        float headPivot = second ? -.4375F : .4375F;
        var chain =
                new ChainInstance.Chain()
                        .rotateY(0, Mth.DEG_TO_RAD)
                        .translate(0F, 1.625F, pivot)
                        .rotateX(1, sign * Mth.DEG_TO_RAD)
                        .translate(0F, -1.625F, -pivot);
        if (joints > 1) {
            chain.translate(0F, 2.375F, pivot)
                    .rotateX(2, sign * Mth.DEG_TO_RAD)
                    .translate(0F, -2.375F, -pivot);
        }
        if (joints > 2) {
            chain.translate(0F, 2.375F, headPivot)
                    .rotateX(3, sign * Mth.DEG_TO_RAD)
                    .translate(0F, -2.375F, -headPivot);
        }
        if (joints > 3) chain.slideY(4, 1F);
        return chain.end();
    }

    @Override
    public void frame(DynamicVisual.Context context) {
        updatePreview(context.partialTick());
    }

    @Override
    public void tick(TickableVisual.Context context) {
        int changed = readChannels();
        boolean frame = blockEntity.frame;
        if (frame != frameShown) {
            frameShown = frame;
            instances[0].setVisible(frame);
        }
        for (int i = 1; i < instances.length; i++) {
            int side = i < 6 ? 0 : 1;
            int joints = i == 1 ? 0 : i < 6 ? i - 1 : i - 5;
            int channels = ((1 << (joints + 1)) - 1) << (side * 5);
            if ((changed & channels) != 0) write(i);
        }
    }

    private int readChannels() {
        int changed = 0;
        for (int side = 0; side < 2; side++) {
            JointedArm arm = blockEntity.arms[side];
            int channels =
                    set(previous[side], (float) blockEntity.prevRing, arm.prevAngles)
                            | set(current[side], (float) blockEntity.ring, arm.angles);
            changed |= channels << (side * 5);
        }
        return changed;
    }

    private static int set(float[] channels, float ring, double[] angles) {
        int changed = channels[0] != ring ? 1 : 0;
        channels[0] = ring;
        for (int i = 0; i < 4; i++) {
            float angle = (float) angles[i];
            if (channels[i + 1] != angle) changed |= 1 << (i + 1);
            channels[i + 1] = angle;
        }
        return changed;
    }

    private void write(int index) {
        int side = index < 6 ? 0 : 1;
        instances[index].channels(previous[side], current[side]).setChanged();
    }

    private void updatePreview(float partialTick) {
        ItemStack stack = AssemblyMachineRenderer.previewStack(blockEntity);
        FramedItem.Arm arm = preview.setFramed(stack, level);
        if (arm == null) return;
        previewPoses.setIdentity();
        previewPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        previewPoses.translate(.5, 0, .5);
        previewPoses.rotate((Quaternionfc) Axis.YP.rotationDegrees(yaw));
        AssemblyMachineRenderer.previewPose(
                previewPoses, arm, stack.getItem() instanceof BlockItem);
        preview.write(previewPoses.last().pose(), partialTick);
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
        for (ChainInstance instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (ChainInstance instance : instances) instance.delete();
        preview.delete();
    }
}
