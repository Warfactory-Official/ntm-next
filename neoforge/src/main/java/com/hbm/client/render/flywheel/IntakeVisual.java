// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineIntake;
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
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class IntakeVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineIntake>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.intake;
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.intake_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final MeshPart FAN_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Fan")], MODEL.smoothing(), MATERIAL);
    private final TransformedInstance fan;
    private final Matrix4f fanPose = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB rawBodyBounds;
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastRotation = Float.NaN;

    public IntakeVisual(
            VisualizationContext context, BlockEntityMachineIntake blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Base", basePose, pos));
        fan =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, FAN_PART.model())
                        .createInstance();
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        float rotation = Mth.lerp(partialTick, blockEntity.prevFan, blockEntity.fan);
        if (rotation == lastRotation) return;
        lastRotation = rotation;
        fanPose.set(basePose).translate(-.5F, 0F, .5F).rotateY(-rotation * Mth.DEG_TO_RAD);
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(fanPose);
        fan.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, FAN_PART.model(), fanPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
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
        consumer.accept(fan);
    }

    @Override
    protected void _delete() {
        fan.delete();
    }
}
