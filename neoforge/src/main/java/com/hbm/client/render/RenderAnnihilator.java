// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineAnnihilator;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderAnnihilator
        implements BlockEntityRenderer<BlockEntityMachineAnnihilator, RenderAnnihilator.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROLLER = ResourceManager.annihilator.partId("Roller");
    private static final int BELT = ResourceManager.annihilator.partId("Belt");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;
    private final RenderType beltType;

    public RenderAnnihilator(BlockEntityRendererProvider.Context context) {
        this.model = ResourceManager.annihilator;
        this.bodyType = RenderTypes.entitySolid(ResourceManager.annihilator_tex);
        this.beltType = RenderTypes.entitySolid(ResourceManager.annihilator_belt_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachineAnnihilator be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 5,
                pos.getY(),
                pos.getZ() - 5,
                pos.getX() + 6,
                pos.getY() + 8,
                pos.getZ() + 6);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineAnnihilator be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        long now = GameTime.now();
        state.rollerAngle = (float) (now * 0.15D % 360D);
        state.beltOffset = (float) -(now / 3000D % 1D);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(facingYaw(s.facing)));

        ps.pushPose();
        ps.translate(0.0, 1.75, 0.0);
        ps.mulPose(Axis.ZN.rotationDegrees(s.rollerAngle));
        ps.translate(0.0, -1.75, 0.0);
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, ROLLER));
        ps.popPose();

        col.submitCustomGeometry(
                ps,
                beltType,
                (pose, buffer) ->
                        model.renderPart(pose, buffer, light, -1, BELT, 1F, 1F, s.beltOffset, 0F));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float rollerAngle;
        public float beltOffset;
    }
}
