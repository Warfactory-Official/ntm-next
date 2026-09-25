// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineCyclotron;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderCyclotron
        implements BlockEntityRenderer<BlockEntityMachineCyclotron, RenderCyclotron.State>,
                ConcurrentRenderStateExtraction {
    private static final int[] BAYS = {
        ResourceManager.cyclotron.partId("B1"),
        ResourceManager.cyclotron.partId("B2"),
        ResourceManager.cyclotron.partId("B3"),
        ResourceManager.cyclotron.partId("B4"),
    };

    private static final Identifier[] EMPTY = {
        ResourceManager.cyclotron_ashes_tex, ResourceManager.cyclotron_book_tex,
        ResourceManager.cyclotron_gavel_tex, ResourceManager.cyclotron_coin_tex,
    };
    private static final Identifier[] FILLED = {
        ResourceManager.cyclotron_ashes_filled_tex, ResourceManager.cyclotron_book_filled_tex,
        ResourceManager.cyclotron_gavel_filled_tex, ResourceManager.cyclotron_coin_filled_tex,
    };

    public static final String MOTTO = "plures necat crapula quam gladius";
    public static final FontDescription ALT_FONT =
            new FontDescription.Resource(Identifier.withDefaultNamespace("alt"));
    public static final int MOTTO_COLOR = 0x600060;

    public static final double RING_SPEED = 0.025D;
    public static final double RING_RADIUS = 2.75D;
    public static final float RING_SCALE = 0.1F;
    public static final double RING_LIFT = 2.0D;

    private final RenderType[] emptyTypes = new RenderType[EMPTY.length];
    private final RenderType[] filledTypes = new RenderType[FILLED.length];
    private final Font font;

    public RenderCyclotron(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
        for (int i = 0; i < EMPTY.length; i++) {
            emptyTypes[i] = WorldRenderPipeline.oneSidedCutout(EMPTY[i]);
            filledTypes[i] = WorldRenderPipeline.oneSidedCutout(FILLED[i]);
        }
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
    public AABB getRenderBoundingBox(BlockEntityMachineCyclotron be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 2.5,
                pos.getY(),
                pos.getZ() - 2.5,
                pos.getX() + 3.5,
                pos.getY() + 4,
                pos.getZ() + 3.5);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityMachineCyclotron be,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                be, state, partialTicks, cameraPosition, breaking);
        boolean plugged = true;
        for (int i = 0; i < BlockEntityMachineCyclotron.PLUGS; i++) {
            state.plugs[i] = be.getPlug(i);
            if (!state.plugs[i]) plugged = false;
        }
        state.plugged = plugged;
        state.ringYaw = ringYaw();
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector col, CameraRenderState camera) {
        int light = s.lightCoords;
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);

        for (int i = 0; i < BAYS.length; i++) {
            final int bay = BAYS[i];
            RenderType type = s.plugs[i] ? filledTypes[i] : emptyTypes[i];
            col.submitCustomGeometry(
                    ps,
                    type,
                    (pose, buf) -> ResourceManager.cyclotron.renderPart(pose, buf, light, -1, bay));
        }
        if (s.plugged) submitMotto(s, ps, col);

        ps.popPose();
    }

    public static float ringYaw() {
        return (float) (GameTime.now() * RING_SPEED % 360D);
    }

    private void submitMotto(State s, PoseStack ps, SubmitNodeCollector col) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(s.ringYaw));
        ps.translate(0.0, RING_LIFT, 0.0);
        ps.mulPose(Axis.XP.rotationDegrees(180F));

        float rot = 0F;
        for (int i = 0; i < MOTTO.length(); i++) {
            String glyph = String.valueOf(MOTTO.charAt(i));

            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(rot));

            rot -= font.width(glyph) * 2F;
            ps.translate(RING_RADIUS, 0.0, 0.0);
            ps.mulPose(Axis.YP.rotationDegrees(-90F));
            ps.scale(RING_SCALE, RING_SCALE, RING_SCALE);

            FormattedCharSequence seq =
                    Component.literal(glyph)
                            .withStyle(Style.EMPTY.withFont(ALT_FONT))
                            .getVisualOrderText();
            TextRenderTypes.submitAdditive(
                    col, ps, font, seq, ARGB.opaque(MOTTO_COLOR), s.lightCoords);
            ps.popPose();
        }

        ps.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        public final boolean[] plugs = new boolean[BlockEntityMachineCyclotron.PLUGS];
        public boolean plugged;
        public float ringYaw;
    }
}
