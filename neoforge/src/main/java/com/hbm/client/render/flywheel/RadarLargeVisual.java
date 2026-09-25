// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineRadarLarge;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class RadarLargeVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineRadarLarge>
        implements ShaderLightVisual {
    private static final MeshPart DISH_PART =
            MeshPart.obj(
                    ResourceManager.radar_large.groups[ResourceManager.radar_large.partId("Dish")],
                    ResourceManager.radar_large.smoothing(),
                    SimpleMaterial.builderOf(MeshPart.litCutout(ResourceManager.radar_large_tex))
                            .backfaceCulling(false)
                            .build());

    private final TransformedInstance dish;
    private final AABB bodyBounds;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f dishBasePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastSpin = Float.NaN;

    public RadarLargeVisual(
            VisualizationContext context,
            BlockEntityMachineRadarLarge blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal =
                new Matrix4f().translation(.5F, 0F, .5F).rotateY((180F) * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.radar_large, "Radar", bodyLocal, pos);
        dishBasePose.set(bodyLocal);
        dish =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, DISH_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float spin =
                blockEntity.prevRotation
                        + (blockEntity.rotation - blockEntity.prevRotation) * partialTick;
        if (spin == lastSpin) return;
        lastSpin = spin;
        localPose.set(dishBasePose).rotateY((-spin) * Mth.DEG_TO_RAD);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        dish.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, DISH_PART.model(), localPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 5,
                                pos.getY(),
                                pos.getZ() - 5,
                                pos.getX() + 6,
                                pos.getY() + 10,
                                pos.getZ() + 6)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(dish);
    }

    @Override
    protected void _delete() {
        dish.delete();
    }
}
