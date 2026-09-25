// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineRadar;
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

public final class RadarVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineRadar>
        implements ShaderLightVisual {
    private static final MeshPart DISH_PART =
            MeshPart.obj(
                    ResourceManager.radar.groups[ResourceManager.radar.partId("Dish")],
                    ResourceManager.radar.smoothing(),
                    SimpleMaterial.builderOf(MeshPart.litCutout(ResourceManager.radar_dish_tex))
                            .backfaceCulling(false)
                            .build());

    private final TransformedInstance dish;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f dishBasePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB rawBodyBounds;
    private @Nullable AABB lastLightBounds;
    private float lastSpin = Float.NaN;

    public RadarVisual(
            VisualizationContext context, BlockEntityMachineRadar blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal = new Matrix4f().translation(.5F, 0F, .5F);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(LightBounds.of(ResourceManager.radar, "Base", rawBodyLocal, pos));
        dish =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, DISH_PART.model())
                        .createInstance();
        dishBasePose.translation(.5F, 0F, .5F).rotateY((180F) * Mth.DEG_TO_RAD);
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
        localPose.set(dishBasePose).rotateY((-spin) * Mth.DEG_TO_RAD).translate(-.125F, 0F, 0F);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        dish.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, DISH_PART.model(), localPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).expandTowards(1, 3, 1).inflate(1));
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
