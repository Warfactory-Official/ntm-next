// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.MachineStirling;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityStirling;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderStirling
        implements BlockEntityRenderer<BlockEntityStirling, RenderStirling.State>,
                ConcurrentRenderStateExtraction {

    private static final int COG = ResourceManager.stirling.partId("Cog");
    private static final int COG_SMALL = ResourceManager.stirling.partId("CogSmall");
    private static final int PISTON = ResourceManager.stirling.partId("Piston");

    private final HFRWavefrontObject model;
    private final RenderType[] bodyTypes;

    public RenderStirling() {
        this.model = ResourceManager.stirling;
        this.bodyTypes =
                new RenderType[] {
                    RenderTypes.entityCutoutCull(ResourceManager.stirling_tex),
                    RenderTypes.entityCutoutCull(ResourceManager.stirling_steel_tex),
                    RenderTypes.entityCutoutCull(ResourceManager.stirling_creative_tex)
                };
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 180);
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
    public AABB getRenderBoundingBox(BlockEntityStirling be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 3,
                pos.getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityStirling be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.rot = Mth.lerp(partialTicks, be.lastSpin, be.spin);
        state.hasCog = be.hasCog;
        state.skin = ((MachineStirling) be.getBlockState().getBlock()).tier().gear;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        RenderType type = bodyTypes[s.skin];
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        if (s.hasCog) {
            ps.pushPose();
            ps.translate(0.0, 1.375, 0.0);
            ps.mulPose(Axis.ZP.rotationDegrees(-s.rot));
            ps.translate(0.0, -1.375, 0.0);
            part(col, ps, type, light, COG);
            ps.popPose();
        }

        ps.pushPose();
        ps.translate(0.0, 1.375, 0.25);
        ps.mulPose(Axis.XP.rotationDegrees(s.rot * 2F + 3F));
        ps.translate(0.0, -1.375, -0.25);
        part(col, ps, type, light, COG_SMALL);
        ps.popPose();

        ps.translate(Math.sin(s.rot * Math.PI / 90D) * 0.25 + 0.125, 0.0, 0.0);
        part(col, ps, type, light, PISTON);

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, RenderType type, int light, int name) {
        col.submitCustomGeometry(
                ps, type, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float rot;
        public boolean hasCog;
        public int skin;
    }
}
