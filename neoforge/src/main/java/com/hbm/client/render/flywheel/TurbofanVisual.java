// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineTurbofan;
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

public final class TurbofanVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineTurbofan>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.turbofan;
    private static final MeshPart BLADES_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Blades")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.turbofan_tex));
    private static final MeshPart AFTERBURNER_COLD_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Afterburner")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.turbofan_back_tex));
    private static final MeshPart AFTERBURNER_HOT_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Afterburner")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.turbofan_afterburner_tex));

    private final TransformedInstance blades;
    private final TransformedInstance afterburnerCold;
    private final TransformedInstance afterburnerHot;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f afterburnerPose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB rawBodyBounds;
    private float lastSpin = Float.NaN;
    private boolean lastHot;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public TurbofanVisual(
            VisualizationContext context,
            BlockEntityMachineTurbofan blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        var rawBodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                Facing.yaw(
                                                BlockMultiblockCore.coreFacing(
                                                        blockEntity.getBlockState()),
                                                90)
                                        * Mth.DEG_TO_RAD);
        basePose.set(rawBodyLocal);
        rawBodyBounds = LightBounds.of(MODEL, "Body", rawBodyLocal, pos);
        blades =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BLADES_PART.model())
                        .createInstance();
        afterburnerCold =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, AFTERBURNER_COLD_PART.model())
                        .createInstance();
        afterburnerHot =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, AFTERBURNER_HOT_PART.model())
                        .createInstance();
        afterburnerPose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(basePose);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float spin = blockEntity.lastSpin + (blockEntity.spin - blockEntity.lastSpin) * partialTick;
        boolean hot = blockEntity.afterburner > 0;
        boolean spinChanged = !initialized || Float.compare(spin, lastSpin) != 0;
        boolean hotChanged = !initialized || hot != lastHot;
        if (!spinChanged && !hotChanged) return;
        lastSpin = spin;
        lastHot = hot;
        initialized = true;
        if (spinChanged) {
            localPose
                    .set(basePose)
                    .translate(0F, 1.5F, 0F)
                    .rotateZ(-(spin) * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(localPose);
            blades.setTransform(instancePose).light(0).setChanged();
        }
        if (hotChanged) {
            TransformedInstance shown = hot ? afterburnerHot : afterburnerCold;
            (hot ? afterburnerCold : afterburnerHot).setVisible(false);
            shown.setVisible(true);
            shown.setTransform(afterburnerPose).light(0).setChanged();
        }
        LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, BLADES_PART.model(), basePose, pos);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator,
                hot ? AFTERBURNER_HOT_PART.model() : AFTERBURNER_COLD_PART.model(),
                basePose,
                pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                pos.getY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 3,
                                pos.getZ() + 4)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(blades);
        consumer.accept(afterburnerCold);
        consumer.accept(afterburnerHot);
    }

    @Override
    protected void _delete() {
        blades.delete();
        afterburnerCold.delete();
        afterburnerHot.delete();
    }
}
