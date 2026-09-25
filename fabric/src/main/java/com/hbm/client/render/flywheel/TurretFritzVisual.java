// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretFritz;
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

public final class TurretFritzVisual extends HbmDynamicBlockEntityVisual<BlockEntityTurretFritz>
        implements ShaderLightVisual {
    private static final int CONNECTORS = 8;
    private static final float[] PLUG_ROT = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] PLUG_OZ = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Carriage")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_carriage_tex));
    private static final MeshPart GUN_PART =
            MeshPart.obj(
                    ResourceManager.turret_fritz.groups[ResourceManager.turret_fritz.partId("Gun")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_fritz_tex));

    private final TransformedInstance[] connectors = new TransformedInstance[CONNECTORS];
    private final TransformedInstance carriage;
    private final TransformedInstance gun;
    private final Matrix4f placement = new Matrix4f();
    private final Matrix4f[] connectorPoses = new Matrix4f[CONNECTORS];
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f gunPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB baseBounds;
    private @Nullable AABB lastLightBounds;
    private int lastConnectorMask = Integer.MIN_VALUE;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private boolean initialized;

    public TurretFritzVisual(
            VisualizationContext context, BlockEntityTurretFritz blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Vec3 offset = blockEntity.getHorizontalOffset();
        placement.translation((float) offset.x, 0F, (float) offset.z);
        baseBounds = LightBounds.of(ResourceManager.turret_chekhov, "Base", placement, pos);
        for (int i = 0; i < CONNECTORS; i++) {
            connectorPoses[i] =
                    new Matrix4f(placement)
                            .rotateY(PLUG_ROT[i] * Mth.DEG_TO_RAD)
                            .translate(0F, 0F, PLUG_OZ[i]);
            connectors[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, CONNECTOR_PART.model())
                            .createInstance();
        }
        carriage =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CARRIAGE_PART.model())
                        .createInstance();
        gun =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, GUN_PART.model())
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
        boolean maskChanged = !initialized || mask != lastConnectorMask;
        boolean mechanicalChanged =
                !initialized
                        || Float.compare(yawRadians, lastYaw) != 0
                        || Float.compare(pitchRadians, lastPitch) != 0;
        if (!maskChanged && !mechanicalChanged) return;
        if (maskChanged)
            for (int i = 0; i < CONNECTORS; i++) {
                boolean present = (mask & (1 << i)) != 0;
                connectors[i].setVisible(present);
                if (present) write(connectors[i], connectorPoses[i]);
            }
        lastConnectorMask = mask;
        lastYaw = yawRadians;
        lastPitch = pitchRadians;
        initialized = true;
        carriagePose.set(placement).rotateY(yawRadians - 90F * Mth.DEG_TO_RAD);
        gunPose.set(carriagePose)
                .translate(0F, 1.5F, 0F)
                .rotateZ(pitchRadians)
                .translate(0F, -1.5F, 0F);
        write(carriage, carriagePose);
        write(gun, gunPose);
        LightBounds.resetBounds(lightBoundsAccumulator, baseBounds);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator, CARRIAGE_PART.model(), carriagePose, pos);
        LightBounds.includeLightBounds(lightBoundsAccumulator, GUN_PART.model(), gunPose, pos);
        for (int i = 0; i < CONNECTORS; i++)
            if ((mask & (1 << i)) != 0)
                LightBounds.includeLightBounds(
                        lightBoundsAccumulator, CONNECTOR_PART.model(), connectorPoses[i], pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
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
        for (TransformedInstance connector : connectors) consumer.accept(connector);
        consumer.accept(carriage);
        consumer.accept(gun);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance connector : connectors) connector.delete();
        carriage.delete();
        gun.delete();
    }
}
