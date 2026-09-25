// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.turret.BlockEntityTurretArty;
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

public final class TurretArtyVisual extends HbmDynamicBlockEntityVisual<BlockEntityTurretArty>
        implements ShaderLightVisual {
    private static final HFRWavefrontObject MODEL = ResourceManager.turret_arty;
    private static final Material MATERIAL = MeshPart.litCutout(ResourceManager.turret_arty_tex);
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Carriage")], MODEL.smoothing(), MATERIAL);
    private static final MeshPart CANNON_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Cannon")], MODEL.smoothing(), MATERIAL);
    private static final MeshPart BARREL_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Barrel")], MODEL.smoothing(), MATERIAL);
    private final AABB bodyBounds;
    private final TransformedInstance carriage;
    private final TransformedInstance cannon;
    private final TransformedInstance barrel;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f cannonPose = new Matrix4f();
    private final Matrix4f barrelPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private float lastRecoil = Float.NaN;
    private boolean initialized;

    public TurretArtyVisual(
            VisualizationContext context, BlockEntityTurretArty blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        Matrix4f bodyLocal = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        bodyBounds = LightBounds.of(MODEL, "Base", bodyLocal, pos);
        rootPose.set(bodyLocal);
        carriage =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CARRIAGE_PART.model())
                        .createInstance();
        cannon =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CANNON_PART.model())
                        .createInstance();
        barrel =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BARREL_PART.model())
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
        float recoil =
                (float)
                        (Mth.lerp(partialTick, blockEntity.lastBarrelPos, blockEntity.barrelPos)
                                * 2.5D);
        boolean yawChanged =
                !initialized || Float.floatToIntBits(yawRadians) != Float.floatToIntBits(lastYaw);
        boolean pitchChanged =
                !initialized
                        || Float.floatToIntBits(pitchRadians) != Float.floatToIntBits(lastPitch);
        boolean recoilChanged =
                !initialized || Float.floatToIntBits(recoil) != Float.floatToIntBits(lastRecoil);
        if (!yawChanged && !pitchChanged && !recoilChanged) return;
        if (yawChanged) {
            carriagePose.set(rootPose).rotateY(yawRadians - Mth.PI);
            write(carriage, carriagePose);
        }
        if (yawChanged || pitchChanged) {
            cannonPose
                    .set(carriagePose)
                    .translate(0F, 3F, 0F)
                    .rotateX(pitchRadians)
                    .translate(0F, -3F, 0F);
            write(cannon, cannonPose);
        }
        barrelPose.set(cannonPose).translate(0F, 0F, recoil);
        write(barrel, barrelPose);
        LightBounds.resetBounds(lightBounds, bodyBounds);
        LightBounds.includeLightBounds(lightBounds, CARRIAGE_PART.model(), carriagePose, pos);
        LightBounds.includeLightBounds(lightBounds, CANNON_PART.model(), cannonPose, pos);
        LightBounds.includeLightBounds(lightBounds, BARREL_PART.model(), barrelPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastYaw = yawRadians;
        lastPitch = pitchRadians;
        lastRecoil = recoil;
        initialized = true;
    }

    private void write(TransformedInstance instance, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(12));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(carriage);
        consumer.accept(cannon);
        consumer.accept(barrel);
    }

    @Override
    protected void _delete() {
        carriage.delete();
        cannon.delete();
        barrel.delete();
    }
}
