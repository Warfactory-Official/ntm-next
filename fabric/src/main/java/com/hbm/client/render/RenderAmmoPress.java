// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineAmmoPress.AnimationState;
import com.hbm.tileentity.machine.BlockEntityMachineAmmoPress;
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
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderAmmoPress
        implements BlockEntityRenderer<BlockEntityMachineAmmoPress, RenderAmmoPress.State>,
                ConcurrentRenderStateExtraction {
    private static final int PRESS = ResourceManager.ammo_press.partId("Press");
    private static final int SHELLS = ResourceManager.ammo_press.partId("Shells");
    private static final int BULLETS = ResourceManager.ammo_press.partId("Bullets");

    private final HFRWavefrontObject model;
    private final RenderType bodyType;

    public RenderAmmoPress() {
        this.model = ResourceManager.ammo_press;
        this.bodyType = WorldRenderPipeline.oneSidedCutout(ResourceManager.ammo_press_tex);
    }

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
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineAmmoPress be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = facingYaw(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.press = Mth.lerp(partialTicks, be.prevPress, be.press);
        state.lift = Mth.lerp(partialTicks, be.prevLift, be.lift);
        state.showBullets =
                be.animState == AnimationState.RETRACTING
                        || be.animState == AnimationState.LOWERING;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        ps.translate(0.0, -s.press * 0.25, 0.0);
        part(col, ps, light, PRESS);
        ps.popPose();

        ps.pushPose();
        ps.translate(0.0, s.lift * 0.5 - 0.5, 0.0);
        part(col, ps, light, SHELLS);
        if (s.showBullets) part(col, ps, light, BULLETS);
        ps.popPose();

        ps.popPose();
    }

    private void part(SubmitNodeCollector col, PoseStack ps, int light, int name) {
        col.submitCustomGeometry(
                ps, bodyType, (pose, buffer) -> model.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float press;
        public float lift;
        public boolean showBullets;
    }
}
