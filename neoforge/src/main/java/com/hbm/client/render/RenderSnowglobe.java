// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.blocks.generic.BlockSnowglobe;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.BlockEntitySnowglobe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

public class RenderSnowglobe
        implements BlockEntityRenderer<BlockEntitySnowglobe, RenderSnowglobe.State>,
                ConcurrentRenderStateExtraction {
    public static final int SOCKET_PART = ResourceManager.snowglobe.partId("Socket");
    public static final int GLASS_PART = ResourceManager.snowglobe.partId("Glass");
    public static final float SCALE = 0.0625F;
    public static final int LABEL_COLOR = 0xFFFFFFFF;

    private static final Map<SnowglobeType, Integer> FEATURE_PARTS =
            new EnumMap<>(SnowglobeType.class);
    private static final Quaternionfc TURN = Axis.YP.rotationDegrees(90);
    private static final RenderType SOCKET =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.snowglobe_tex);
    private static final RenderType GLASS =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.snowglobe_glass_tex);
    private static final RenderType FEATURES =
            WorldRenderPipeline.oneSidedCutout(ResourceManager.snowglobe_features_tex);

    static {
        FEATURE_PARTS.put(SnowglobeType.RIVETCITY, ResourceManager.snowglobe.partId("RivetCity"));
        FEATURE_PARTS.put(
                SnowglobeType.TENPENNYTOWER, ResourceManager.snowglobe.partId("TenpennyTower"));
        FEATURE_PARTS.put(SnowglobeType.LUCKY38, ResourceManager.snowglobe.partId("Lucky38"));
        FEATURE_PARTS.put(
                SnowglobeType.SIERRAMADRE, ResourceManager.snowglobe.partId("SierraMadre"));
        FEATURE_PARTS.put(SnowglobeType.PRYDWEN, ResourceManager.snowglobe.partId("Prydwen"));
    }

    private final Font font;

    public RenderSnowglobe(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    private RenderSnowglobe(Font font) {
        this.font = font;
    }

    public static RenderSnowglobe forItem() {
        return new RenderSnowglobe(Minecraft.getInstance().font);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            BlockEntitySnowglobe be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breakProgress);
        state.type = be.type;
        state.rotation = be.getBlockState().getValue(BlockSnowglobe.ROTATION);
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        ps.mulPose(Axis.YN.rotationDegrees(22.5F * s.rotation + 90.0F));
        renderSnowglobe(s.type, ps, col, s.lightCoords);
        ps.popPose();
    }

    public static int featurePart(SnowglobeType type) {
        Integer feature = FEATURE_PARTS.get(type);
        return feature == null ? -1 : feature;
    }

    public static Matrix4f labelBase(Matrix4f pose) {

        float f3 = 0.05F;
        return pose.translate(4.025F, 0.5F, 0F).scale(f3, -f3, f3);
    }

    public static Matrix4f labelWidth(Matrix4f pose, int width, int lineHeight) {
        return pose.translate(0F, -lineHeight / 2.0F, width * 0.5F)
                .rotate(TURN)
                .translate(0F, 1F, 0F);
    }

    public void renderSnowglobe(
            SnowglobeType type, PoseStack ps, SubmitNodeCollector col, int light) {
        ps.scale(SCALE, SCALE, SCALE);

        part(col, ps, SOCKET, light, SOCKET_PART);
        part(col, ps, GLASS, light, GLASS_PART);
        int feature = featurePart(type);
        if (feature >= 0) part(col, ps, FEATURES, light, feature);

        labelWidth(labelBase(ps.last().pose()), font.width(type.label), font.lineHeight);

        col.submitText(
                ps,
                0,
                0,
                FormattedCharSequence.forward(type.label, Style.EMPTY),
                false,
                Font.DisplayMode.NORMAL,
                LightCoordsUtil.FULL_BRIGHT,
                LABEL_COLOR,
                0,
                0);
    }

    private void part(SubmitNodeCollector col, PoseStack ps, RenderType type, int light, int name) {
        HFRWavefrontObject mesh = ResourceManager.snowglobe;
        col.submitCustomGeometry(
                ps, type, (pose, buffer) -> mesh.renderPart(pose, buffer, light, -1, name));
    }

    public static final class State extends BlockEntityRenderState {
        public SnowglobeType type = SnowglobeType.NONE;
        public int rotation;
    }
}
