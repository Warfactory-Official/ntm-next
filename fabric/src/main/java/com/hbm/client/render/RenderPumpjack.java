// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.oil.BlockEntityMachinePumpjack;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderPumpjack
        implements BlockEntityRenderer<BlockEntityMachinePumpjack, RenderPumpjack.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROTOR = ResourceManager.pumpjack.partId("Rotor");
    private static final int HEAD = ResourceManager.pumpjack.partId("Head");
    private static final int CARRIAGE = ResourceManager.pumpjack.partId("Carriage");

    private static final RenderType LINKAGE_TYPE = WorldRenderPipeline.UNTEXTURED;

    private final RenderType bodyType;

    public RenderPumpjack() {
        this.bodyType = RenderTypes.entitySolid(ResourceManager.pumpjack_tex);
    }

    private static void renderLinkage(
            PoseStack.Pose pose, VertexConsumer buf, float rotation, int light) {
        float[] backPos = {0F, 0F, -2F};
        rotateAroundX(backPos, -(float) Math.sin(Math.toRadians(rotation)) * 0.25F);

        float[] rotVec = {0F, 0.5F, 0F};
        rotateAroundX(rotVec, -(float) Math.toRadians(rotation - 90));

        int gray = ARGB.colorFromFloat(1F, 0.5F, 0.5F, 0.5F);
        for (int i = -1; i <= 1; i += 2) {
            float xi = 0.53125F * i;
            float y1 = 1.5F + rotVec[1], z1 = -5.5F + rotVec[2];
            float y2 = 3.5F + backPos[1], z2 = -3.5F + backPos[2];
            corner(buf, pose, xi, y1, z1 - 0.0625F, gray, light);
            corner(buf, pose, xi, y1, z1 + 0.0625F, gray, light);
            corner(buf, pose, xi, y2, z2 + 0.0625F, gray, light);
            corner(buf, pose, xi, y2, z2 - 0.0625F, gray, light);
        }

        int dark = ARGB.colorFromFloat(1F, 0.2F, 0.2F, 0.2F);
        float pd = 0.03125F;
        float width = 0.25F;
        float height = -(float) Math.sin(Math.toRadians(rotation));
        float cutlet = 360F / 32F;

        for (int i = -1; i <= 1; i += 2) {
            float pRot = -(float) (Math.sin(Math.toRadians(rotation)) * 0.25);

            float[] frontPos = {0F, 0F, 1F};
            rotateAroundX(frontPos, pRot);

            float dist = 0.03125F;
            float[] frontRad = {0F, 0F, 2.5F + dist};
            rotateAroundX(frontRad, pRot);
            rotateAroundX(frontRad, -(float) Math.toRadians(cutlet * -3));

            for (int j = 0; j < 4; j++) {
                float sumY1 = frontPos[1] + frontRad[1];
                float sumZ1 = frontRad[1] < 0 ? 3.5F + dist * 0.5F : frontPos[2] + frontRad[2];

                rotateAroundX(frontRad, -(float) Math.toRadians(cutlet));

                float sumY2 = frontPos[1] + frontRad[1];
                float sumZ2 = frontRad[1] < 0 ? 3.5F + dist * 0.5F : frontPos[2] + frontRad[2];

                float xL = (width - pd) * i, xR = (width + pd) * i;
                corner(buf, pose, xL, 3.5F + sumY1, -3.5F + sumZ1, dark, light);
                corner(buf, pose, xR, 3.5F + sumY1, -3.5F + sumZ1, dark, light);
                corner(buf, pose, xR, 3.5F + sumY2, -3.5F + sumZ2, dark, light);
                corner(buf, pose, xL, 3.5F + sumY2, -3.5F + sumZ2, dark, light);
            }

            float sumY = frontPos[1] + frontRad[1];
            float sumZ = frontRad[1] < 0 ? 3.5F + dist * 0.5F : frontPos[2] + frontRad[2];
            float xR = (width + pd) * i, xL = (width - pd) * i;
            corner(buf, pose, xR, 3.5F + sumY, -3.5F + sumZ, dark, light);
            corner(buf, pose, xL, 3.5F + sumY, -3.5F + sumZ, dark, light);
            corner(buf, pose, xL, 2F + height, 0F, dark, light);
            corner(buf, pose, xR, 2F + height, 0F, dark, light);
        }

        float p = 0.03125F;
        corner(buf, pose, p, height + 1.5F, p, dark, light);
        corner(buf, pose, -p, height + 1.5F, -p, dark, light);
        corner(buf, pose, -p, 0.75F, -p, dark, light);
        corner(buf, pose, p, 0.75F, p, dark, light);
        corner(buf, pose, -p, height + 1.5F, p, dark, light);
        corner(buf, pose, p, height + 1.5F, -p, dark, light);
        corner(buf, pose, p, 0.75F, -p, dark, light);
        corner(buf, pose, -p, 0.75F, p, dark, light);
    }

    private static void corner(
            VertexConsumer buf,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            int color,
            int light) {
        Vertices.emit(buf, pose, x, y, z, color, 0F, 0F, light, 0F, 0F, 1F);
    }

    private static void rotateAroundX(float[] v, float radians) {
        float c = Mth.cos(radians), s = Mth.sin(radians);
        float y = v[1] * c + v[2] * s;
        float z = v[2] * c - v[1] * s;
        v[1] = y;
        v[2] = z;
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
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
    public void extractRenderState(
            BlockEntityMachinePumpjack be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.rotation = Mth.lerp(pt, be.prevRot, be.rot);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        float rotation = s.rotation;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(facingYaw(s.facing)));

        ps.pushPose();
        ps.translate(0, 1.5, -5.5);
        ps.mulPose(Axis.XP.rotationDegrees(rotation - 90F));
        ps.translate(0, -1.5, 5.5);
        part(col, ps, light, ROTOR);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 3.5, -3.5);
        ps.mulPose(
                Axis.XP.rotationDegrees(
                        (float) Math.toDegrees(Math.sin(Math.toRadians(rotation))) * 0.25F));
        ps.translate(0, -3.5, 3.5);
        part(col, ps, light, HEAD);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, -Math.sin(Math.toRadians(rotation)), 0);
        part(col, ps, light, CARRIAGE);
        ps.popPose();

        col.submitCustomGeometry(
                ps, LINKAGE_TYPE, (pose, buffer) -> renderLinkage(pose, buffer, rotation, light));

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buffer) ->
                        ResourceManager.pumpjack.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float rotation;
    }
}
