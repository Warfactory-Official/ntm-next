// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineLPW2;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class LPW2Visual extends HbmDynamicBlockEntityVisual<BlockEntityMachineLPW2>
        implements ShaderLightVisual {
    private static final int CENTER = 0,
            ROTOR = 1,
            TURBINE_FRONT = 2,
            TURBINE_BACK = 3,
            PISTON = 4,
            ENGINE = 5,
            SHROUD_H = 6,
            SHROUD_V = 7,
            FLAP_1 = 8,
            FLAP_2 = 9,
            FLAP_3 = 10,
            FLAP_4 = 11,
            FLAP_5 = 12,
            FLAP_6 = 13,
            FLAP_7 = 14,
            FLAP_8 = 15,
            SUSPENSION_LEFT = 16,
            SUSPENSION_RIGHT = 17,
            SUSPENSION_TOP = 18,
            SUSPENSION_BOTTOM = 19,
            WIRE_LEFT = 20,
            WIRE_RIGHT = 21,
            COVER = 22,
            SUSPENSION_COVER_FRONT = 23,
            SUSPENSION_COVER_BACK = 24,
            SUSPENSION_BACK_OUTER = 25,
            SUSPENSION_BACK_CENTER = 26,
            SERVER_1 = 27,
            SERVER_2 = 28,
            SERVER_3 = 29,
            SERVER_4 = 30,
            MONITOR = 31,
            SCREEN = 32;
    private static final String[] NAMES = {
        "Center",
        "Rotor",
        "TurbineFront",
        "TurbineBack",
        "Piston",
        "Engine",
        "ShroudH",
        "ShroudV",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "Flap",
        "SuspensionLeft",
        "SuspensionRight",
        "SuspensionTop",
        "SuspensionBottom",
        "WireLeft",
        "WireRight",
        "Cover",
        "SuspensionCoverFront",
        "SuspensionCoverBack",
        "SuspensionBackOuter",
        "SuspensionBackCenter",
        "Server1",
        "Server2",
        "Server3",
        "Server4",
        "Monitor",
        "Screen"
    };
    private static final double SERVER_SWAY = .0625D * .25D;
    private static final HFRWavefrontObject MODEL = ResourceManager.lpw2;
    private static final Material BODY_MATERIAL = MeshPart.litCutout(ResourceManager.lpw2_tex);
    private static final Material SCREEN_MATERIAL =
            MeshPart.litCutout(ResourceManager.lpw2_error_tex);
    private static final MeshPart[] PARTS = buildParts();
    private static final MeshPart SCREEN_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId(NAMES[SCREEN])], MODEL.smoothing(), SCREEN_MATERIAL);
    private final AABB bodyBounds;
    private final TransformedInstance[] instances = new TransformedInstance[SCREEN];
    private final UvTransformedInstance screen;
    private final Matrix4f[] localPoses = new Matrix4f[NAMES.length];
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f[] previousPoses = new Matrix4f[NAMES.length];
    private final PoseStack poses = new PoseStack();
    private final float fixedYaw;
    private double lastSway = Double.NaN;
    private double lastH = Double.NaN;
    private double lastV = Double.NaN;
    private double lastPiston = Double.NaN;
    private double lastRotor = Double.NaN;
    private double lastTurbine = Double.NaN;
    private double lastCover = Double.NaN;
    private double lastSx = Double.NaN;
    private double lastSy = Double.NaN;
    private double lastErrorScroll = Double.NaN;
    private boolean initialized;

    public LPW2Visual(
            VisualizationContext context, BlockEntityMachineLPW2 blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        fixedYaw = Facing.yaw(blockState.getValue(BlockMultiblockCore.FACING), 90);
        Matrix4f bodyLocal =
                new Matrix4f().translation(.5F, 0F, .5F).rotateY(fixedYaw * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Frame", bodyLocal, pos);
        for (int i = 0; i < localPoses.length; i++) {
            localPoses[i] = new Matrix4f();
            previousPoses[i] = new Matrix4f();
        }
        for (int i = 0; i < SCREEN; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        screen =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, SCREEN_PART.model())
                        .createInstance();
        writeFrame(partialTick);
    }

    public static void initModels() {}

    private static MeshPart[] buildParts() {
        MeshPart[] parts = new MeshPart[SCREEN];
        for (int i = 0; i < parts.length; i++)
            parts[i] =
                    MeshPart.obj(
                            MODEL.groups[MODEL.partId(NAMES[i])], MODEL.smoothing(), BODY_MATERIAL);
        return parts;
    }

    private static boolean same(Matrix4f first, Matrix4f second) {
        return first.m00() == second.m00()
                && first.m01() == second.m01()
                && first.m02() == second.m02()
                && first.m03() == second.m03()
                && first.m10() == second.m10()
                && first.m11() == second.m11()
                && first.m12() == second.m12()
                && first.m13() == second.m13()
                && first.m20() == second.m20()
                && first.m21() == second.m21()
                && first.m22() == second.m22()
                && first.m23() == second.m23()
                && first.m30() == second.m30()
                && first.m31() == second.m31()
                && first.m32() == second.m32()
                && first.m33() == second.m33();
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        double time = GameTime.millis(blockEntity.getLevel()) / 50D + partialTick;
        double swayTimer = time / 3D % (Math.PI * 4D);
        double sway =
                (Math.sin(swayTimer)
                                + Math.sin(swayTimer * 2D)
                                + Math.sin(swayTimer * 4D)
                                + 2.23255D)
                        * .5D;
        double bellTimer = time / 5D % (Math.PI * 4D);
        double h = (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
        double v = (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
        double piston = BobMathUtil.sps(time / 5D % (Math.PI * 2D));
        double rotor =
                (BobMathUtil.sps(time / 5D % (Math.PI * 16D))
                                + time / 5D % (Math.PI * 16D) / 2D
                                - 1D)
                        / 25.1327412287D;
        double turbine = time % 100D / 100D;
        double coverTimer = time / 5D % (Math.PI * 4D);
        double cover =
                (Math.sin(coverTimer) + Math.sin(coverTimer * 2D) + Math.sin(coverTimer * 4D))
                        * .5D;
        double serverTimer = time / 2D % (Math.PI * 4D);
        double sx = (Math.sin(serverTimer + Math.PI) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
        double sy = (Math.sin(serverTimer) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
        double errorTimer = time / 3D;
        double errorScroll = (BobMathUtil.sps(errorTimer) + errorTimer / 2D) % 1D;

        boolean poseInputsChanged =
                !initialized
                        || sway != lastSway
                        || h != lastH
                        || v != lastV
                        || piston != lastPiston
                        || rotor != lastRotor
                        || turbine != lastTurbine
                        || cover != lastCover
                        || sx != lastSx
                        || sy != lastSy;
        if (!poseInputsChanged && errorScroll == lastErrorScroll) return;
        if (poseInputsChanged)
            prepareParts(fixedYaw, sway, h, v, piston, rotor, turbine, cover, sx, sy);
        for (int i = 0; i < SCREEN; i++) {
            if (initialized && same(localPoses[i], previousPoses[i])) continue;
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(localPoses[i]);
            instances[i].setTransform(instancePose).light(0).setChanged();
            previousPoses[i].set(localPoses[i]);
        }
        instancePose
                .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                .mul(localPoses[SCREEN]);
        screen.uvRegion(0F, (float) errorScroll, 1F, 1F);
        screen.setTransform(instancePose).light(0).setChanged();
        lastSway = sway;
        lastH = h;
        lastV = v;
        lastPiston = piston;
        lastRotor = rotor;
        lastTurbine = turbine;
        lastCover = cover;
        lastSx = sx;
        lastSy = sy;
        lastErrorScroll = errorScroll;
        initialized = true;
    }

    private void prepareParts(
            float yaw,
            double sway,
            double h,
            double v,
            double piston,
            double rotor,
            double turbine,
            double cover,
            double sx,
            double sy) {
        PoseStack ps = poses;
        ps.setIdentity();
        ps.pushPose();
        ps.translate(.5D, 0D, .5D);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));

        ps.pushPose();
        ps.translate(0D, 0D, -sway * .125D);
        part(ps, CENTER);
        ps.pushPose();
        ps.translate(0D, 3.5D, 0D);
        ps.mulPose(Axis.ZN.rotationDegrees((float) (rotor * 360D)));
        ps.translate(0D, -3.5D, 0D);
        part(ps, ROTOR);
        ps.popPose();
        ps.pushPose();
        ps.translate(0D, 3.5D, 0D);
        ps.mulPose(Axis.ZP.rotationDegrees((float) (turbine * 360D)));
        ps.translate(0D, -3.5D, 0D);
        part(ps, TURBINE_FRONT);
        ps.popPose();
        ps.pushPose();
        ps.translate(0D, 3.5D, 0D);
        ps.mulPose(Axis.ZN.rotationDegrees((float) (turbine * 360D)));
        ps.translate(0D, -3.5D, 0D);
        part(ps, TURBINE_BACK);
        ps.popPose();

        ps.pushPose();
        ps.translate(0D, 0D, piston * .375D + .375D);
        part(ps, PISTON);
        ps.popPose();
        ps.pushPose();
        ps.translate(0D, 3.5D, 2.75D);
        ps.mulPose(Axis.YP.rotationDegrees((float) (v * 2D)));
        ps.mulPose(Axis.XP.rotationDegrees((float) (h * 2D)));
        ps.translate(0D, -3.5D, -2.75D);
        part(ps, ENGINE);
        ps.popPose();
        ps.popPose();

        ps.pushPose();
        ps.translate(0D, -h * .125D, 0D);
        part(ps, SHROUD_H);
        flap(ps, FLAP_1, 112.5D, 5D * v + 10D);
        flap(ps, FLAP_2, 67.5D, 5D * v + 10D);
        flap(ps, FLAP_3, 292.5D, -5D * v + 10D);
        flap(ps, FLAP_4, 247.5D, -5D * v + 10D);
        ps.popPose();
        ps.pushPose();
        ps.translate(v * .125D, 0D, 0D);
        part(ps, SHROUD_V);
        flap(ps, FLAP_5, 22.5D, 5D * h + 10D);
        flap(ps, FLAP_6, -22.5D, 5D * h + 10D);
        flap(ps, FLAP_7, 202.5D, -5D * h + 10D);
        flap(ps, FLAP_8, 157.5D, -5D * h + 10D);
        ps.popPose();
        scale(
                ps,
                SUSPENSION_LEFT,
                -2.625D,
                0D,
                0D,
                (float) ((.6875D + v * .125D) / .6875D),
                1F,
                1F);
        scale(
                ps,
                SUSPENSION_RIGHT,
                2.625D,
                0D,
                0D,
                (float) ((.6875D - v * .125D) / .6875D),
                1F,
                1F);
        scale(ps, SUSPENSION_TOP, 0D, 6.125D, 0D, 1F, (float) ((.6875D + h * .125D) / .6875D), 1F);
        scale(
                ps,
                SUSPENSION_BOTTOM,
                0D,
                .875D,
                0D,
                1F,
                (float) ((.6875D - h * .125D) / .6875D),
                1F);
        ps.pushPose();
        ps.translate(-2.9375D, 0D, 2.375D);
        ps.mulPose(Axis.YP.rotationDegrees((float) (sway * 10D)));
        ps.translate(2.9375D, 0D, -2.375D);
        part(ps, WIRE_LEFT);
        ps.popPose();
        ps.pushPose();
        ps.translate(2.9375D, 0D, 2.375D);
        ps.mulPose(Axis.YP.rotationDegrees((float) (-sway * 10D)));
        ps.translate(-2.9375D, 0D, -2.375D);
        part(ps, WIRE_RIGHT);
        ps.popPose();
        ps.pushPose();
        ps.translate(0D, 0D, -cover * .125D);
        part(ps, COVER);
        ps.popPose();
        scale(
                ps,
                SUSPENSION_COVER_FRONT,
                0D,
                0D,
                3.5D,
                1F,
                1F,
                (float) ((3D + cover * .125D) / 3D));
        scale(
                ps,
                SUSPENSION_COVER_BACK,
                0D,
                0D,
                -5.5D,
                1F,
                1F,
                (float) ((1.5D - cover * .125D) / 1.5D));
        scale(
                ps,
                SUSPENSION_BACK_OUTER,
                0D,
                0D,
                -9D,
                1F,
                1F,
                (float) ((1.25D - sway * .125D) / 1.25D));
        scale(
                ps,
                SUSPENSION_BACK_CENTER,
                0D,
                0D,
                -9.5D,
                1F,
                1F,
                (float) ((1.75D - sway * .125D) / 1.75D));
        server(ps, SERVER_1, sx * SERVER_SWAY, sy * SERVER_SWAY);
        server(ps, SERVER_2, -sy * SERVER_SWAY, sx * SERVER_SWAY);
        server(ps, SERVER_3, sy * SERVER_SWAY, -sx * SERVER_SWAY);
        server(ps, SERVER_4, -sx * SERVER_SWAY, -sy * SERVER_SWAY);
        ps.pushPose();
        ps.translate(sy * SERVER_SWAY, 0D, sx * SERVER_SWAY);
        part(ps, MONITOR);
        part(ps, SCREEN);
        ps.popPose();
        ps.popPose();
    }

    private void part(PoseStack poses, int part) {
        localPoses[part].set(poses.last().pose());
    }

    private void flap(PoseStack poses, int part, double position, double rotation) {
        poses.pushPose();
        poses.translate(0D, 3.5D, 0D);
        poses.mulPose(Axis.ZP.rotationDegrees((float) position));
        poses.translate(0D, -3.5D, 0D);
        poses.translate(0D, 6.96875D, 8.5D);
        poses.mulPose(Axis.XP.rotationDegrees((float) rotation));
        poses.translate(0D, -6.96875D, -8.5D);
        part(poses, part);
        poses.popPose();
    }

    private void scale(
            PoseStack poses, int part, double x, double y, double z, float sx, float sy, float sz) {
        poses.pushPose();
        poses.translate(x, y, z);
        poses.scale(sx, sy, sz);
        poses.translate(-x, -y, -z);
        part(poses, part);
        poses.popPose();
    }

    private void server(PoseStack poses, int part, double x, double z) {
        poses.pushPose();
        poses.translate(x, 0D, z);
        part(poses, part);
        poses.popPose();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 10,
                                pos.getY(),
                                pos.getZ() - 10,
                                pos.getX() + 11,
                                pos.getY() + 7,
                                pos.getZ() + 11)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var instance : instances) consumer.accept(instance);
        consumer.accept(screen);
    }

    @Override
    protected void _delete() {
        for (var instance : instances) instance.delete();
        screen.delete();
    }
}
