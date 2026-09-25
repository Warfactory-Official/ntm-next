// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystronCreative;
import com.hbm.util.Facing;
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

public final class FusionKlystronCreativeVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityFusionKlystronCreative>
        implements ShaderLightVisual {
    private static final MeshPart ROTOR_PART =
            MeshPart.obj(
                    FusionKlystronVisual.MODEL.groups[FusionKlystronVisual.MODEL.partId("Rotor")],
                    FusionKlystronVisual.MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.fusion_klystron_creative_tex));

    private final TransformedInstance rotor;
    private final AABB bodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastFan = Float.NaN;

    public FusionKlystronCreativeVisual(
            VisualizationContext context,
            BlockEntityFusionKlystronCreative blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds =
                LightBounds.of(
                        FusionKlystronVisual.MODEL,
                        "Klystron",
                        new Matrix4f(basePose).translate(-1F, 0F, 0F),
                        pos);
        rotor =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROTOR_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float fan = Mth.lerp(partialTick, blockEntity.prevFan, blockEntity.fan);
        if (fan == lastFan) return;
        FusionKlystronVisual.rotorPose(localPose, basePose, fan);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        rotor.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, ROTOR_PART.model(), localPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastFan = fan;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(5).expandTowards(0, 5, 0).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(rotor);
    }

    @Override
    protected void _delete() {
        rotor.delete();
    }
}
