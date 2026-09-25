// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.turret.BlockEntityTurretMaxwell;
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

public final class TurretMaxwellVisual extends HbmDynamicBlockEntityVisual<BlockEntityTurretMaxwell>
        implements ShaderLightVisual {
    private static final int CONNECTORS = 8;
    private static final float[] PLUG_ROT = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] PLUG_OZ = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};
    private static final int CARRIAGE = ResourceManager.turret_howard.partId("Carriage");
    private static final int MICROWAVE = ResourceManager.turret_maxwell.partId("Microwave");
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_howard.groups[CARRIAGE],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_carriage_ciws_tex));
    private static final MeshPart MICROWAVE_PART =
            MeshPart.obj(
                    ResourceManager.turret_maxwell.groups[MICROWAVE],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_maxwell_tex));
    private final TransformedInstance[] connectors = new TransformedInstance[CONNECTORS];
    private final TransformedInstance carriage;
    private final TransformedInstance microwave;
    private final BeamVisual[] beams = new BeamVisual[8];
    private final Matrix4f placement = new Matrix4f();
    private final Matrix4f[] connectorPoses = new Matrix4f[CONNECTORS];
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f microwavePose = new Matrix4f();
    private final Matrix4f beamBasePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final AABB bodyBounds;
    private final double[] lightBounds = new double[6];
    private @Nullable AABB lastLightBounds;
    private Vec3 beamDirection = new Vec3(0D, 0D, 0D);
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private int lastConnectorMask;
    private double lastBeamLength = Double.NaN;
    private double lastBarrelLength = Double.NaN;
    private int lastBeamSegments;
    private boolean lastBeamActive;
    private boolean initialized;
    private boolean extentBeam;
    private double extentDistance = Double.NaN;

    public TurretMaxwellVisual(
            VisualizationContext context, BlockEntityTurretMaxwell blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var offset = blockEntity.getHorizontalOffset();
        placement.translation((float) offset.x, 0F, (float) offset.z);
        bodyBounds = LightBounds.of(ResourceManager.turret_chekhov, "Base", placement, pos);
        for (int i = 0; i < CONNECTORS; i++) {
            connectors[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, CONNECTOR_PART.model())
                            .createInstance();
            connectorPoses[i] =
                    new Matrix4f(placement)
                            .rotateY((PLUG_ROT[i]) * Mth.DEG_TO_RAD)
                            .translate(0F, 0F, PLUG_OZ[i]);
        }
        carriage =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CARRIAGE_PART.model())
                        .createInstance();
        microwave =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, MICROWAVE_PART.model())
                        .createInstance();
        for (int i = 0; i < beams.length; i++)
            beams[i] = new BeamVisual(context, level, pos, false, 2, .1F);
        trackExtent();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void trackExtent() {
        boolean beam = blockEntity.beam > 0;
        double distance = blockEntity.lastDist;
        if (beam == extentBeam && (!beam || distance == extentDistance)) return;
        extentBeam = beam;
        extentDistance = distance;
        refreshVisibleBounds();
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float yaw =
                (float)
                        -Math.toDegrees(
                                Mth.lerp(
                                        partialTick,
                                        blockEntity.lastRotationYaw,
                                        blockEntity.rotationYaw));
        float pitch =
                (float)
                        Math.toDegrees(
                                Mth.lerp(
                                        partialTick,
                                        blockEntity.lastRotationPitch,
                                        blockEntity.rotationPitch));
        int mask = blockEntity.connectorMask;
        boolean connectorChanged = !initialized || mask != lastConnectorMask;
        if (connectorChanged)
            for (int i = 0; i < CONNECTORS; i++) {
                boolean present = (mask & (1 << i)) != 0;
                connectors[i].setVisible(present);
                if (present) write(connectors[i], connectorPoses[i]);
            }
        boolean yawChanged =
                !initialized || Float.floatToIntBits(yaw) != Float.floatToIntBits(lastYaw);
        boolean pitchChanged =
                !initialized || Float.floatToIntBits(pitch) != Float.floatToIntBits(lastPitch);
        if (yawChanged) {
            carriagePose.set(placement).rotateY((yaw - 90F) * Mth.DEG_TO_RAD);
            write(carriage, carriagePose);
        }
        if (yawChanged || pitchChanged) {
            microwavePose
                    .set(carriagePose)
                    .translate(0F, 1.5F, 0F)
                    .rotateZ((pitch) * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F);
            write(microwave, microwavePose);
        }

        boolean beamActive = blockEntity.beam > 0;
        double barrelLength = blockEntity.getBarrelLength();
        double length = blockEntity.lastDist - barrelLength;
        int segments = (int) (blockEntity.lastDist + 1D);
        boolean beamGeometryChanged =
                !initialized
                        || beamActive != lastBeamActive
                        || Double.doubleToLongBits(length)
                                != Double.doubleToLongBits(lastBeamLength)
                        || Double.doubleToLongBits(barrelLength)
                                != Double.doubleToLongBits(lastBarrelLength)
                        || segments != lastBeamSegments;
        if (beamActive) {
            if (yawChanged || pitchChanged || beamGeometryChanged) {
                beamBasePose.set(microwavePose).translate((float) barrelLength, 2F, 0F);
            }
            if (beamGeometryChanged) beamDirection = new Vec3(length, 0D, 0D);
            for (int i = 0; i < beams.length; i++) {
                int start = (int) ((level.getGameTime() + partialTick) * -50F + i * 45F) % 360;
                beams[i].update(
                        beamBasePose,
                        beamDirection,
                        EnumWaveType.SPIRAL,
                        start,
                        segments,
                        .375F,
                        .05F,
                        0x2020FF,
                        0x2020FF);
            }
        } else if (!initialized || lastBeamActive) {
            for (BeamVisual beam : beams) beam.hide();
        }
        if (!connectorChanged && !yawChanged && !pitchChanged && !beamGeometryChanged) {
            lastBeamActive = beamActive;
            return;
        }
        LightBounds.resetBounds(lightBounds, bodyBounds);
        for (int i = 0; i < connectors.length; i++)
            if ((mask & (1 << i)) != 0)
                LightBounds.includeLightBounds(
                        lightBounds, CONNECTOR_PART.model(), connectorPoses[i], pos);
        LightBounds.includeLightBounds(lightBounds, CARRIAGE_PART.model(), carriagePose, pos);
        LightBounds.includeLightBounds(lightBounds, MICROWAVE_PART.model(), microwavePose, pos);
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
        lastYaw = yaw;
        lastPitch = pitch;
        lastConnectorMask = mask;
        lastBeamLength = length;
        lastBarrelLength = barrelLength;
        lastBeamSegments = segments;
        lastBeamActive = beamActive;
        initialized = true;
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
    protected AABB visibleBounds() {
        AABB lit = getRenderBoundingBox();
        return extentBeam ? lit.minmax(new AABB(pos).inflate(extentDistance + 3D)) : lit;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance instance : connectors) consumer.accept(instance);
        consumer.accept(carriage);
        consumer.accept(microwave);
    }

    @Override
    protected void _delete() {
        for (var connector : connectors) connector.delete();
        carriage.delete();
        microwave.delete();
        for (BeamVisual beam : beams) beam.delete();
    }
}
