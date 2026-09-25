// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.ModBlocks;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.storage.BlockEntityFileCabinet;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFileCabinet
        implements BlockEntityRenderer<BlockEntityFileCabinet, RenderFileCabinet.State>,
                ConcurrentRenderStateExtraction {
    private static final int LOWER_DRAWER = ResourceManager.file_cabinet.partId("LowerDrawer");
    private static final int UPPER_DRAWER = ResourceManager.file_cabinet.partId("UpperDrawer");

    private static final float DRAWER_SLIDE = 0.6875F;

    private final HFRWavefrontObject model;
    private final RenderType greenType;
    private final RenderType steelType;

    public RenderFileCabinet() {
        this.model = ResourceManager.file_cabinet;
        this.greenType = RenderTypes.entityCutoutCull(ResourceManager.file_cabinet_tex);
        this.steelType = RenderTypes.entityCutoutCull(ResourceManager.file_cabinet_steel_tex);
    }

    private static float cabinetYaw(Direction facing) {
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
    public AABB getRenderBoundingBox(BlockEntityFileCabinet be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 1,
                pos.getZ() + 2);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityFileCabinet be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        BlockState block = be.getBlockState();
        state.facing = block.getValue(HorizontalDirectionalBlock.FACING);
        state.steel = block.is(ModBlocks.FILING_CABINET_STEEL.get());
        state.lower = Mth.lerp(partialTicks, be.prevLowerExtent, be.lowerExtent);
        state.upper = Mth.lerp(partialTicks, be.prevUpperExtent, be.upperExtent);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        RenderType type = s.steel ? steelType : greenType;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(cabinetYaw(s.facing)));

        ps.pushPose();
        ps.translate(0.0, 0.0, DRAWER_SLIDE * s.lower);
        part(col, ps, type, light, LOWER_DRAWER);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0, 0.0, DRAWER_SLIDE * s.upper);
        part(col, ps, type, light, UPPER_DRAWER);
        ps.popPose();

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, RenderType type, int light, int name) {
        final HFRWavefrontObject m = this.model;
        col.submitCustomGeometry(
                ps, type, (pose, buffer) -> m.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public boolean steel;
        public float lower;
        public float upper;
    }
}
