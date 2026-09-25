// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.turret.BlockEntityTurretHoward;
import dev.engine_room.flywheel.api.instance.Instance;
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

public final class TurretHowardVisual extends HbmDynamicBlockEntityVisual<BlockEntityTurretHoward>
        implements ShaderLightVisual {
    private static final float[] CONNECTOR_ROTATION = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] CONNECTOR_Z = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};
    private static final HFRWavefrontObject MODEL = ResourceManager.turret_howard;
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Carriage")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_carriage_ciws_tex));
    private static final MeshPart GUN_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Body")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_howard_tex));
    private static final MeshPart TOP_BARRELS_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("BarrelsTop")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_howard_barrels_tex));
    private static final MeshPart BOTTOM_BARRELS_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("BarrelsBottom")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_howard_barrels_tex));
    private final AABB bodyBounds;
    private final TransformedInstance[] connectors = new TransformedInstance[8];
    private final Matrix4f[] connectorPoses = new Matrix4f[8];
    private final TransformedInstance carriage;
    private final TransformedInstance gun;
    private final TransformedInstance topBarrels;
    private final TransformedInstance bottomBarrels;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f gunPose = new Matrix4f();
    private final Matrix4f topBarrelsPose = new Matrix4f();
    private final Matrix4f bottomBarrelsPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private float lastSpin = Float.NaN;
    private int lastConnectorMask;
    private boolean initialized;

    public TurretHowardVisual(
            VisualizationContext context, BlockEntityTurretHoward blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        Matrix4f bodyLocal = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        bodyBounds = LightBounds.of(ResourceManager.turret_chekhov, "Base", bodyLocal, pos);
        rootPose.set(bodyLocal);
        for (int i = 0; i < connectors.length; i++) {
            connectors[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, CONNECTOR_PART.model())
                            .createInstance();
            connectorPoses[i] =
                    new Matrix4f(bodyLocal)
                            .rotateY(CONNECTOR_ROTATION[i] * Mth.DEG_TO_RAD)
                            .translate(0F, 0F, CONNECTOR_Z[i]);
        }
        carriage =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CARRIAGE_PART.model())
                        .createInstance();
        gun =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, GUN_PART.model())
                        .createInstance();
        topBarrels =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, TOP_BARRELS_PART.model())
                        .createInstance();
        bottomBarrels =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BOTTOM_BARRELS_PART.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int mask = blockEntity.connectorMask;
        boolean connectorChanged = !initialized || mask != lastConnectorMask;
        if (connectorChanged)
            for (int i = 0; i < connectors.length; i++) writeConnector(i, (mask & 1 << i) != 0);

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
        float spin = Mth.lerp(partialTick, blockEntity.lastSpin, blockEntity.spin) * Mth.DEG_TO_RAD;
        boolean yawChanged =
                !initialized || Float.floatToIntBits(yawRadians) != Float.floatToIntBits(lastYaw);
        boolean pitchChanged =
                !initialized
                        || Float.floatToIntBits(pitchRadians) != Float.floatToIntBits(lastPitch);
        boolean spinChanged =
                !initialized || Float.floatToIntBits(spin) != Float.floatToIntBits(lastSpin);
        if (!connectorChanged && !yawChanged && !pitchChanged && !spinChanged) return;
        if (yawChanged) {
            carriagePose.set(rootPose).rotateY(yawRadians - Mth.HALF_PI);
            write(carriage, carriagePose);
        }
        if (yawChanged || pitchChanged) {
            gunPose.set(carriagePose)
                    .translate(0F, 2.25F, 0F)
                    .rotateZ(pitchRadians)
                    .translate(0F, -2.25F, 0F);
            write(gun, gunPose);
        }
        if (yawChanged || pitchChanged || spinChanged) {
            topBarrelsPose
                    .set(gunPose)
                    .translate(0F, 2.5F, 0F)
                    .rotateX(-spin)
                    .translate(0F, -2.5F, 0F);
            bottomBarrelsPose
                    .set(gunPose)
                    .translate(0F, 2F, 0F)
                    .rotateX(spin)
                    .translate(0F, -2F, 0F);
            write(topBarrels, topBarrelsPose);
            write(bottomBarrels, bottomBarrelsPose);
        }
        LightBounds.resetBounds(lightBounds, bodyBounds);
        for (int i = 0; i < connectors.length; i++)
            if ((mask & 1 << i) != 0)
                LightBounds.includeLightBounds(
                        lightBounds, CONNECTOR_PART.model(), connectorPoses[i], pos);
        LightBounds.includeLightBounds(lightBounds, CARRIAGE_PART.model(), carriagePose, pos);
        LightBounds.includeLightBounds(lightBounds, GUN_PART.model(), gunPose, pos);
        LightBounds.includeLightBounds(lightBounds, TOP_BARRELS_PART.model(), topBarrelsPose, pos);
        LightBounds.includeLightBounds(
                lightBounds, BOTTOM_BARRELS_PART.model(), bottomBarrelsPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastYaw = yawRadians;
        lastPitch = pitchRadians;
        lastSpin = spin;
        lastConnectorMask = mask;
        initialized = true;
    }

    private void writeConnector(int index, boolean visible) {
        TransformedInstance instance = connectors[index];
        instance.setVisible(visible);
        if (!visible) return;
        write(instance, connectorPoses[index]);
    }

    private void write(TransformedInstance instance, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(8));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance connector : connectors) consumer.accept(connector);
        consumer.accept(carriage);
        consumer.accept(gun);
        consumer.accept(topBarrels);
        consumer.accept(bottomBarrels);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance connector : connectors) connector.delete();
        carriage.delete();
        gun.delete();
        topBarrels.delete();
        bottomBarrels.delete();
    }
}
