// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionMHDT;
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

public class RenderFusionMHDT
        implements BlockEntityRenderer<BlockEntityFusionMHDT, RenderFusionMHDT.State>,
                ConcurrentRenderStateExtraction {
    private static final int COILS = ResourceManager.fusion_mhdt.partId("Coils");

    private final HFRWavefrontObject model = ResourceManager.fusion_mhdt;

    private final RenderType baseType =
            RenderTypes.entityCutoutCull(ResourceManager.fusion_mhdt_tex);

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
    public AABB getRenderBoundingBox(BlockEntityFusionMHDT be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 7,
                pos.getY(),
                pos.getZ() - 7,
                pos.getX() + 8,
                pos.getY() + 4.3125,
                pos.getZ() + 8);
    }

    @Override
    public void extractRenderState(
            BlockEntityFusionMHDT be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = FusionYaw.of(BlockMultiblockCore.coreFacing(be.getBlockState()));

        state.rotor = Mth.lerp(partialTicks, be.prevRotor, be.rotor) % 15F;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.translate(0.0, 1.5, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(s.rotor));
        ps.translate(0.0, -1.5, 0.0);
        col.submitCustomGeometry(
                ps, baseType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, COILS));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rotor;
    }
}
