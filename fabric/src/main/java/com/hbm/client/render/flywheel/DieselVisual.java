// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.MachineDiesel;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineDiesel;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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

public final class DieselVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineDiesel>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.dieselgen;
    private static final MeshPart ENGINE_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Engine")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.dieselgen_tex));
    private final TransformedInstance engine;
    private final AABB bodyBounds;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private long lastTime = Long.MIN_VALUE;
    private boolean lastShaking;
    private boolean initialized;

    public DieselVisual(
            VisualizationContext context, BlockEntityMachineDiesel blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(MachineDiesel.FACING), 90) * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Generator", basePose, pos);
        engine =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ENGINE_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        boolean shaking =
                blockEntity.isOn
                        && blockEntity.hasAcceptableFuel()
                        && blockEntity.tank.getFill() > 0;
        long time = GameTime.now();
        if (initialized && shaking == lastShaking && (!shaking || time == lastTime)) return;
        localPose.set(basePose);
        if (shaking)
            localPose.translate(
                    (float) (Math.sin(time / 25D) * .005D),
                    0F,
                    (float) (Math.sin(time / 50D) * .005D));
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPose);
        engine.setTransform(instancePose).light(0).setChanged();
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, ENGINE_PART.model(), localPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastShaking = shaking;
        lastTime = time;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(engine);
    }

    @Override
    protected void _delete() {
        engine.delete();
    }
}
