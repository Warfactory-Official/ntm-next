// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineSatLink;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SatLinkVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineSatLink>
        implements ShaderLightVisual {

    private static final MeshPart ROTOR =
            MeshPart.obj(
                    ResourceManager.satlink.groups[ResourceManager.satlink.partId("Rotor")],
                    ResourceManager.satlink.smoothing(),
                    MeshPart.litCutout(ResourceManager.satlink_tex));
    private static final MeshPart DISH =
            MeshPart.obj(
                    ResourceManager.satlink.groups[ResourceManager.satlink.partId("Dish")],
                    ResourceManager.satlink.smoothing(),
                    MeshPart.litCutout(ResourceManager.satlink_tex));

    private final TransformedInstance rotor;
    private final TransformedInstance dish;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f rotorPose = new Matrix4f();
    private final Matrix4f dishPose = new Matrix4f();
    private final Matrix4f worldPose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB originBounds;
    private @Nullable AABB lastLightBounds;
    private float lastRot = Float.NaN;
    private float lastLift = Float.NaN;

    public SatLinkVisual(
            VisualizationContext context,
            BlockEntityMachineSatLink blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Direction facing = blockEntity.getBlockState().getValue(BlockMultiblockCore.FACING);
        Direction lateral = facing.getCounterClockWise();
        basePose.translation(.5F, 0F, .5F)
                .rotateY(180F * Mth.DEG_TO_RAD)
                .translate(
                        (facing.getStepX() + lateral.getStepX()) * .5F,
                        0F,
                        (facing.getStepZ() + lateral.getStepZ()) * .5F);
        originBounds =
                new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        rotor =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROTOR.model())
                        .createInstance();
        dish =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, DISH.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    private void updateMovingParts(float partialTick) {
        float rot = blockEntity.prevRot + (blockEntity.rot - blockEntity.prevRot) * partialTick;
        float lift = blockEntity.prevLift + (blockEntity.lift - blockEntity.prevLift) * partialTick;
        if (rot == lastRot && lift == lastLift && lastLightBounds != null) return;
        lastRot = rot;
        lastLift = lift;

        rotorPose.set(basePose).rotateY(rot * Mth.DEG_TO_RAD);
        dishPose.set(rotorPose)
                .translate(0F, 7.375F, 0F)
                .rotateZ(lift * Mth.DEG_TO_RAD)
                .translate(0F, -7.375F, 0F);
        worldPose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(rotorPose);
        rotor.setTransform(worldPose).light(0).setChanged();
        worldPose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(dishPose);
        dish.setTransform(worldPose).light(0).setChanged();

        LightBounds.resetBounds(lightBoundsAccumulator, originBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, ROTOR.model(), rotorPose, pos);
        LightBounds.includeLightBounds(lightBoundsAccumulator, DISH.model(), dishPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                pos.getX() - 2,
                pos.getY(),
                pos.getZ() - 2,
                pos.getX() + 3,
                pos.getY() + 10,
                pos.getZ() + 3);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotor);
        consumer.accept(dish);
    }

    @Override
    protected void _delete() {
        rotor.delete();
        dish.delete();
    }
}
