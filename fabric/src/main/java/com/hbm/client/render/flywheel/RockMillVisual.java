// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineRockMill;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RockMillVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineRockMill>
        implements ShaderLightVisual {
    private static final int FRAME = ResourceManager.rock_mill.partId("Frame");
    private static final int WHEEL = ResourceManager.rock_mill.partId("Wheel");
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.rock_mill_tex);
    private static final MeshPart FRAME_PART =
            MeshPart.obj(
                    ResourceManager.rock_mill.groups[FRAME],
                    ResourceManager.rock_mill.smoothing(),
                    MATERIAL);
    private static final MeshPart WHEEL_PART =
            MeshPart.obj(
                    ResourceManager.rock_mill.groups[WHEEL],
                    ResourceManager.rock_mill.smoothing(),
                    MATERIAL);

    private final TransformedInstance frame;
    private final TransformedInstance wheel;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f worldPose = new Matrix4f();
    private final AABB bounds;
    private float lastRotation = Float.NaN;
    private boolean lastFrame;
    private boolean initialized;

    public RockMillVisual(
            VisualizationContext context,
            BlockEntityMachineRockMill blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        float yaw =
                90F + Facing.yaw(BlockMultiblockCore.coreFacing(blockEntity.getBlockState()), 0);
        basePose.translation(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        bounds =
                new AABB(
                        pos.getX() - 2,
                        pos.getY(),
                        pos.getZ() - 2,
                        pos.getX() + 3,
                        pos.getY() + 3,
                        pos.getZ() + 3);
        frame =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FRAME_PART.model())
                        .createInstance();
        wheel =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, WHEEL_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    private void updateMovingParts(float partialTick) {
        if (!initialized || blockEntity.frame != lastFrame) {
            frame.setVisible(blockEntity.frame);
            if (blockEntity.frame) write(frame, basePose);
            lastFrame = blockEntity.frame;
        }
        float rotation = Mth.lerp(partialTick, blockEntity.prevRotation, blockEntity.rotation);
        if (!initialized || rotation != lastRotation) {
            localPose.set(basePose).rotateY(-rotation * Mth.DEG_TO_RAD);
            write(wheel, localPose);
            lastRotation = rotation;
        }
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f pose) {
        worldPose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(worldPose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(frame);
        consumer.accept(wheel);
    }

    @Override
    protected void _delete() {
        frame.delete();
        wheel.delete();
    }
}
