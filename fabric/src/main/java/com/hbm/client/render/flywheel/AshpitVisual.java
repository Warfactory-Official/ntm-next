// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityAshpit;
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

public final class AshpitVisual extends HbmDynamicBlockEntityVisual<BlockEntityAshpit>
        implements ShaderLightVisual {
    private static final int DOOR = ResourceManager.heater_oven.partId("Door");
    private static final int INNER_BURNING = ResourceManager.heater_oven.partId("InnerBurning");
    private static final int INNER = ResourceManager.heater_oven.partId("Inner");
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.ashpit_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(
                ResourceManager.heater_oven.groups[DOOR],
                ResourceManager.heater_oven.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.heater_oven.groups[INNER_BURNING],
                ResourceManager.heater_oven.smoothing(),
                MATERIAL),
        MeshPart.obj(
                ResourceManager.heater_oven.groups[INNER],
                ResourceManager.heater_oven.smoothing(),
                MATERIAL)
    };
    private final AABB bodyBounds;
    private final TransformedInstance[] instances;
    private final Matrix4f doorPose = new Matrix4f();
    private final Matrix4f bodyPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private float lastDoorAngle = Float.NaN;
    private boolean lastFull;
    private boolean initialized;

    public AshpitVisual(
            VisualizationContext context, BlockEntityAshpit blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        float facing = Facing.yaw(blockState.getValue(BlockMultiblockCore.FACING), 180);
        bodyPose.translation(.5F, 0F, .5F).rotateY((facing - 90F) * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.heater_oven, "Main", bodyPose, pos);
        instances = new TransformedInstance[PARTS.length];
        for (int i = 0; i < PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float doorAngle = Mth.lerp(partialTick, blockEntity.prevDoorAngle, blockEntity.doorAngle);
        boolean full = blockEntity.isFull;
        if (!initialized || doorAngle != lastDoorAngle) {
            doorPose.set(bodyPose).translate(0F, 0F, doorAngle * .75F / 135F);
            write(0, doorPose, true);
            lastDoorAngle = doorAngle;
        }
        if (!initialized || full != lastFull) {
            write(1, bodyPose, full);
            write(2, bodyPose, !full);
            lastFull = full;
        }
        initialized = true;
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
                                pos.getY() + 1,
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
    }
}
