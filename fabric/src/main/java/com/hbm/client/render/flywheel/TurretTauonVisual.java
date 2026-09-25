// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.turret.BlockEntityTurretTauon;
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

public final class TurretTauonVisual extends HbmDynamicBlockEntityVisual<BlockEntityTurretTauon>
        implements ShaderLightVisual {
    private static final int CONNECTORS = 8;
    private static final float[] PLUG_ROT = {0F, 0F, 90F, 90F, 180F, 180F, 270F, 270F};
    private static final float[] PLUG_OZ = {0F, -1F, -1F, 0F, -1F, 0F, -1F, 0F};
    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int CANNON = ResourceManager.turret_tauon.partId("Cannon");
    private static final int ROTOR = ResourceManager.turret_tauon.partId("Rotor");
    private static final MeshPart CONNECTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov
                            .groups[ResourceManager.turret_chekhov.partId("Connectors")],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_connector_tex));
    private static final MeshPart CARRIAGE_PART =
            MeshPart.obj(
                    ResourceManager.turret_chekhov.groups[CARRIAGE],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_carriage_tex));
    private static final MeshPart CANNON_PART =
            MeshPart.obj(
                    ResourceManager.turret_tauon.groups[CANNON],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_tauon_tex));
    private static final MeshPart ROTOR_PART =
            MeshPart.obj(
                    ResourceManager.turret_tauon.groups[ROTOR],
                    true,
                    MeshPart.litCutout(ResourceManager.turret_tauon_tex));
    private final TransformedInstance[] connectors = new TransformedInstance[CONNECTORS];
    private final TransformedInstance carriage;
    private final TransformedInstance cannon;
    private final TransformedInstance rotor;
    private final BeamVisual beam;
    private final Matrix4f placement = new Matrix4f();
    private final Matrix4f[] connectorPoses = new Matrix4f[CONNECTORS];
    private final Matrix4f carriagePose = new Matrix4f();
    private final Matrix4f cannonPose = new Matrix4f();
    private final Matrix4f rotorPose = new Matrix4f();
    private final Matrix4f beamPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private Vec3 beamVector = Vec3.ZERO;
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB rawBodyBounds;
    private @Nullable AABB lastLightBounds;
    private int lastConnectorMask = Integer.MIN_VALUE;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;
    private float lastSpin = Float.NaN;
    private long lastBeamTick = Long.MIN_VALUE;
    private double lastBeamDistance = Double.NaN;
    private boolean lastBeam;
    private boolean initialized;
    private boolean extentBeam;
    private double extentDistance = Double.NaN;

    public TurretTauonVisual(
            VisualizationContext context, BlockEntityTurretTauon blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        var rawOffset = blockEntity.getHorizontalOffset();
        placement.translation((float) rawOffset.x, 0F, (float) rawOffset.z);
        rawBodyBounds =
                new AABB(pos)
                        .minmax(
                                LightBounds.of(
                                        ResourceManager.turret_chekhov, "Base", placement, pos));
        for (int i = 0; i < CONNECTORS; i++) {
            connectorPoses[i] =
                    new Matrix4f(placement)
                            .rotateY((PLUG_ROT[i]) * Mth.DEG_TO_RAD)
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
        cannon =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, CANNON_PART.model())
                        .createInstance();
        rotor =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, ROTOR_PART.model())
                        .createInstance();
        beam = new BeamVisual(context, level, pos, true, 1, 1F);
        trackExtent();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void trackExtent() {
        boolean active = blockEntity.beam > 0;
        double distance = blockEntity.lastDist;
        if (active == extentBeam && (!active || distance == extentDistance)) return;
        extentBeam = active;
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
        float spin = Mth.lerp(partialTick, blockEntity.lastSpin, blockEntity.spin);
        boolean maskChanged = !initialized || mask != lastConnectorMask;
        boolean mechanicalChanged =
                !initialized
                        || Float.compare(yaw, lastYaw) != 0
                        || Float.compare(pitch, lastPitch) != 0
                        || Float.compare(spin, lastSpin) != 0;
        boolean beamActive = blockEntity.beam > 0;
        long beamTick = level.getGameTime() / 5L;
        double beamDistance = blockEntity.lastDist;
        double previousBeamDistance = lastBeamDistance;
        boolean beamChanged =
                !initialized
                        || beamActive != lastBeam
                        || beamActive
                                && (mechanicalChanged
                                        || beamTick != lastBeamTick
                                        || Double.compare(beamDistance, lastBeamDistance) != 0);
        if (!maskChanged && !mechanicalChanged && !beamChanged) return;
        if (maskChanged)
            for (int i = 0; i < CONNECTORS; i++) {
                boolean present = (mask & (1 << i)) != 0;
                connectors[i].setVisible(present);
                if (present) write(connectors[i], connectorPoses[i]);
            }
        lastConnectorMask = mask;
        lastYaw = yaw;
        lastPitch = pitch;
        lastSpin = spin;
        lastBeam = beamActive;
        lastBeamTick = beamTick;
        lastBeamDistance = beamDistance;
        initialized = true;
        if (mechanicalChanged) {
            carriagePose.set(placement).rotateY((yaw - 90F) * Mth.DEG_TO_RAD);
            cannonPose
                    .set(carriagePose)
                    .translate(0F, 1.5F, 0F)
                    .rotateZ((pitch) * Mth.DEG_TO_RAD)
                    .translate(0F, -1.5F, 0F);
            rotorPose
                    .set(cannonPose)
                    .translate(0F, 1.375F, 0F)
                    .rotateX(-(spin) * Mth.DEG_TO_RAD)
                    .translate(0F, -1.375F, 0F);
            write(carriage, carriagePose);
            write(cannon, cannonPose);
            write(rotor, rotorPose);
        }
        if (mechanicalChanged || maskChanged) {
            LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, CARRIAGE_PART.model(), carriagePose, pos);
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, CANNON_PART.model(), cannonPose, pos);
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, ROTOR_PART.model(), rotorPose, pos);
            for (int i = 0; i < CONNECTORS; i++)
                if ((mask & (1 << i)) != 0)
                    LightBounds.includeLightBounds(
                            lightBoundsAccumulator, CONNECTOR_PART.model(), connectorPoses[i], pos);
            lastLightBounds =
                    LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
        }
        if (beamChanged) {
            if (beamActive) {
                beamPose.set(cannonPose).translate(0F, 1.5F, 0F);
                if (Double.compare(beamDistance, previousBeamDistance) != 0
                        || beamVector == Vec3.ZERO) beamVector = new Vec3(beamDistance, 0D, 0D);
                beam.update(
                        beamPose,
                        beamVector,
                        EnumWaveType.RANDOM,
                        (int) (beamTick % 360L),
                        (int) beamDistance + 1,
                        .1F,
                        0F,
                        0xFFA200,
                        0xFFD000);
            } else beam.hide();
        }
    }

    private void write(TransformedInstance instance, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(new AABB(pos).inflate(8));
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
        consumer.accept(cannon);
        consumer.accept(rotor);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance connector : connectors) connector.delete();
        carriage.delete();
        cannon.delete();
        rotor.delete();
        beam.delete();
    }
}
