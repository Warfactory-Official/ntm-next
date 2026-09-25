// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionTorus;
import com.hbm.util.GameTime;
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
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderFusionTorus
        implements BlockEntityRenderer<BlockEntityFusionTorus, RenderFusionTorus.State>,
                ConcurrentRenderStateExtraction {
    private static final int MAGNET = ResourceManager.fusion_torus.partId("Magnet");
    private static final int PLASMA = ResourceManager.fusion_torus.partId("Plasma");

    private static final double EXTRA_LAYER_RANGE_SQ = 100 * 100;

    private static final int[] BOLTS =
            ResourceManager.fusion_torus.partIds("Bolts2", "Bolts4", "Bolts3", "Bolts1");

    private final HFRWavefrontObject model = ResourceManager.fusion_torus;
    private final RenderType baseType =
            RenderTypes.entityCutoutCull(ResourceManager.fusion_torus_tex);
    private final RenderType plasmaType =
            FusionPlasmaRenderTypes.plasma(ResourceManager.fusion_plasma_tex);
    private final RenderType glowType =
            FusionPlasmaRenderTypes.plasma(ResourceManager.fusion_plasma_glow_tex);
    private final RenderType sparkleType =
            FusionPlasmaRenderTypes.plasma(ResourceManager.fusion_plasma_sparkle_tex);

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static int color(float r, float g, float b, float a) {
        return ARGB.colorFromFloat(
                Mth.clamp(a, 0F, 1F),
                Mth.clamp(r, 0F, 1F),
                Mth.clamp(g, 0F, 1F),
                Mth.clamp(b, 0F, 1F));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityFusionTorus be) {
        BlockPos pos = be.getBlockPos();
        double floor = be.isTilted() ? -1.3125 : 0;
        return new AABB(
                pos.getX() - 8,
                pos.getY() + floor,
                pos.getZ() - 8,
                pos.getX() + 9,
                pos.getY() + 5,
                pos.getZ() + 9);
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
            BlockEntityFusionTorus be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);

        state.tilted = be.isTilted();
        state.magnet = Mth.lerp(partialTicks, be.prevMagnet, be.magnet);
        System.arraycopy(be.connections, 0, state.connections, 0, state.connections.length);

        FusionRecipe recipe = be.module.getRecipe() instanceof FusionRecipe f ? f : null;
        state.burning = be.plasmaEnergy > 0 && recipe != null;
        if (state.burning) {
            state.r = recipe.r;
            state.g = recipe.g;
            state.b = recipe.b;
        }

        long offset = Math.floorMod(be.getBlockPos().hashCode(), 30_000);
        state.timeMs = GameTime.now() + offset;

        double dx = be.getBlockPos().getX() + 0.5 - cameraPosition.x;
        double dy = be.getBlockPos().getY() + 2.5 - cameraPosition.y;
        double dz = be.getBlockPos().getZ() + 0.5 - cameraPosition.z;
        state.nearby = dx * dx + dy * dy + dz * dz < EXTRA_LAYER_RANGE_SQ;
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        if (s.tilted) {
            ps.translate(0.0, -1.0, 0.0);
            ps.mulPose(Axis.ZP.rotationDegrees(10F));
            ps.mulPose(Axis.YP.rotationDegrees(5F));
        }

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.magnet));
        part(col, ps, baseType, light, -1, MAGNET);
        ps.popPose();

        for (int i = 0; i < BOLTS.length; i++) {
            if (s.connections[i]) part(col, ps, baseType, light, -1, BOLTS[i]);
        }

        if (s.burning) submitPlasma(s, ps, col);

        ps.popPose();
    }

    private void submitPlasma(State s, PoseStack ps, SubmitNodeCollector col) {
        double time = s.timeMs;
        float alpha = 0.35F + (float) (Math.sin(time / 1000D) * 0.25F);

        double mainOsc = sps(time / 1000D) % 1D;
        double glowOsc = Math.sin(time / 2000D) % 1D;
        double glowExtra = time / 10000D % 1D;
        double sparkleSpin = time / 500D * -1 % 1D;
        double sparkleOsc = Math.sin(time / 1000D) * 0.5D % 1D;

        int light = 0xF000F0;

        plasmaLayer(col, ps, plasmaType, light, color(s.r, s.g, s.b, alpha), 0F, (float) mainOsc);

        if (!s.nearby) return;

        plasmaLayer(
                col,
                ps,
                glowType,
                light,
                color(s.r * 2F, s.g * 2F, s.b * 2F, alpha * 2F),
                0F,
                (float) (glowOsc + glowExtra));
        plasmaLayer(
                col,
                ps,
                sparkleType,
                light,
                color(s.r * 2F, s.g * 2F, s.b * 2F, 0.75F),
                (float) sparkleSpin,
                (float) sparkleOsc);
    }

    private void plasmaLayer(
            SubmitNodeCollector col,
            PoseStack ps,
            RenderType type,
            int light,
            int color,
            float uOff,
            float vOff) {
        col.submitCustomGeometry(
                ps,
                type,
                (pose, buffer) ->
                        model.renderPart(pose, buffer, light, color, PLASMA, 1F, 1F, uOff, vOff));
    }

    private void part(
            SubmitNodeCollector col,
            PoseStack ps,
            RenderType type,
            int light,
            int color,
            int name) {
        col.submitCustomGeometry(
                ps, type, (pose, buffer) -> model.renderPart(pose, buffer, light, color, name));
    }

    public static final class State extends BlockEntityRenderState {
        public final boolean[] connections = new boolean[4];
        public boolean tilted;
        public float magnet;
        public boolean burning;
        public float r;
        public float g;
        public float b;
        public long timeMs;
        public boolean nearby;
    }
}
