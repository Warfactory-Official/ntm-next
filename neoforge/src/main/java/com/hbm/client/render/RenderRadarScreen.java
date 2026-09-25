// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.api.entity.RadarEntry;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.gui.ScreenMachineRadar;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.BlockEntityMachineRadarScreen;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRadarScreen
        implements BlockEntityRenderer<BlockEntityMachineRadarScreen, RenderRadarScreen.State>,
                ConcurrentRenderStateExtraction {

    private static final float PLANE_X = 0.38F;

    private static final long BAR_PERIOD = 56L;
    private static final float BAR_SPEED = 30F;
    private static final float BAR_TOP = 2F;
    private static final float BAR_HEIGHT = 0.125F;

    private static final float FACE_MIN = -0.375F;
    private static final float FACE_MAX = 1.375F;

    private static final double SCOPE_SPAN = 0.875D;

    private static final double BLIP_HALF = 0.0625D;

    private static final int STATIC_V = 118;
    private static final int STATIC_ROWS = 81;
    private static final float STATIC_HEIGHT = 40F;

    private static final int BAR_TOP_COLOR = ARGB.color(0, 0, 255, 0);

    private static final int BAR_BOTTOM_COLOR = ARGB.color(50, 0, 255, 0);

    private final RenderType barType = FlatTranslucent.litPlain(ResourceManager.white_tex);
    private final RenderType blipType = FlatTranslucent.litCutout(ScreenMachineRadar.TEXTURE);

    private final RenderType staticType =
            WorldRenderPipeline.oneSidedCutout(ScreenMachineRadar.TEXTURE);

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineRadarScreen be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 2,
                pos.getZ() + 2);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineRadarScreen be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.yaw = Facing.yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING), 90);
        state.linked = be.linked;
        state.barOffset = ((be.getLevel().getGameTime() % BAR_PERIOD) + partialTicks) / BAR_SPEED;

        state.staticV = STATIC_V + ThreadLocalRandom.current().nextInt(STATIC_ROWS);

        state.blips.clear();
        for (RadarEntry entry : be.entries) {
            state.blips.add(
                    new Blip(
                            (entry.posX - be.refX) / ((double) be.range + 1) * SCOPE_SPAN,
                            (entry.posZ - be.refZ) / ((double) be.range + 1) * SCOPE_SPAN,
                            entry.blipLevel));
        }
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        if (s.linked) {
            float top = BAR_TOP - s.barOffset;
            col.order(0)
                    .submitCustomGeometry(
                            ps,
                            barType,
                            (pose, buf) -> {
                                emit(buf, pose, top, FACE_MAX, BAR_TOP_COLOR, light);
                                emit(buf, pose, top, FACE_MIN, BAR_TOP_COLOR, light);
                                emit(
                                        buf,
                                        pose,
                                        top - BAR_HEIGHT,
                                        FACE_MIN,
                                        BAR_BOTTOM_COLOR,
                                        light);
                                emit(
                                        buf,
                                        pose,
                                        top - BAR_HEIGHT,
                                        FACE_MAX,
                                        BAR_BOTTOM_COLOR,
                                        light);
                            });

            if (!s.blips.isEmpty()) {
                col.order(1)
                        .submitCustomGeometry(
                                ps,
                                blipType,
                                (pose, buf) -> {
                                    for (Blip blip : s.blips) {
                                        float y0 = (float) (1 - blip.z() - BLIP_HALF);
                                        float y1 = (float) (1 - blip.z() + BLIP_HALF);
                                        float z0 = (float) (0.5 - blip.x() - BLIP_HALF);
                                        float z1 = (float) (0.5 - blip.x() + BLIP_HALF);
                                        float v0 = blip.level() * 8F / 256F;
                                        float v1 = (blip.level() * 8F + 8F) / 256F;
                                        emit(buf, pose, y1, z1, 216F / 256F, v1, light);
                                        emit(buf, pose, y1, z0, 224F / 256F, v1, light);
                                        emit(buf, pose, y0, z0, 224F / 256F, v0, light);
                                        emit(buf, pose, y0, z1, 216F / 256F, v0, light);
                                    }
                                });
            }
        } else {
            float v0 = s.staticV / 256F;
            float v1 = (s.staticV + STATIC_HEIGHT) / 256F;
            col.submitCustomGeometry(
                    ps,
                    staticType,
                    (pose, buf) -> {
                        emit(buf, pose, 1.875F, FACE_MAX, 216F / 256F, v1, light);
                        emit(buf, pose, 1.875F, FACE_MIN, 256F / 256F, v1, light);
                        emit(buf, pose, 0.125F, FACE_MIN, 256F / 256F, v0, light);
                        emit(buf, pose, 0.125F, FACE_MAX, 216F / 256F, v0, light);
                    });
        }

        ps.popPose();
    }

    private static void emit(
            VertexConsumer buf, PoseStack.Pose pose, float y, float z, int color, int light) {
        Vertices.emit(buf, pose, PLANE_X, y, z, color, 0F, 0F, light, 0F, 1F, 0F);
    }

    private static void emit(
            VertexConsumer buf,
            PoseStack.Pose pose,
            float y,
            float z,
            float u,
            float v,
            int light) {
        Vertices.emit(buf, pose, PLANE_X, y, z, -1, u, v, light, 0F, 1F, 0F);
    }

    private record Blip(double x, double z, int level) {}

    public static final class State extends BlockEntityRenderState {
        public final List<Blip> blips = new ArrayList<>();
        public float yaw;
        public boolean linked;
        public float barOffset;
        public int staticV;
    }
}
