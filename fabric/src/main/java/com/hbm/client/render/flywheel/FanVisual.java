// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.MachineFan;
import com.hbm.client.model.FanModel;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityFan;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FanVisual extends HbmDynamicBlockEntityVisual<BlockEntityFan>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.fan;
    private static final MeshPart BLADES_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId(FanModel.BLADES)],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.fan_tex));
    private final TransformedInstance blades;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private final AABB rawBodyBounds;
    private @Nullable AABB lastLightBounds;
    private float lastSpin = Float.NaN;

    public FanVisual(VisualizationContext context, BlockEntityFan blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, .5F, .5F);
        switch (blockState.getValue(MachineFan.FACING)) {
            case DOWN -> basePose.rotateX((180F) * Mth.DEG_TO_RAD);
            case UP -> {}
            case NORTH -> basePose.rotateX((-90F) * Mth.DEG_TO_RAD);
            case SOUTH -> basePose.rotateX((90F) * Mth.DEG_TO_RAD);
            case WEST -> basePose.rotateZ((90F) * Mth.DEG_TO_RAD);
            case EAST -> basePose.rotateZ((-90F) * Mth.DEG_TO_RAD);
        }
        basePose.translate(0F, -.5F, 0F);
        rawBodyBounds = LightBounds.of(MODEL, "Frame", basePose, pos);
        blades =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BLADES_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float spin = blockEntity.prevSpin + (blockEntity.spin - blockEntity.prevSpin) * partialTick;
        if (spin == lastSpin) return;
        localPose.set(basePose).rotateY(-(spin) * Mth.DEG_TO_RAD);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        blades.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBounds, rawBodyBounds);
        LightBounds.includeLightBounds(lightBounds, BLADES_PART.model(), localPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastSpin = spin;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(blades);
    }

    @Override
    protected void _delete() {
        blades.delete();
    }
}
