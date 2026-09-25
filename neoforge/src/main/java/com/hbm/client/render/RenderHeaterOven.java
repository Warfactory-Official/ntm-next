// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityHeaterOven;
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
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderHeaterOven
        implements BlockEntityRenderer<BlockEntityHeaterOven, RenderHeaterOven.State>,
                ConcurrentRenderStateExtraction {
    private static final int DOOR = ResourceManager.heater_oven.partId("Door");
    private static final int INNER_BURNING = ResourceManager.heater_oven.partId("InnerBurning");
    private static final int INNER = ResourceManager.heater_oven.partId("Inner");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;
    private final RenderType hotType;

    public RenderHeaterOven() {
        this.model = ResourceManager.heater_oven;
        this.bodyType = RenderTypes.entityCutoutCull(ResourceManager.heater_oven_tex);

        this.hotType = FlatCutout.of(ResourceManager.heater_oven_tex);
    }

    private static float ovenYaw(Direction facing) {
        return Facing.yaw(facing, 180);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityHeaterOven be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1.1875,
                pos.getY(),
                pos.getZ() - 1.1875,
                pos.getX() + 2.1875,
                pos.getY() + 1,
                pos.getZ() + 2.1875);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            BlockEntityHeaterOven be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(be.getBlockState());
        state.doorAngle = Mth.lerp(partialTicks, be.prevDoorAngle, be.doorAngle);
        state.wasOn = be.wasOn;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        ps.mulPose(Axis.YP.rotationDegrees(ovenYaw(s.facing) - 90F));

        ps.pushPose();

        ps.translate(0.0, 0.0, s.doorAngle * 0.75 / 135.0);
        part(col, ps, bodyType, light, DOOR);
        ps.popPose();

        if (s.wasOn) {
            part(col, ps, hotType, LightCoordsUtil.FULL_BRIGHT, INNER_BURNING);
        } else {
            part(col, ps, bodyType, light, INNER);
        }

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, RenderType type, int light, int name) {
        final HFRWavefrontObject m = this.model;
        col.submitCustomGeometry(
                ps, type, (pose, buffer) -> m.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float doorAngle;
        public boolean wasOn;
    }
}
