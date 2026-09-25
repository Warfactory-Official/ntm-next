// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.storage.BlockEntityMachineFENSU;
import com.hbm.util.Facing;
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
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFENSU
        implements BlockEntityRenderer<BlockEntityMachineFENSU, RenderFENSU.State>,
                ConcurrentRenderStateExtraction {

    private static final int DISC = ResourceManager.fensu.partId("Disc");
    private static final int LIGHTS = ResourceManager.fensu.partId("Lights");

    private final HFRWavefrontObject model = ResourceManager.fensu;
    private final RenderType discType = RenderTypes.entityCutoutCull(ResourceManager.fensu_tex);
    private final RenderType lightsType =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.fensu_tex);

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
    public AABB getRenderBoundingBox(BlockEntityMachineFENSU be) {
        return AABB.INFINITE;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineFENSU be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.rot = Mth.lerp(partialTicks, be.prevRotation, be.rotation);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.translate(0, 2.5, 0);
        ps.mulPose(Axis.XP.rotationDegrees(s.rot));
        ps.translate(0, -2.5, 0);

        col.submitCustomGeometry(
                ps, discType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, DISC));
        col.submitCustomGeometry(
                ps,
                lightsType,
                (pose, buffer) ->
                        model.renderPart(pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, LIGHTS));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rot;
    }
}
