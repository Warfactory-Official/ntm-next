// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineRadGen;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRadGen
        implements BlockEntityRenderer<BlockEntityMachineRadGen, RenderRadGen.State>,
                ConcurrentRenderStateExtraction {

    private static final int ROTOR = ResourceManager.radgen.partId("Rotor");
    private static final int LIGHT = ResourceManager.radgen.partId("Light");
    private static final int GLASS = ResourceManager.radgen.partId("Glass");

    private static final double PIVOT = 1.5D;

    private static final long SPIN_PERIOD = 3600L;
    private static final float SPIN_RATE = -0.1F;

    private static final int LAMP_ON = ARGB.colorFromFloat(1F, 0F, 1F, 0F);
    private static final int LAMP_OFF = ARGB.colorFromFloat(1F, 0F, 0.1F, 0F);

    private static final int GLASS_TINT = ARGB.colorFromFloat(0.3F, 0.5F, 0.75F, 1F);

    private final HFRWavefrontObject model = ResourceManager.radgen;

    private final RenderType bodyType =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.radgen_tex);

    private final RenderType lampType = FlatCutout.of(ResourceManager.white_tex);

    private final RenderType tintType = FlatTranslucent.lit(ResourceManager.white_tex);

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
            BlockEntityMachineRadGen be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        state.yaw = Facing.yaw(be.getBlockState().getValue(BlockMultiblockCore.FACING), 90);
        state.isOn = be.isOn;
        state.spin = (GameTime.now() % SPIN_PERIOD) * SPIN_RATE;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;

        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(s.yaw));

        ps.pushPose();
        if (s.isOn) {
            ps.translate(0.0, PIVOT, 0.0);
            ps.mulPose(Axis.XP.rotationDegrees(s.spin));
            ps.translate(0.0, -PIVOT, 0.0);
        }
        col.order(0)
                .submitCustomGeometry(
                        ps, bodyType, (pose, buf) -> model.renderPart(pose, buf, light, -1, ROTOR));
        ps.popPose();

        col.order(1)
                .submitCustomGeometry(
                        ps,
                        lampType,
                        (pose, buf) ->
                                model.renderPart(
                                        pose,
                                        buf,
                                        LightCoordsUtil.FULL_BRIGHT,
                                        s.isOn ? LAMP_ON : LAMP_OFF,
                                        LIGHT));

        col.order(2)
                .submitCustomGeometry(
                        ps,
                        tintType,
                        (pose, buf) -> model.renderPart(pose, buf, light, GLASS_TINT, GLASS));
        col.order(3)
                .submitCustomGeometry(
                        ps, bodyType, (pose, buf) -> model.renderPart(pose, buf, light, -1, GLASS));

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public boolean isOn;
        public float spin;
    }
}
