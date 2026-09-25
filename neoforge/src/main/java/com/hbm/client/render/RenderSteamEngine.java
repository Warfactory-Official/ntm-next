// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntitySteamEngine;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderSteamEngine
        implements BlockEntityRenderer<BlockEntitySteamEngine, RenderSteamEngine.State>,
                ConcurrentRenderStateExtraction {
    private static final int FLYWHEEL = ResourceManager.steam_engine.partId("Flywheel");
    private static final int SHAFT = ResourceManager.steam_engine.partId("Shaft");
    private static final int TRANSMISSION = ResourceManager.steam_engine.partId("Transmission");
    private static final int PISTON = ResourceManager.steam_engine.partId("Piston");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderSteamEngine() {
        this.model = ResourceManager.steam_engine;
        this.bodyType = WorldRenderPipeline.oneSidedCutout(ResourceManager.steam_engine_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 270);
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
    public AABB getRenderBoundingBox(BlockEntitySteamEngine be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 5,
                pos.getY(),
                pos.getZ() - 5,
                pos.getX() + 6,
                pos.getY() + 3,
                pos.getZ() + 6);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntitySteamEngine be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.rot = Mth.lerp(partialTicks, be.lastRotor, be.rotor);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        double rot = s.rot;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));
        ps.translate(2.0, 0.0, 0.0);

        ps.pushPose();
        ps.translate(2, 1.375, 0);
        ps.mulPose(Axis.ZN.rotationDegrees((float) rot));
        ps.translate(-2, -1.375, 0);
        part(col, ps, light, FLYWHEEL);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 1.375, -0.5);
        ps.mulPose(Axis.XP.rotationDegrees((float) (rot * 2D)));
        ps.translate(0, -1.375, 0.5);
        part(col, ps, light, SHAFT);
        ps.popPose();

        double sin = Math.sin(rot * Math.PI / 180D) * 0.25D - 0.25D;
        double cos = Math.cos(rot * Math.PI / 180D) * 0.25D;
        double ang = Math.acos(cos / 1.875D);
        ps.pushPose();
        ps.translate((float) sin, (float) cos, 0);
        ps.translate(2.25F, 1.375F, 0);
        ps.mulPose(Axis.ZN.rotationDegrees((float) (ang * 180D / Math.PI - 90D)));
        ps.translate(-2.25F, -1.375F, 0);
        part(col, ps, light, TRANSMISSION);
        ps.popPose();

        double cath = Math.sqrt(3.515625D - (cos * cos) / 2);
        ps.pushPose();
        ps.translate((float) (1.875D - cath + sin), 0, 0);
        part(col, ps, light, PISTON);
        ps.popPose();

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rot;
    }
}
