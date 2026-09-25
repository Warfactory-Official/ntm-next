// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntitySawmill;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SawmillVisual extends HbmDynamicBlockEntityVisual<BlockEntitySawmill>
        implements ShaderLightVisual {
    private static final String[] NAMES = {"Blade", "GearLeft", "GearRight"};
    private static final HFRWavefrontObject MODEL = ResourceManager.sawmill;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.sawmill_tex);
    private static final MeshPart[] MOVING_PARTS = buildMovingParts();
    private final AABB bodyBounds;
    private final TransformedInstance[] instances = new TransformedInstance[NAMES.length];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f[] partPoses = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastSpin = Float.NaN;
    private boolean lastHasBlade;
    private boolean initialized;

    public SawmillVisual(
            VisualizationContext context, BlockEntitySawmill blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(
                                                BlockMultiblockCore.coreFacing(
                                                        blockEntity.getBlockState()),
                                                180))
                                        * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Main", bodyLocal, pos);
        basePose.set(bodyLocal);
        for (int i = 0; i < NAMES.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, MOVING_PARTS[i].model())
                            .createInstance();
        updateMovingParts(partialTick);
    }

    private static MeshPart[] buildMovingParts() {
        var parts = new MeshPart[NAMES.length];
        for (int i = 0; i < parts.length; i++)
            parts[i] =
                    MeshPart.obj(MODEL.groups[MODEL.partId(NAMES[i])], MODEL.smoothing(), MATERIAL);
        return parts;
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float rot = blockEntity.lastSpin + (blockEntity.spin - blockEntity.lastSpin) * partialTick;
        boolean hasBlade = blockEntity.hasBlade;
        boolean spinChanged =
                !initialized || Float.floatToIntBits(rot) != Float.floatToIntBits(lastSpin);
        boolean bladeChanged = !initialized || hasBlade != lastHasBlade;
        if (!spinChanged && !bladeChanged) return;
        if (bladeChanged || hasBlade && spinChanged) {

            partPoses[0]
                    .set(basePose)
                    .translate(0F, 1.375F, 0F)
                    .rotateZ((-rot * 2F) * Mth.DEG_TO_RAD)
                    .translate(0F, -1.375F, 0F);
            write(0, partPoses[0], hasBlade);
        }
        if (spinChanged) {
            partPoses[1]
                    .set(basePose)
                    .translate(.5625F, 1.375F, 0F)
                    .rotateZ((rot) * Mth.DEG_TO_RAD)
                    .translate(-.5625F, -1.375F, 0F);
            write(1, partPoses[1], true);
            partPoses[2]
                    .set(basePose)
                    .translate(-.5625F, 1.375F, 0F)
                    .rotateZ((-rot) * Mth.DEG_TO_RAD)
                    .translate(.5625F, -1.375F, 0F);
            write(2, partPoses[2], true);
        }
        LightBounds.resetBounds(lightBounds, bodyBounds);
        if (hasBlade)
            LightBounds.includeLightBounds(lightBounds, MOVING_PARTS[0].model(), partPoses[0], pos);
        LightBounds.includeLightBounds(lightBounds, MOVING_PARTS[1].model(), partPoses[1], pos);
        LightBounds.includeLightBounds(lightBounds, MOVING_PARTS[2].model(), partPoses[2], pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastSpin = rot;
        lastHasBlade = hasBlade;
        initialized = true;
    }

    private void write(int index, Matrix4f pose, boolean visible) {
        instances[index].setVisible(visible);
        if (!visible) return;
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instances[index].setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).expandTowards(1, 2, 1).inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (var instance : instances) instance.delete();
    }
}
