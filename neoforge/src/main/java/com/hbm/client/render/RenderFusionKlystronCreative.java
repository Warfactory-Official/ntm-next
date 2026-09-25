// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionKlystronCreative;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFusionKlystronCreative
        implements BlockEntityRenderer<
                        BlockEntityFusionKlystronCreative, RenderFusionKlystronCreative.State>,
                ConcurrentRenderStateExtraction {

    private final HFRWavefrontObject model = ResourceManager.fusion_klystron;

    private final RenderType baseType =
            RenderTypes.entityCutoutCull(ResourceManager.fusion_klystron_creative_tex);

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
    public AABB getRenderBoundingBox(BlockEntityFusionKlystronCreative be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 4,
                pos.getY(),
                pos.getZ() - 4,
                pos.getX() + 5,
                pos.getY() + 5,
                pos.getZ() + 5);
    }

    @Override
    public void extractRenderState(
            BlockEntityFusionKlystronCreative be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = FusionYaw.of(BlockMultiblockCore.coreFacing(be.getBlockState()));
        state.fan = Mth.lerp(partialTicks, be.prevFan, be.fan);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        RenderFusionKlystron.submitRotor(s.yaw, s.fan, s.lightCoords, ps, col, model, baseType);
    }

    public static final class State extends BlockEntityRenderState {
        public float yaw;
        public float fan;
    }
}
