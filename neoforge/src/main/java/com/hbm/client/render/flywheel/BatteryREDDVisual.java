// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.storage.BlockEntityBatteryREDD;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BatteryREDDVisual extends HbmDynamicBlockEntityVisual<BlockEntityBatteryREDD>
        implements ShaderLightVisual {
    private static final int WHEEL = ResourceManager.battery_redd.partId("Wheel");
    private static final int LIGHTS = ResourceManager.battery_redd.partId("Lights");
    private static final int PLASMA = ResourceManager.battery_redd.partId("Plasma");
    private static final double PIVOT = 5.5D;
    private static final double LEN = 4.25D;
    private static final double WIDTH = .125D;
    private static final double ARC_X = .8125D;
    private static final double SPARKLE_RANGE = 100D;
    private static final int[][] ARC_COLORS = {
        {0xBFFFFF00, 0xBFFFFF00, 0x7FFFFF00, 0x7FFFFF00},
        {0x7FFFFF00, 0x7FFFFF00, 0x3FFFFF00, 0x3FFFFF00},
        {0x3FFFFF00, 0x3FFFFF00, 0x00FFFF00, 0x00FFFF00}
    };
    private static final Model[] ARC_MODELS = arcModels();
    private static final HFRWavefrontObject MODEL = ResourceManager.battery_redd;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.battery_redd_tex);
    private static final Material FIXED_MATERIAL =
            SimpleMaterial.builderOf(BODY_MATERIAL)
                    .light(LightShaders.NONE)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .backfaceCulling(false)
                    .build();
    private static final Material GLOW_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.fusion_plasma_tex)
                    .mipmap(false)
                    .useLight(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(false)
                    .build();
    private static final Material SPARKLE_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.fusion_plasma_sparkle_tex)
                    .mipmap(false)
                    .useLight(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(false)
                    .build();
    private static final MeshPart WHEEL_PART =
            MeshPart.obj(MODEL.groups[WHEEL], MODEL.smoothing(), BODY_MATERIAL);
    private static final MeshPart LIGHTS_PART =
            MeshPart.obj(MODEL.groups[LIGHTS], MODEL.smoothing(), FIXED_MATERIAL);
    private static final MeshPart PLASMA_PART =
            MeshPart.obj(MODEL.groups[PLASMA], MODEL.smoothing(), GLOW_MATERIAL);
    private static final MeshPart SPARKLE_PART =
            MeshPart.obj(MODEL.groups[PLASMA], MODEL.smoothing(), SPARKLE_MATERIAL);
    private final AABB bodyBounds;
    private final TransformedInstance wheel;
    private final TransformedInstance lights;
    private final UvTransformedInstance plasma;
    private final UvTransformedInstance sparkle;
    private final TransformedInstance[] arcs = new TransformedInstance[48];
    private final BeamVisual[] zaps = new BeamVisual[8];
    private final Matrix4f wheelPose = new Matrix4f();
    private final Matrix4f wheelWorld = new Matrix4f();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f arcPose = new Matrix4f();
    private final Matrix4f zapCornerPose = new Matrix4f();
    private final Matrix4f local = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Vector3f p0 = new Vector3f(),
            p1 = new Vector3f(),
            p2 = new Vector3f(),
            p3 = new Vector3f();
    private final Random zapRandom = new Random();
    private final Vec3[] zapSkeletons = new Vec3[4];
    private float lastRotation = Float.NaN;
    private float lastSpeed = Float.NaN;
    private long lastTime = Long.MIN_VALUE;
    private boolean lastNear;
    private boolean arcsShown;
    private boolean initialized;

    public BatteryREDDVisual(
            VisualizationContext context, BlockEntityBatteryREDD blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        basePose.translation(.5F, 0F, .5F)
                .rotateY(
                        (Facing.yaw(
                                        BlockMultiblockCore.coreFacing(blockEntity.getBlockState()),
                                        270))
                                * Mth.DEG_TO_RAD);
        for (int corner = 0; corner < 4; corner++) {
            double x = (corner & 1) == 0 ? 3.125D : -3.125D;
            double z = corner < 2 ? 3.75D : -3.75D;
            zapSkeletons[corner] = new Vec3(-x / 3.125D * 1.375D, -2.625D, z);
        }

        bodyBounds = LightBounds.of(MODEL, "Base", basePose, pos);
        wheel =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, WHEEL_PART.model())
                        .createInstance();
        lights =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, LIGHTS_PART.model())
                        .createInstance();
        plasma =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, PLASMA_PART.model())
                        .createInstance();
        sparkle =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, SPARKLE_PART.model())
                        .createInstance();
        for (int i = 0; i < arcs.length; i++) {
            arcs[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, ARC_MODELS[i % 3])
                            .createInstance();
            arcs[i].setVisible(false);
        }
        for (int i = 0; i < zaps.length; i++)
            zaps[i] = new BeamVisual(context, level, pos, false, 3, .1F);
        writeFrame(partialTick, Vec3.ZERO);
    }

    public static void initModels() {}

    private static Model[] arcModels() {
        Model[] models = new Model[3];
        float[] data = {
            0F, 0F, 0F, 0F, 0F, 0F, 0F, 1F,
            1F, 0F, 0F, 1F, 0F, 0F, 0F, 1F,
            1F, 1F, 0F, 1F, 1F, 0F, 0F, 1F,
            0F, 1F, 0F, 0F, 1F, 0F, 0F, 1F
        };
        Material material =
                SimpleMaterial.builder()
                        .texture(ResourceManager.white_tex)
                        .mipmap(false)
                        .useLight(false)
                        .useOverlay(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                        .writeMask(WriteMask.COLOR)
                        .backfaceCulling(false)
                        .build();
        for (int i = 0; i < 3; i++) {
            models[i] =
                    new SingleMeshModel(
                            PackedQuadMesh.of(data, ARC_COLORS[i], new int[4]), material);
        }
        return models;
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick(), context.camera().position());
    }

    private void writeFrame(float partialTick, Vec3 camera) {
        float rotation = Mth.lerp(partialTick, blockEntity.prevRotation, blockEntity.rotation);
        float speed = blockEntity.getSpeed();
        long time = GameTime.now();
        boolean near =
                camera.distanceToSqr(pos.getX() + .5D, pos.getY() + 2.5D, pos.getZ() + .5D)
                        < SPARKLE_RANGE * SPARKLE_RANGE;
        boolean running = speed > 0F;
        boolean wheelChanged = !initialized || rotation != lastRotation;
        boolean speedChanged = !initialized || speed != lastSpeed;
        boolean effectChanged = speedChanged || near != lastNear || running && time != lastTime;
        if (!wheelChanged && !effectChanged) return;
        lastRotation = rotation;
        lastSpeed = speed;
        lastTime = time;
        lastNear = near;
        if (wheelChanged) {
            wheelPose
                    .set(basePose)
                    .translate(0F, (float) PIVOT, 0F)
                    .rotateX((rotation) * Mth.DEG_TO_RAD)
                    .translate(0F, (float) -PIVOT, 0F);
            wheelWorld
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(wheelPose);
            wheel.setTransform(wheelWorld).light(0).setChanged();
            lights.setTransform(wheelWorld).light(LightCoordsUtil.FULL_BRIGHT).setChanged();
        }
        float alphaMult = speed / 15F;
        plasma.setVisible(running);
        if (running) {
            float alpha = .45F + (float) Math.sin(time / 1000D) * .15F;
            plasma.setTransform(wheelWorld)
                    .colorArgb(ARGB.colorFromFloat(alpha * alphaMult, 1F, .25F, .75F))
                    .light(LightCoordsUtil.FULL_BRIGHT);
            plasma.uvRegion(0F, (float) (BobMathUtil.sps(time / 1000D) % 1D), 1F, 1F).setChanged();
        }
        sparkle.setVisible(running && near);
        if (running && near) {
            sparkle.setTransform(wheelWorld)
                    .colorArgb(ARGB.colorFromFloat(.75F * alphaMult, 1F, .5F, 1F))
                    .light(LightCoordsUtil.FULL_BRIGHT);
            sparkle.uvRegion(
                            (float) (time / 250D * -1D % 1D),
                            (float) (Math.sin(time / 1000D) * .5D % 1D),
                            1F,
                            1F)
                    .setChanged();
        }
        if (effectChanged) writeZaps(speed, time);
        if (wheelChanged || speedChanged) writeArcs(speed);
        initialized = true;
    }

    private void writeZaps(float speed, long time) {
        zapRandom.setSeed(level.getGameTime() / 5L);
        zapRandom.nextBoolean();
        int start = (int) (time % 1000L) / 50;
        int child = 0;
        for (int corner = 0; corner < 4; corner++) {
            boolean active = speed > 0F && zapRandom.nextBoolean();
            double x = (corner & 1) == 0 ? 3.125D : -3.125D;
            if (active) {
                zapCornerPose.set(basePose).translate((float) x, (float) PIVOT, 0F);
                zaps[child++].update(
                        zapCornerPose,
                        zapSkeletons[corner],
                        EnumWaveType.RANDOM,
                        start,
                        15,
                        .25F,
                        .0625F,
                        0x404040,
                        0x002040);
                zaps[child++].update(
                        zapCornerPose,
                        zapSkeletons[corner],
                        EnumWaveType.RANDOM,
                        start,
                        1,
                        0F,
                        .0625F,
                        0x404040,
                        0x002040);
            }
        }
        while (child < zaps.length) zaps[child++].hide();
    }

    private void writeArcs(float speed) {
        if (!(speed > 0F)) {
            if (arcsShown) for (TransformedInstance arc : arcs) arc.setVisible(false);
            arcsShown = false;
            return;
        }
        arcsShown = true;
        arcPose.set(wheelPose).translate(0F, (float) PIVOT, 0F);
        double span = speed * .75D;
        int index = 0;
        for (int side = -1; side <= 1; side += 2)
            for (int spoke = 0; spoke < 8; spoke++) {
                for (int segment = 0; segment < 3; segment++) {
                    double from = spoke * 45D + span * segment;
                    double to = from + span;
                    writeArc(index++, (float) (ARC_X * side), from, to);
                }
            }
    }

    private void writeArc(int index, float x, double from, double to) {
        double y0 = Math.cos(from * Mth.DEG_TO_RAD), z0 = -Math.sin(from * Mth.DEG_TO_RAD);
        double y1 = Math.cos(to * Mth.DEG_TO_RAD), z1 = -Math.sin(to * Mth.DEG_TO_RAD);
        p0.set(x, (float) (y0 * (LEN - WIDTH)), (float) (z0 * (LEN - WIDTH)));
        p1.set(x, (float) (y0 * (LEN + WIDTH)), (float) (z0 * (LEN + WIDTH)));
        p2.set(x, (float) (y1 * (LEN + WIDTH)), (float) (z1 * (LEN + WIDTH)));
        p3.set(x, (float) (y1 * (LEN - WIDTH)), (float) (z1 * (LEN - WIDTH)));
        float ax = p1.x - p0.x, ay = p1.y - p0.y, az = p1.z - p0.z;
        float bx = p3.x - p0.x, by = p3.y - p0.y, bz = p3.z - p0.z;
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (length == 0F) return;
        local.identity()
                .m00(ax)
                .m01(ay)
                .m02(az)
                .m10(bx)
                .m11(by)
                .m12(bz)
                .m20(nx / length)
                .m21(ny / length)
                .m22(nz / length)
                .m30(p0.x)
                .m31(p0.y)
                .m32(p0.z);
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(arcPose)
                .mul(local);
        arcs[index].setVisible(true);
        arcs[index].setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                        pos.getX() - 9,
                        pos.getY() - 1,
                        pos.getZ() - 9,
                        pos.getX() + 10,
                        pos.getY() + 8,
                        pos.getZ() + 10));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(wheel);
    }

    @Override
    protected void _delete() {
        wheel.delete();
        lights.delete();
        plasma.delete();
        sparkle.delete();
        for (TransformedInstance arc : arcs) arc.delete();
        for (BeamVisual zap : zaps) zap.delete();
    }
}
