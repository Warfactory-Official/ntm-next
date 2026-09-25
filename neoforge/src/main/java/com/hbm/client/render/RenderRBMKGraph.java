// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.machine.rbmk.RBMKMiniPanelBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph.GraphUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGraph;
import com.hbm.util.BobMathUtil;
import com.hbm.util.Facing;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRBMKGraph
        implements BlockEntityRenderer<BlockEntityRBMKGraph, RenderRBMKGraph.State>,
                ConcurrentRenderStateExtraction {
    static final RenderPipeline PLOT_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                            .withLocation("pipeline/ntm_rbmk_graph_plot")
                            .withColorTargetState(ColorTargetState.DEFAULT));

    private static final RenderType PLOT =
            RenderType.create(
                    "ntm_rbmk_graph_plot", RenderSetup.builder(PLOT_PIPELINE).createRenderSetup());

    private static final float LINE_WIDTH = 2F;
    private static final int PLOT_COLOR = ARGB.opaque(0x00ff00);
    public static final double LINE_SCALE = 0.0025D;

    private final Font font;

    public RenderRBMKGraph(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    private static void emitPlot(
            PoseStack.Pose pose, VertexConsumer buf, long[] values, long lowest, long highest) {
        long range = highest - lowest;
        float dx = 0.03225F;
        for (int v = 0; v < values.length - 1; v++) {
            float y0 = plotY(values[v], lowest, highest, range);
            float z0 = plotZ(v, values.length);
            float y1 = plotY(values[v + 1], lowest, highest, range);
            float z1 = plotZ(v + 1, values.length);

            float ny = y1 - y0, nz = z1 - z0;
            float len = Math.max(Math.abs(ny) + Math.abs(nz), 1.0E-4F);
            Vertices.emitLine(
                    buf, pose, dx, y0, z0, PLOT_COLOR, 0F, ny / len, nz / len, LINE_WIDTH);
            Vertices.emitLine(
                    buf, pose, dx, y1, z1, PLOT_COLOR, 0F, ny / len, nz / len, LINE_WIDTH);
        }
    }

    private static float plotY(long value, long lowest, long highest, long range) {
        long flux = value;
        if (flux < lowest) flux = lowest;
        if (flux > highest) flux = highest;
        return (float) (0.5D - 0.03125D + (flux - lowest) * 0.1875D / Math.max(range, 1));
    }

    private static float plotZ(int k, int count) {
        return (float) (0.375D - k * 0.75D / (count - 1));
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityRBMKGraph be) {
        return new AABB(be.getBlockPos()).inflate(1.0D);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntityRBMKGraph be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.yaw = Facing.yaw(be.getBlockState().getValue(RBMKMiniPanelBase.FACING), 90);

        for (int i = 0; i < BlockEntityRBMKGraph.GRAPHS; i++) {
            GraphUnit unit = be.graphs[i];
            Plot out = state.plots[i];
            out.active = unit.active;
            if (!unit.active) continue;

            out.values = unit.values.clone();
            out.lowest = unit.minBound ? unit.min : BobMathUtil.min(unit.values);
            out.highest = unit.maxBound ? unit.max : BobMathUtil.max(unit.values);
            out.lower = Component.literal("" + out.lowest).getVisualOrderText();
            out.upper = Component.literal("" + out.highest).getVisualOrderText();
            out.label =
                    unit.label == null || unit.label.isEmpty()
                            ? null
                            : Component.literal(unit.label).getVisualOrderText();
        }
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yaw));

        int light = state.lightCoords;

        for (int i = 0; i < BlockEntityRBMKGraph.GRAPHS; i++) {
            Plot plot = state.plots[i];
            if (!plot.active || plot.values == null) continue;

            poseStack.pushPose();
            poseStack.translate(0.25, i * -0.5 + 0.25, 0.0);

            collector.submitCustomGeometry(
                    poseStack,
                    Sheets.SHELL,
                    (pose, buffer) ->
                            ResourceManager.rbmk_numitron.render(pose, buffer, light, -1));

            long[] values = plot.values;
            long lowest = plot.lowest;
            long highest = plot.highest;
            collector.submitCustomGeometry(
                    poseStack,
                    PLOT,
                    (pose, buffer) -> emitPlot(pose, buffer, values, lowest, highest));

            poseStack.pushPose();
            poseStack.translate(0.032, 0.5 - 0.03125 * 1.5, -0.375 + 0.03125);
            poseStack.scale((float) LINE_SCALE, (float) -LINE_SCALE, (float) LINE_SCALE);
            poseStack.mulPose(Axis.YP.rotationDegrees(90F));
            collector.submitText(
                    poseStack,
                    -font.width(plot.lower),
                    -font.lineHeight / 2,
                    plot.lower,
                    false,
                    Font.DisplayMode.NORMAL,
                    LightCoordsUtil.FULL_BRIGHT,
                    PLOT_COLOR,
                    0,
                    0);
            poseStack.translate(0.0, -0.03125 * 7 / LINE_SCALE, 0.0);
            collector.submitText(
                    poseStack,
                    -font.width(plot.upper),
                    -font.lineHeight / 2,
                    plot.upper,
                    false,
                    Font.DisplayMode.NORMAL,
                    LightCoordsUtil.FULL_BRIGHT,
                    PLOT_COLOR,
                    0,
                    0);
            poseStack.popPose();

            if (plot.label != null) {
                poseStack.translate(0.01, 0.3125, 0.0);
                int width = font.width(plot.label);
                float f3 = Math.min(0.0125F, 0.75F / Math.max(width, 1));
                poseStack.scale(f3, -f3, f3);
                poseStack.mulPose(Axis.YP.rotationDegrees(90F));
                collector.submitText(
                        poseStack,
                        -width / 2,
                        -font.lineHeight / 2,
                        plot.label,
                        false,
                        Font.DisplayMode.NORMAL,
                        LightCoordsUtil.FULL_BRIGHT,
                        PLOT_COLOR,
                        0,
                        0);
            }
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    public static final class Plot {
        public boolean active;
        public long @Nullable [] values;
        public long lowest;
        public long highest;
        public FormattedCharSequence lower = FormattedCharSequence.EMPTY;
        public FormattedCharSequence upper = FormattedCharSequence.EMPTY;
        public @Nullable FormattedCharSequence label;
    }

    public static final class State extends BlockEntityRenderState {
        public final Plot[] plots = new Plot[BlockEntityRBMKGraph.GRAPHS];
        public float yaw;

        public State() {
            for (int i = 0; i < plots.length; i++) plots[i] = new Plot();
        }
    }

    private static final class Sheets {
        static final RenderType SHELL = RenderTypes.entitySolid(ResourceManager.rbmk_numitron_tex);
    }
}
