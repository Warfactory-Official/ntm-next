// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachinePumpBase;
import com.hbm.tileentity.machine.BlockEntityMachinePumpElectric;
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

public class RenderPump
        implements BlockEntityRenderer<BlockEntityMachinePumpBase, RenderPump.State>,
                ConcurrentRenderStateExtraction {
    private static final int ROTOR = ResourceManager.pump.partId("Rotor");
    private static final int ARMS = ResourceManager.pump.partId("Arms");
    private static final int PISTON = ResourceManager.pump.partId("Piston");

    private final RenderType steamType;
    private final RenderType electricType;

    public RenderPump() {
        this.steamType = WorldRenderPipeline.oneSidedCutout(ResourceManager.pump_steam_tex);
        this.electricType = WorldRenderPipeline.oneSidedCutout(ResourceManager.pump_electric_tex);
    }

    private static float facingYaw(Direction facing) {
        return Facing.yaw(facing, 270);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityMachinePumpBase be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 5,
                pos.getZ() + 2);
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
            BlockEntityMachinePumpBase be,
            State state,
            float pt,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, pt, cameraPosition, breakProgress);
        state.facing = be.getBlockState().getValue(BlockMultiblockCore.FACING);
        state.rotation = Mth.lerp(pt, be.lastRotor, be.rotor);
        state.electric = be instanceof BlockEntityMachinePumpElectric;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        double rot = s.rotation;
        RenderType bodyType = s.electric ? electricType : steamType;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(facingYaw(s.facing)));

        ps.pushPose();
        ps.translate(0, 2.25, 0);
        ps.mulPose(Axis.ZP.rotationDegrees((float) (rot - 90)));
        ps.translate(0, -2.25, 0);
        part(col, ps, bodyType, light, ROTOR);
        ps.popPose();

        double sin = Math.sin(Math.toRadians(rot)) * 0.5 - 0.5;
        double cos = Math.cos(Math.toRadians(rot)) * 0.5;
        double ang = Math.acos(cos / 2.0);
        double cath = Math.sqrt(1 + (cos * cos) / 2);

        ps.pushPose();
        ps.translate(0, 1 - cath + sin, 0);
        ps.translate(0, 4.75, 0);
        ps.mulPose(Axis.ZN.rotationDegrees((float) (Math.toDegrees(ang) - 90)));
        ps.translate(0, -4.75, 0);
        part(col, ps, bodyType, light, ARMS);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, 1 - cath + sin, 0);
        part(col, ps, bodyType, light, PISTON);
        ps.popPose();

        ps.popPose();
    }

    private void part(
            SubmitNodeCollector col, PoseStack ps, RenderType bodyType, int light, int name) {
        col.submitCustomGeometry(
                ps,
                bodyType,
                (pose, buffer) -> ResourceManager.pump.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.WEST;
        public float rotation;
        public boolean electric;
    }
}
