// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityHeaterOven;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class HeaterOvenVisual extends HbmDynamicBlockEntityVisual<BlockEntityHeaterOven>
        implements ShaderLightVisual {
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.heater_oven_tex);
    private static final Material HOT_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.heater_oven_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .build();
    private static final MeshPart DOOR_PART =
            MeshPart.obj(
                    ResourceManager.heater_oven.groups[ResourceManager.heater_oven.partId("Door")],
                    ResourceManager.heater_oven.smoothing(),
                    BODY_MATERIAL);
    private static final MeshPart INNER_PART =
            MeshPart.obj(
                    ResourceManager.heater_oven.groups[ResourceManager.heater_oven.partId("Inner")],
                    ResourceManager.heater_oven.smoothing(),
                    BODY_MATERIAL);
    private static final MeshPart BURNING_PART =
            MeshPart.obj(
                    ResourceManager.heater_oven
                            .groups[ResourceManager.heater_oven.partId("InnerBurning")],
                    ResourceManager.heater_oven.smoothing(),
                    HOT_MATERIAL);

    private final AABB bodyBounds;
    private final TransformedInstance door;
    private final TransformedInstance innerBurning;
    private final TransformedInstance inner;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f doorPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastDoorAngle = Float.NaN;
    private boolean lastBurning;
    private boolean initialized;

    public HeaterOvenVisual(
            VisualizationContext context, BlockEntityHeaterOven blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translate(.5F, 0F, .5F)
                .rotateY(
                        (Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 180) - 90F)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.heater_oven, "Main", basePose, pos);
        door =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, DOOR_PART.model())
                        .createInstance();
        innerBurning =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BURNING_PART.model())
                        .createInstance();
        inner =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, INNER_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float doorAngle =
                blockEntity.prevDoorAngle
                        + (blockEntity.doorAngle - blockEntity.prevDoorAngle) * partialTick;
        boolean burning = blockEntity.wasOn;
        boolean doorChanged = !initialized || doorAngle != lastDoorAngle;
        boolean burnChanged = !initialized || burning != lastBurning;
        if (doorChanged) {
            doorPose.set(basePose).translate(0F, 0F, doorAngle * .75F / 135F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(doorPose);
            door.setTransform(instancePose).light(0).setChanged();
            lastDoorAngle = doorAngle;
        }

        if (burnChanged) {
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(basePose);
            if (burning) {
                innerBurning.setVisible(true);
                innerBurning
                        .setTransform(instancePose)
                        .light(LightCoordsUtil.FULL_BRIGHT)
                        .setChanged();
                inner.setVisible(false);
            } else {
                innerBurning.setVisible(false);
                inner.setVisible(true);
                inner.setTransform(instancePose).light(0).setChanged();
            }
            lastBurning = burning;
        }
        if (initialized && !doorChanged && !burnChanged) return;
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, DOOR_PART.model(), doorPose, pos);
        if (!burning)
            LightBounds.includeLightBounds(lightBounds, INNER_PART.model(), basePose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).expandTowards(1, 1, 1).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(door);
        consumer.accept(inner);
    }

    @Override
    protected void _delete() {
        door.delete();
        innerBurning.delete();
        inner.delete();
    }
}
