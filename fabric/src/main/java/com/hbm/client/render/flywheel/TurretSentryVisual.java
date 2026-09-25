// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.turret.BlockEntityTurretSentry;
import com.hbm.tileentity.turret.BlockEntityTurretSentryDamaged;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class TurretSentryVisual extends HbmDynamicBlockEntityVisual<BlockEntityTurretSentry>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.turret_sentry;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.turret_sentry_tex);
    private static final Material DAMAGED_MATERIAL =
            MeshPart.litCutout(ResourceManager.turret_sentry_damaged_tex);
    private static final int GROUP_1 = MODEL.partId("Pivot");
    private static final int GROUP_2 = MODEL.partId("Body");
    private static final int GROUP_3 = MODEL.partId("Drum");
    private static final int GROUP_4 = MODEL.partId("BarrelL");
    private static final int GROUP_5 = MODEL.partId("BarrelR");
    private static final MeshPart PART_1 = MeshPart.obj(MODEL.groups[GROUP_1], true, MATERIAL);
    private static final MeshPart PART_2 = MeshPart.obj(MODEL.groups[GROUP_2], true, MATERIAL);
    private static final MeshPart PART_3 = MeshPart.obj(MODEL.groups[GROUP_3], true, MATERIAL);
    private static final MeshPart PART_4 = MeshPart.obj(MODEL.groups[GROUP_4], true, MATERIAL);
    private static final MeshPart PART_5 = MeshPart.obj(MODEL.groups[GROUP_5], true, MATERIAL);
    private static final MeshPart DAMAGED_1 =
            MeshPart.obj(MODEL.groups[GROUP_1], true, DAMAGED_MATERIAL);
    private static final MeshPart DAMAGED_2 =
            MeshPart.obj(MODEL.groups[GROUP_2], true, DAMAGED_MATERIAL);
    private static final MeshPart DAMAGED_3 =
            MeshPart.obj(MODEL.groups[GROUP_3], true, DAMAGED_MATERIAL);
    private static final MeshPart DAMAGED_4 =
            MeshPart.obj(MODEL.groups[GROUP_4], true, DAMAGED_MATERIAL);
    private static final MeshPart DAMAGED_5 =
            MeshPart.obj(MODEL.groups[GROUP_5], true, DAMAGED_MATERIAL);
    private final boolean damaged;
    private final MeshPart pivotPart;
    private final MeshPart bodyPart;
    private final MeshPart drumPart;
    private final MeshPart barrelLeftPart;
    private final MeshPart barrelRightPart;
    private final TransformedInstance pivot;
    private final TransformedInstance body;
    private final TransformedInstance drum;
    private final TransformedInstance barrelLeft;
    private final TransformedInstance barrelRight;
    private final Matrix4f placement = new Matrix4f();
    private final Matrix4f pivotPose = new Matrix4f();
    private final Matrix4f bodyPose = new Matrix4f();
    private final Matrix4f drumPose = new Matrix4f();
    private final Matrix4f barrelLeftPose = new Matrix4f();
    private final Matrix4f barrelRightPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private final AABB baseBounds;
    private @Nullable AABB lastLightBounds;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private float lastLeft = Float.NaN;
    private float lastRight = Float.NaN;
    private boolean initialized;

    public TurretSentryVisual(
            VisualizationContext context, BlockEntityTurretSentry blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        placement.translation((float) offset.x, 0F, (float) offset.z);
        damaged = blockEntity instanceof BlockEntityTurretSentryDamaged;
        pivotPart = damaged ? DAMAGED_1 : PART_1;
        bodyPart = damaged ? DAMAGED_2 : PART_2;
        drumPart = damaged ? DAMAGED_3 : PART_3;
        barrelLeftPart = damaged ? DAMAGED_4 : PART_4;
        barrelRightPart = damaged ? DAMAGED_5 : PART_5;

        baseBounds = LightBounds.of(MODEL, "Base", placement, pos);

        pivot =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, pivotPart.model())
                        .createInstance();
        body =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, bodyPart.model())
                        .createInstance();
        drum =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, drumPart.model())
                        .createInstance();
        barrelLeft =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, barrelLeftPart.model())
                        .createInstance();
        barrelRight =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, barrelRightPart.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float yawRadians =
                (float)
                        -Mth.lerp(
                                partialTick, blockEntity.lastRotationYaw, blockEntity.rotationYaw);
        float pitchRadians =
                (float)
                        Mth.lerp(
                                partialTick,
                                blockEntity.lastRotationPitch,
                                blockEntity.rotationPitch);
        float left =
                (float)
                                Mth.lerp(
                                        partialTick,
                                        blockEntity.lastBarrelLeftPos,
                                        blockEntity.barrelLeftPos)
                        * -.5F;
        float right =
                (float)
                                Mth.lerp(
                                        partialTick,
                                        blockEntity.lastBarrelRightPos,
                                        blockEntity.barrelRightPos)
                        * -.5F;
        boolean yawChanged =
                !initialized || Float.floatToIntBits(yawRadians) != Float.floatToIntBits(lastYaw);
        boolean pitchChanged =
                !initialized
                        || Float.floatToIntBits(pitchRadians) != Float.floatToIntBits(lastPitch);
        boolean leftChanged =
                !initialized || Float.floatToIntBits(left) != Float.floatToIntBits(lastLeft);
        boolean rightChanged =
                !initialized || Float.floatToIntBits(right) != Float.floatToIntBits(lastRight);
        if (!yawChanged && !pitchChanged && !leftChanged && !rightChanged) return;
        if (yawChanged) {
            pivotPose.set(placement).rotateY(yawRadians);
            write(pivot, pivotPose);
        }
        if (yawChanged || pitchChanged) {
            bodyPose.set(pivotPose)
                    .translate(0F, 1.25F, 0F)
                    .rotateX(-pitchRadians)
                    .translate(0F, -1.25F, 0F);
            drumPose.set(bodyPose);
            write(body, bodyPose);
            write(drum, drumPose);
        }
        if (yawChanged || pitchChanged || leftChanged) {
            barrelLeftPose.set(bodyPose).translate(0F, 0F, left);
            write(barrelLeft, barrelLeftPose);
        }
        if (yawChanged || pitchChanged || rightChanged) {
            barrelRightPose.set(bodyPose);

            if (damaged) {
                barrelRightPose
                        .translate(0F, 1.5F, 0.5F)
                        .rotateX(25F * Mth.DEG_TO_RAD)
                        .translate(0F, -1.5F, -0.5F);
            } else {
                barrelRightPose.translate(0F, 0F, right);
            }
            write(barrelRight, barrelRightPose);
        }
        LightBounds.resetBounds(lightBounds, baseBounds);
        LightBounds.includeLightBounds(lightBounds, pivotPart.model(), pivotPose, pos);
        LightBounds.includeLightBounds(lightBounds, bodyPart.model(), bodyPose, pos);
        LightBounds.includeLightBounds(lightBounds, drumPart.model(), drumPose, pos);
        LightBounds.includeLightBounds(lightBounds, barrelLeftPart.model(), barrelLeftPose, pos);
        LightBounds.includeLightBounds(lightBounds, barrelRightPart.model(), barrelRightPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastYaw = yawRadians;
        lastPitch = pitchRadians;
        lastLeft = left;
        lastRight = right;
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f local) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(local);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return baseBounds.minmax(new AABB(pos).inflate(8));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(pivot);
        consumer.accept(body);
        consumer.accept(drum);
        consumer.accept(barrelLeft);
        consumer.accept(barrelRight);
    }

    @Override
    protected void _delete() {
        pivot.delete();
        body.delete();
        drum.delete();
        barrelLeft.delete();
        barrelRight.delete();
    }
}
