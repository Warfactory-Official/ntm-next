// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.machine.BlockMachineHorizontal;
import com.hbm.client.render.RenderNukeFstbmb;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.bomb.BlockEntityNukeBalefire;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class NukeFstbmbVisual extends HbmDynamicBlockEntityVisual<BlockEntityNukeBalefire>
        implements ShaderLightVisual {
    private static final int GLINT_LAYERS = 2;
    private static final float GLINT_SPEED = 5F;
    private static final int GLINT_COLOR = ARGB.colorFromFloat(1F, 0F, .8F * .76F, .15F * .76F);
    private static final int UNLOADED = Integer.MIN_VALUE;
    private static final HFRWavefrontObject MODEL = ResourceManager.fstbmb;
    private static final Material GLINT_MATERIAL =
            SimpleMaterial.builderOf(Materials.GLINT)
                    .shaders(StandardMaterialShaders.DEFAULT)
                    .texture(ResourceManager.glint_bf_tex)
                    .blur(false)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .depthTest(DepthTest.EQUAL)
                    .writeMask(WriteMask.COLOR)
                    .build();
    private static final MeshPart GLINT_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Balefire")], MODEL.smoothing(), GLINT_MATERIAL);
    private final AABB bodyBounds;
    private final AffineUvTransformedInstance[] glints =
            new AffineUvTransformedInstance[GLINT_LAYERS];
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final WorldText clock =
            new WorldText(instancerProvider(), WorldText.Style.NORMAL, true);
    private final Matrix4f clockPose;
    private int clockSeconds = UNLOADED;
    private boolean glintsShown;

    public NukeFstbmbVisual(
            VisualizationContext context, BlockEntityNukeBalefire blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        float yaw = RenderNukeFstbmb.yawFor(blockState.getValue(BlockMachineHorizontal.FACING));
        basePose.translate(.5F, 0F, .5F).rotateY(yaw * Mth.DEG_TO_RAD);
        bodyBounds =
                new AABB(pos)
                        .inflate(1)
                        .minmax(LightBounds.of(MODEL, "Body", basePose, pos))
                        .minmax(LightBounds.of(MODEL, "Balefire", basePose, pos));
        PoseStack clockPoses = new PoseStack();
        clockPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
        clockPoses.translate(.5D, 0D, .5D);
        clockPoses.mulPose(Axis.YP.rotationDegrees(yaw));
        RenderNukeFstbmb.clockPose(clockPoses);
        clockPose = clockPoses.last().pose();
        for (int i = 0; i < glints.length; i++) {
            glints[i] =
                    instancerProvider()
                            .instancer(AffineUvTransformedInstance.TYPE, GLINT_PART.model())
                            .createInstance();
            glints[i].setVisible(false);
        }
        writeFrame(partialTick);
        writeClock();
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
        writeClock();
    }

    private void writeClock() {
        int seconds = blockEntity.loaded ? blockEntity.timer / 20 : UNLOADED;
        if (seconds != clockSeconds) {
            clockSeconds = seconds;
            clock.set(
                    seconds == UNLOADED
                            ? null
                            : Component.literal(
                                    blockEntity.getMinutes() + ":" + blockEntity.getSeconds()),
                    0F,
                    0F,
                    0xFFFF0000);
        }
        clock.write(clockPose);
    }

    private void writeFrame(float partialTick) {
        boolean loaded = blockEntity.loaded;
        if (!loaded) {
            if (!glintsShown) return;
            for (var glint : glints) glint.setVisible(false);
            glintsShown = false;
            return;
        }
        float offset = (float) (GameTime.millis(blockEntity.getLevel()) / 50D + partialTick);
        boolean reveal = !glintsShown;
        for (int i = 0; i < glints.length; i++) {
            float movement = offset * (.001F + i * .003F) * GLINT_SPEED;
            float angle = (float) Math.toRadians(30F - i * 60F);
            float cos = Mth.cos(angle), sin = Mth.sin(angle);
            if (reveal) {
                glints[i].setVisible(true);
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(basePose);
                glints[i].setTransform(instancePose).colorArgb(GLINT_COLOR).light(0);
            }
            glints[i].uv(
                    2F * cos,
                    -2F * sin,
                    2F * sin,
                    2F * cos,
                    -2F * sin * movement,
                    2F * cos * movement);
            glints[i].setChanged();
        }
        glintsShown = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(new AABB(pos).inflate(1D));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {}

    @Override
    protected void _delete() {
        for (var glint : glints) glint.delete();
        clock.delete();
    }
}
