// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.MachineThresher;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityThresher;
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

public final class ThresherVisual extends HbmDynamicBlockEntityVisual<BlockEntityThresher>
        implements ShaderLightVisual {
    private static final String[] NAMES = {"Engine", "ArmUpper", "ArmLower", "Front", "Wheel"};
    private static final HFRWavefrontObject MODEL = ResourceManager.thresher;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.thresher_tex);
    private static final MeshPart[] MOVING_PARTS = buildMovingParts();
    private final AABB bodyBounds;
    private final TransformedInstance[] instances = new TransformedInstance[NAMES.length];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f[] partPoses = {
        new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f(), new Matrix4f()
    };
    private final double[] lightBounds = new double[6];
    private float lastArm = Float.NaN;
    private float lastSpin = Float.NaN;
    private float lastEngine = Float.NaN;
    private @Nullable AABB lastLightBounds;

    public ThresherVisual(
            VisualizationContext context, BlockEntityThresher blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyPose =
                new Matrix4f()
                        .translate(.5F, 0F, .5F)
                        .rotateY(
                                (Facing.yaw(blockState.getValue(MachineThresher.FACING), 0))
                                        * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Base", bodyPose, pos);
        basePose.set(bodyPose);
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
        float arm =
                82.5F
                        - (blockEntity.prevAngle
                                + (blockEntity.angle - blockEntity.prevAngle) * partialTick);
        float spin = blockEntity.lastSpin + (blockEntity.spin - blockEntity.lastSpin) * partialTick;
        float engine =
                blockEntity.isOn
                        ? (float) Math.sin((level.getGameTime() * 2 % (2 * Math.PI)) + partialTick)
                        : 0F;
        boolean armChanged = Float.floatToIntBits(arm) != Float.floatToIntBits(lastArm);
        boolean spinChanged = Float.floatToIntBits(spin) != Float.floatToIntBits(lastSpin);
        boolean engineChanged = Float.floatToIntBits(engine) != Float.floatToIntBits(lastEngine);
        if (!armChanged && !spinChanged && !engineChanged) return;

        if (engineChanged) {
            partPoses[0].set(basePose).translate(0F, engine * .01F, 0F);
            write(0, partPoses[0]);
        }
        if (armChanged) {
            partPoses[1]
                    .set(basePose)
                    .translate(0F, .5F, -1F)
                    .rotateX((arm) * Mth.DEG_TO_RAD)
                    .translate(0F, -.5F, 1F);
            write(1, partPoses[1]);
            partPoses[2]
                    .set(partPoses[1])
                    .translate(0F, .5F, -5F)
                    .rotateX((arm * -2F) * Mth.DEG_TO_RAD)
                    .translate(0F, -.5F, 5F)
                    .translate(-.01F, 0F, 0F);
            write(2, partPoses[2]);
            partPoses[3]
                    .set(partPoses[2])
                    .translate(.01F, 0F, 0F)
                    .translate(0F, .5F, -9F)
                    .rotateX((arm) * Mth.DEG_TO_RAD)
                    .translate(0F, -.5F, 9F)
                    .translate(.01F, 0F, 0F);
            write(3, partPoses[3]);
        }
        if (armChanged || spinChanged) {
            partPoses[4]
                    .set(partPoses[3])
                    .translate(0F, .5F, -11F)
                    .rotateX((-spin) * Mth.DEG_TO_RAD)
                    .translate(0F, -.5F, 11F);
            write(4, partPoses[4]);
        }
        LightBounds.resetBounds(lightBounds, bodyBounds);
        for (int i = 0; i < partPoses.length; i++)
            LightBounds.includeLightBounds(lightBounds, MOVING_PARTS[i].model(), partPoses[i], pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastArm = arm;
        lastSpin = spin;
        lastEngine = engine;
    }

    private void write(int index, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instances[index].setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 10,
                                pos.getY(),
                                pos.getZ() - 10,
                                pos.getX() + 11,
                                pos.getY() + 7,
                                pos.getZ() + 11)
                        .inflate(1));
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
