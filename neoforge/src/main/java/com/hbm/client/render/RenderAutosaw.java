// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineAutosaw;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderAutosaw
        implements BlockEntityRenderer<BlockEntityMachineAutosaw, RenderAutosaw.State>,
                ConcurrentRenderStateExtraction {
    private static final int MAIN = ResourceManager.autosaw.partId("Main");
    private static final int ENGINE = ResourceManager.autosaw.partId("Engine");
    private static final int ARM_UPPER = ResourceManager.autosaw.partId("ArmUpper");
    private static final int ARM_LOWER = ResourceManager.autosaw.partId("ArmLower");
    private static final int ARM_TIP = ResourceManager.autosaw.partId("ArmTip");
    private static final int SAWBLADE = ResourceManager.autosaw.partId("Sawblade");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderAutosaw() {
        this.model = ResourceManager.autosaw;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.autosaw_tex);
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
    public AABB getRenderBoundingBox(BlockEntityMachineAutosaw be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 12,
                pos.getY(),
                pos.getZ() - 12,
                pos.getX() + 13,
                pos.getY() + 10,
                pos.getZ() + 13);
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineAutosaw be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.turn = Mth.lerp(partialTicks, be.prevRotationYaw, be.rotationYaw);
        state.angle = 80F - Mth.lerp(partialTicks, be.prevRotationPitch, be.rotationPitch);
        state.spin = Mth.lerp(partialTicks, be.lastSpin, be.spin);
        state.engine =
                be.isOn
                        ? (float)
                                Math.sin(
                                        (be.getLevel().getGameTime() * 2 % (2 * Math.PI))
                                                + partialTicks)
                        : 0F;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        ps.mulPose(Axis.YP.rotationDegrees(-s.turn));
        part(col, ps, light, MAIN);

        ps.pushPose();
        ps.translate(0.0, s.engine * 0.01, 0.0);
        part(col, ps, light, ENGINE);
        ps.popPose();

        ps.translate(0.0, 1.75, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(s.angle));
        ps.translate(0.0, -1.75, 0.0);
        part(col, ps, light, ARM_UPPER);

        ps.translate(0.0, 1.75, -4.0);
        ps.mulPose(Axis.XP.rotationDegrees(s.angle * -2F));
        ps.translate(0.0, -1.75, 4.0);
        ps.translate(-0.01, 0.0, 0.0);
        part(col, ps, light, ARM_LOWER);
        ps.translate(0.01, 0.0, 0.0);

        ps.translate(0.0, 1.75, -8.0);
        ps.mulPose(Axis.XP.rotationDegrees(s.angle));
        ps.translate(0.0, -1.75, 8.0);
        part(col, ps, light, ARM_TIP);

        ps.translate(0.0, 1.75, -10.0);
        ps.mulPose(Axis.YP.rotationDegrees(-s.spin));
        ps.translate(0.0, -1.75, 10.0);
        part(col, ps, light, SAWBLADE);

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float turn;
        public float angle;
        public float spin;
        public float engine;
    }
}
