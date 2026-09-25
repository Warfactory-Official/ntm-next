// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretChekhov;
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

public final class TurretChekhovVisual extends HbmDynamicBlockEntityVisual<BlockEntityTurretChekhov>
        implements ShaderLightVisual {
    private static final float[] CONNECTOR_ROTATION = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] CONNECTOR_Z = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Carriage")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_carriage_tex));
    private static final MeshPart GUN_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Body")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_chekhov_tex));
    private static final MeshPart BARRELS_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Barrels")],
                    ResourceManager.turret_chekhov.smoothing(),
                    MeshPart.litCutout(ResourceManager.turret_chekhov_barrels_tex));

    private final AABB bodyBounds;
    private final TransformedInstance[] connectors = new TransformedInstance[8];
    private final Matrix4f[] connectorPoses = new Matrix4f[8];
    private final TransformedInstance carriage;
    private final TransformedInstance gun;
    private final TransformedInstance barrels;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f gunPose = new Matrix4f();
    private final Matrix4f barrelsPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private int lastConnectorMask = Integer.MIN_VALUE;
    private float lastYawRadians = Float.NaN;
    private float lastPitchRadians = Float.NaN;
    private float lastSpin = Float.NaN;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public TurretChekhovVisual(
            VisualizationContext context, BlockEntityTurretChekhov blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        Matrix4f bodyLocal = new Matrix4f().translation((float) offset.x, 0F, (float) offset.z);
        rootPose.set(bodyLocal);
        bodyBounds = LightBounds.of(ResourceManager.turret_chekhov, "Base", bodyLocal, pos);

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
        barrels =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BARRELS_PART.model())
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
        float spin = Mth.lerp(partialTick, blockEntity.lastSpin, blockEntity.spin) * Mth.DEG_TO_RAD;
        int mask = blockEntity.connectorMask;
        boolean maskChanged = !initialized || mask != lastConnectorMask;
        boolean mechanicalChanged =
                !initialized
                        || Float.compare(yawRadians, lastYawRadians) != 0
                        || Float.compare(pitchRadians, lastPitchRadians) != 0
                        || Float.compare(spin, lastSpin) != 0;
        if (!maskChanged && !mechanicalChanged) return;
        if (maskChanged)
            for (int i = 0; i < connectors.length; i++) writeConnector(i, (mask & 1 << i) != 0);
        lastConnectorMask = mask;
        lastYawRadians = yawRadians;
        lastPitchRadians = pitchRadians;
        lastSpin = spin;
        initialized = true;
        carriagePose.set(rootPose).rotateY(yawRadians - Mth.HALF_PI);
        gunPose.set(carriagePose)
                .translate(0F, 1.5F, 0F)
                .rotateZ(pitchRadians)
                .translate(0F, -1.5F, 0F);
        barrelsPose.set(gunPose).translate(0F, 1.5F, 0F).rotateX(-spin).translate(0F, -1.5F, 0F);
        write(carriage, carriagePose);
        write(gun, gunPose);
        write(barrels, barrelsPose);
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator, CARRIAGE_PART.model(), carriagePose, pos);
        LightBounds.includeLightBounds(lightBoundsAccumulator, GUN_PART.model(), gunPose, pos);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator, BARRELS_PART.model(), barrelsPose, pos);
        for (int i = 0; i < connectors.length; i++)
            if ((mask & 1 << i) != 0)
                LightBounds.includeLightBounds(
                        lightBoundsAccumulator, CONNECTOR_PART.model(), connectorPoses[i], pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
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
        consumer.accept(barrels);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance connector : connectors) connector.delete();
        carriage.delete();
        gun.delete();
        barrels.delete();
    }
}
