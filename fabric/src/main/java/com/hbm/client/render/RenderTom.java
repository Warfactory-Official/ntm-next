// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityTom;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.LightCoordsUtil;

public class RenderTom extends EntityRenderer<EntityTom, RenderTom.State>
        implements ConcurrentRenderStateExtraction {

    private static final RenderType TYPE = FlatCutout.of(ResourceManager.tom_main_tex);
    private static final RenderType FLAME = TomRenderTypes.flame(ResourceManager.tom_flame_tex);

    private static final int[] SHELL_OFFSETS = shellOffsets();

    private static final double SCROLL_PERIOD = 50000D;
    private static final double SCROLL_DIVISOR = 2500D;

    public RenderTom(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    private static int[] shellOffsets() {
        Random random = new Random(0);
        int[] offsets = new int[20];
        for (int i = 0; i < offsets.length; i++) offsets[i] = random.nextInt(90);
        return offsets;
    }

    @Override
    protected boolean affectedByCulling(EntityTom entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityTom entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        long now = GameTime.now();

        state.spin = -(now / 10L) % 360L;

        state.scroll = (float) ((now % SCROLL_PERIOD) / SCROLL_DIVISOR);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0D, -50D, 0D);
        poseStack.scale(100F, 100F, 100F);
        collector.submitCustomGeometry(
                poseStack,
                TYPE,
                (pose, buffer) ->
                        ResourceManager.tom_main.render(
                                pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1));

        float spin = state.spin;
        float scroll = state.scroll;
        poseStack.scale(0.8F, 5F, 0.8F);
        for (int shell = 0; shell < SHELL_OFFSETS.length; shell++) {
            poseStack.mulPose(Axis.YP.rotationDegrees(spin + SHELL_OFFSETS[shell]));
            collector.submitCustomGeometry(
                    poseStack,
                    FLAME,
                    (pose, buffer) ->
                            ResourceManager.tom_flame.render(
                                    pose,
                                    buffer,
                                    LightCoordsUtil.FULL_BRIGHT,
                                    -1,
                                    1F,
                                    1F,
                                    0F,
                                    scroll));
            poseStack.mulPose(Axis.YN.rotationDegrees(spin));
            poseStack.scale(-1.015F, 0.9F, 1.015F);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public float spin;
        public float scroll;
    }
}
