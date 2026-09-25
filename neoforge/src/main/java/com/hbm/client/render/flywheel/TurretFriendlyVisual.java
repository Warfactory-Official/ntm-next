// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.turret.BlockEntityTurretFriendly;
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

public final class TurretFriendlyVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityTurretFriendly>
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
                    MeshPart.litCutout(ResourceManager.turret_carriage_friendly_tex));
    private static final MeshPart BODY_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Body")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_chekhov_tex));
    private static final MeshPart BARRELS_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Barrels")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_chekhov_barrels_tex));
    private final TransformedInstance[] connectors = new TransformedInstance[CONNECTORS];
    private final TransformedInstance carriage;
    private final TransformedInstance body;
    private final TransformedInstance barrels;
    private final Matrix4f placement = new Matrix4f();
    private final Matrix4f[] connectorPoses = new Matrix4f[CONNECTORS];
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f bodyPose = new Matrix4f();
    private final Matrix4f barrelsPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBounds = new double[6];
    private final AABB baseBounds;
    private @Nullable AABB lastLightBounds;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private float lastSpin = Float.NaN;
    private int lastConnectorMask;
    private boolean initialized;

    public TurretFriendlyVisual(
            VisualizationContext context,
            BlockEntityTurretFriendly blockEntity,
            float partialTick) {
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
        body =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, BODY_PART.model())
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
        int mask = blockEntity.connectorMask;
        boolean connectorChanged = !initialized || mask != lastConnectorMask;
        if (connectorChanged) {
            for (int i = 0; i < CONNECTORS; i++) {
                boolean present = (mask & (1 << i)) != 0;
                if (initialized && present == ((lastConnectorMask & (1 << i)) != 0)) continue;
                connectors[i].setVisible(present);
                if (present) write(connectors[i], connectorPoses[i]);
            }
        }

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
        float spin = Mth.lerp(partialTick, blockEntity.lastSpin, blockEntity.spin);
        boolean yawChanged =
                !initialized || Float.floatToIntBits(yawRadians) != Float.floatToIntBits(lastYaw);
        boolean pitchChanged =
                !initialized
                        || Float.floatToIntBits(pitchRadians) != Float.floatToIntBits(lastPitch);
        boolean spinChanged =
                !initialized || Float.floatToIntBits(spin) != Float.floatToIntBits(lastSpin);
        if (!connectorChanged && !yawChanged && !pitchChanged && !spinChanged) return;
        if (yawChanged) {
            carriagePose.set(placement).rotateY(yawRadians - 90F * Mth.DEG_TO_RAD);
            write(carriage, carriagePose);
        }
        if (yawChanged || pitchChanged) {
            bodyPose.set(carriagePose)
                    .translate(0F, 1.5F, 0F)
                    .rotateZ(pitchRadians)
                    .translate(0F, -1.5F, 0F);
            write(body, bodyPose);
        }
        if (yawChanged || pitchChanged || spinChanged) {
            barrelsPose
                    .set(bodyPose)
                    .translate(0F, 1.5F, 0F)
                    .rotateX(-spin * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F);
            write(barrels, barrelsPose);
        }
        LightBounds.resetBounds(lightBounds, baseBounds);
        for (int i = 0; i < CONNECTORS; i++) {
            boolean present = (mask & (1 << i)) != 0;
            if (present)
                LightBounds.includeLightBounds(
                        lightBounds, CONNECTOR_PART.model(), connectorPoses[i], pos);
        }
        LightBounds.includeLightBounds(lightBounds, CARRIAGE_PART.model(), carriagePose, pos);
        LightBounds.includeLightBounds(lightBounds, BODY_PART.model(), bodyPose, pos);
        LightBounds.includeLightBounds(lightBounds, BARRELS_PART.model(), barrelsPose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastYaw = yawRadians;
        lastPitch = pitchRadians;
        lastSpin = spin;
        lastConnectorMask = mask;
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
        for (TransformedInstance connector : connectors) consumer.accept(connector);
        consumer.accept(carriage);
        consumer.accept(body);
        consumer.accept(barrels);
    }

    @Override
    protected void _delete() {
        for (int i = 0; i < CONNECTORS; i++) connectors[i].delete();
        carriage.delete();
        body.delete();
        barrels.delete();
    }
}
