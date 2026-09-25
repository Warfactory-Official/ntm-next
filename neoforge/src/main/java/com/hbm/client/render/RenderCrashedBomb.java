// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.bomb.BlockCrashedBomb.EnumDudType;
import com.hbm.blocks.bomb.BlockCrashedBomb;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.bomb.TileEntityCrashedBomb;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCrashedBomb
        implements BlockEntityRenderer<TileEntityCrashedBomb, RenderCrashedBomb.State>,
                ConcurrentRenderStateExtraction {

    private static int identity(BlockPos pos) {
        return (pos.getY() + pos.getZ() * 27644437) * 27644437 + pos.getX();
    }

    private static float lengthwise(EnumDudType type, float offset) {
        return -offset
                + switch (type) {
                    case NUKE -> 1.25F;
                    case SALTED -> 0.5F;
                    case BALEFIRE, CONVENTIONAL -> 0F;
                };
    }

    private static HFRWavefrontObject mesh(EnumDudType type) {
        return switch (type) {
            case BALEFIRE -> ResourceManager.dud_balefire;
            case CONVENTIONAL -> ResourceManager.dud_conventional;
            case NUKE -> ResourceManager.dud_nuke;
            case SALTED -> ResourceManager.dud_salted;
        };
    }

    private static Identifier sheet(EnumDudType type) {
        return switch (type) {
            case BALEFIRE -> ResourceManager.dud_balefire_tex;
            case CONVENTIONAL -> ResourceManager.dud_conventional_tex;
            case NUKE -> ResourceManager.dud_nuke_tex;
            case SALTED -> ResourceManager.dud_salted_tex;
        };
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
    public AABB getRenderBoundingBox(TileEntityCrashedBomb be) {
        return new AABB(be.getBlockPos()).inflate(6.0D);
    }

    @Override
    public void extractRenderState(
            TileEntityCrashedBomb be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.type =
                be.getBlockState().getBlock() instanceof BlockCrashedBomb bomb
                        ? bomb.type
                        : EnumDudType.BALEFIRE;

        Random random = new Random(identity(be.getBlockPos()));
        state.yaw = (float) (random.nextDouble() * 360D);
        state.pitch = (float) (random.nextDouble() * 45D + 45D);
        state.roll = (float) (random.nextDouble() * 360D);
        state.lengthwise = lengthwise(state.type, (float) (random.nextDouble() * 2D - 1D));
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        HFRWavefrontObject mesh = mesh(state.type);
        int light = state.lightCoords;

        poseStack.pushPose();

        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(state.pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.roll));
        poseStack.translate(0.0F, 0.0F, state.lengthwise);
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityCutoutCull(sheet(state.type)),
                (pose, buffer) -> mesh.render(pose, buffer, light, -1));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public EnumDudType type = EnumDudType.BALEFIRE;
        public float yaw;
        public float pitch;
        public float roll;
        public float lengthwise;
    }
}
