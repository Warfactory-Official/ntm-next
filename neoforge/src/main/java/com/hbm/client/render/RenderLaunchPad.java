// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.bomb.BlockEntityLaunchPad;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderLaunchPad
        implements BlockEntityRenderer<BlockEntityLaunchPad, RenderLaunchPad.State>,
                ConcurrentRenderStateExtraction {

    private final Map<Item, PadMissiles.Missile> missiles = PadMissiles.table();

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
    public AABB getRenderBoundingBox(BlockEntityLaunchPad pad) {
        BlockPos pos = pad.getBlockPos();
        return new AABB(
                pos.getX() - 3.6875,
                pos.getY(),
                pos.getZ() - 3.6875,
                pos.getX() + 4.6875,
                pos.getY() + 15.4375,
                pos.getZ() + 4.6875);
    }

    @Override
    public void extractRenderState(
            BlockEntityLaunchPad pad,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                pad, state, partialTicks, camera, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(pad.getBlockState());
        state.missile = pad.loadedMissile == null ? null : missiles.get(pad.loadedMissile);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        PadMissiles.Missile missile = state.missile;
        if (missile == null) return;
        int light = state.lightCoords;
        poseStack.pushPose();
        poseStack.translate(.5D, 1D, .5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));
        if (missile.scale() != 1F)
            poseStack.scale(missile.scale(), missile.scale(), missile.scale());
        collector.submitCustomGeometry(
                poseStack,
                missile.type(),
                (pose, buffer) -> missile.mesh().render(pose, buffer, light, -1));
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        PadMissiles.@Nullable Missile missile;
    }
}
