// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystron;
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

public final class FusionKlystronVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityFusionKlystron>
        implements ShaderLightVisual {
    static final HFRWavefrontObject MODEL = ResourceManager.fusion_klystron;
    private static final MeshPart ROTOR_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Rotor")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.fusion_klystron_tex));
    private final AABB bodyBounds;
    private final TransformedInstance rotor;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private float lastFan = Float.NaN;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public FusionKlystronVisual(
            VisualizationContext context,
            BlockEntityFusionKlystron blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.identity()
                .translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds =
                LightBounds.of(
                        MODEL, "Klystron", new Matrix4f(basePose).translate(-1F, 0F, 0F), pos);
        rotor =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROTOR_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    static void rotorPose(Matrix4f pose, Matrix4f base, float fan) {
        pose.set(base)
                .translate(-1F, 2.5F, 0F)
                .rotateX((fan) * Mth.DEG_TO_RAD)
                .translate(0F, -2.5F, 0F);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float fan = Mth.lerp(partialTick, blockEntity.prevFan, blockEntity.fan);
        if (initialized && Float.compare(fan, lastFan) == 0) return;
        lastFan = fan;
        initialized = true;
        rotorPose(localPose, basePose, fan);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        rotor.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, ROTOR_PART.model(), localPose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
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
