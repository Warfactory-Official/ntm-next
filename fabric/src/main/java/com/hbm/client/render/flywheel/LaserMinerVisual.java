// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.BlockEntityMachineMiningLaser;
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

public final class LaserMinerVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineMiningLaser>
        implements ShaderLightVisual {
    private static final double STANDOFF = 1.5D;
    private static final int BEAM_COLOR = 0xA00000;
    private static final HFRWavefrontObject MODEL = ResourceManager.mining_laser;
    private static final MeshPart PIVOT_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Pivot")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.mining_laser_pivot_tex));
    private static final MeshPart LASER_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Laser")],
                    MODEL.smoothing(),
                    MeshPart.litCutout(ResourceManager.mining_laser_laser_tex));
    private final AABB bodyBounds;
    private final TransformedInstance pivot;
    private final TransformedInstance laser;
    private final BeamVisual[] beams = new BeamVisual[3];
    private final Matrix4f pivotPose = new Matrix4f();
    private final Matrix4f laserPose = new Matrix4f();
    private final Matrix4f beamPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private Vec3 beamVector = Vec3.ZERO;
    private Vec3 beamStandoff = Vec3.ZERO;
    private int lastRange = Integer.MIN_VALUE;
    private int lastStart = Integer.MIN_VALUE;
    private double lastTargetX = Double.NaN;
    private double lastTargetY = Double.NaN;
    private double lastTargetZ = Double.NaN;
    private boolean lastBeam;
    private boolean initialized;

    public LaserMinerVisual(
            VisualizationContext context,
            BlockEntityMachineMiningLaser blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        bodyBounds = LightBounds.of(MODEL, "Base", new Matrix4f().translation(.5F, -1F, .5F), pos);
        pivot =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, PIVOT_PART.model())
                        .createInstance();
        laser =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LASER_PART.model())
                        .createInstance();
        for (int i = 0; i < beams.length; i++)
            beams[i] = new BeamVisual(context, level, pos, false, 3, 1F);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        double tx = Mth.lerp(partialTick, blockEntity.lastTargetX, blockEntity.targetX);
        double ty = Mth.lerp(partialTick, blockEntity.lastTargetY, blockEntity.targetY);
        double tz = Mth.lerp(partialTick, blockEntity.lastTargetZ, blockEntity.targetZ);
        boolean beamActive = blockEntity.beam;
        int start = beamActive ? (int) (level.getGameTime() * -25L % 360L) : 0;
        boolean targetChanged =
                !initialized || tx != lastTargetX || ty != lastTargetY || tz != lastTargetZ;
        boolean beamChanged =
                !initialized || beamActive != lastBeam || beamActive && start != lastStart;
        if (initialized && !targetChanged && !beamChanged) return;
        lastTargetX = tx;
        lastTargetY = ty;
        lastTargetZ = tz;
        lastBeam = beamActive;
        lastStart = start;
        if (targetChanged) {
            double vx = tx - pos.getX();
            double vy = ty - pos.getY() + 3D;
            double vz = tz - pos.getZ();
            Vec3 direction = new Vec3(vx, vy, vz);
            beamStandoff = direction.normalize().scale(STANDOFF);
            beamVector = new Vec3(vx - beamStandoff.x, vy - beamStandoff.y, vz - beamStandoff.z);
            float yaw = (float) Math.toDegrees(Math.atan2(beamVector.x, beamVector.z));
            double flat = Math.sqrt(beamVector.x * beamVector.x + beamVector.z * beamVector.z);
            float pitch = (float) Math.toDegrees(Math.atan2(beamVector.y, flat));
            pivotPose.identity().translate(.5F, -1F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
            laserPose
                    .set(pivotPose)
                    .translate(0F, -1F, 0F)
                    .rotateX(-(pitch + 90F) * Mth.DEG_TO_RAD)
                    .translate(0F, 1F, 0F);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(pivotPose);
            pivot.setTransform(instancePose).light(0).setChanged();
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(laserPose);
            laser.setTransform(instancePose).light(0).setChanged();
            lastRange = (int) Math.ceil(beamVector.length() * .5D);
        }

        if (!beamActive) {
            for (var beam : beams) beam.hide();
            initialized = true;
            return;
        }
        int range = lastRange;
        beamPose.identity()
                .translate(
                        .5F + (float) beamStandoff.x,
                        -2F + (float) beamStandoff.y,
                        .5F + (float) beamStandoff.z);
        for (int i = 0; i < beams.length; i++) {
            beams[i].update(
                    beamPose,
                    beamVector,
                    EnumWaveType.SPIRAL,
                    start + i * 120,
                    range * 2,
                    .075F,
                    .025F,
                    BEAM_COLOR,
                    BEAM_COLOR);
        }
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(2).expandTowards(0, -2, 0));
    }

    @Override
    protected AABB visibleBounds() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 32,
                                level.getMinY(),
                                pos.getZ() - 32,
                                pos.getX() + 33,
                                pos.getY() + 4,
                                pos.getZ() + 33)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(pivot);
        consumer.accept(laser);
    }

    @Override
    protected void _delete() {
        pivot.delete();
        laser.delete();
        for (var beam : beams) beam.delete();
    }
}
