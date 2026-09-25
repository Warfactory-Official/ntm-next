// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.rail.RailStandardSwitch;
import com.hbm.blocks.rail.RailStandardSwitchFlipped;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.BlockEntityRail;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRail
        implements BlockEntityRenderer<BlockEntityRail, RenderRail.State>,
                ConcurrentRenderStateExtraction {

    private static final int SWITCH_SIGN_STRAIGHT =
            ResourceManager.rail_standard_switch.partId("SignStraight");
    private static final int SWITCH_SIGN_TURN =
            ResourceManager.rail_standard_switch.partId("SignTurn");
    private static final int FLIPPED_SIGN_STRAIGHT =
            ResourceManager.rail_standard_switch_flipped.partId("SignStraight");
    private static final int FLIPPED_SIGN_TURN =
            ResourceManager.rail_standard_switch_flipped.partId("SignTurn");

    private final RenderType signType =
            RenderTypes.entityCutoutCull(ResourceManager.rail_switch_sign_tex);
    private final RenderType signFlippedType =
            RenderTypes.entityCutoutCull(ResourceManager.rail_switch_sign_flipped_tex);

    private static float yawTurned(Direction facing) {
        return switch (facing) {
            case NORTH -> 180F;
            case WEST -> 270F;
            case EAST -> 90F;
            default -> 0F;
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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityRail be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        Block block = be.getBlockState().getBlock();
        state.sign = block instanceof RailStandardSwitch;
        state.flipped = block instanceof RailStandardSwitchFlipped;
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.switched = be.isSwitched;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        if (!s.sign) return;
        HFRWavefrontObject mesh =
                s.flipped
                        ? ResourceManager.rail_standard_switch_flipped
                        : ResourceManager.rail_standard_switch;
        int part =
                s.flipped
                        ? (s.switched ? FLIPPED_SIGN_TURN : FLIPPED_SIGN_STRAIGHT)
                        : (s.switched ? SWITCH_SIGN_TURN : SWITCH_SIGN_STRAIGHT);
        int light = s.lightCoords;
        Direction facing = s.facing;
        ps.pushPose();
        ps.translate(
                0.5 + facing.getClockWise().getStepX() * 0.5,
                0.0,
                0.5 + facing.getClockWise().getStepZ() * 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yawTurned(facing)));
        col.submitCustomGeometry(
                ps,
                s.flipped ? signFlippedType : signType,
                (pose, buf) -> mesh.renderPart(pose, buf, light, -1, part));
        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public boolean sign;
        public boolean flipped;
        public Direction facing = Direction.NORTH;
        public boolean switched;
    }
}
