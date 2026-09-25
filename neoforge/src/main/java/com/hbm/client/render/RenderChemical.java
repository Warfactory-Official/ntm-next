// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityChemical.ChemicalStyle;
import com.hbm.entity.projectile.EntityChemical;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public class RenderChemical extends EntityRenderer<EntityChemical, RenderChemical.State>
        implements ConcurrentRenderStateExtraction {
    public RenderChemical(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0F;
    }

    @Override
    protected boolean affectedByCulling(EntityChemical entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityChemical entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.style = entity.getStyle();
        state.progress = (entity.tickCount + partialTicks) / entity.getMaxAge();
        state.gasProgress = (entity.tickCount + partialTicks) / (double) entity.getMaxAge();
        NTMFluidProperty prop = NTMFluidProperties.get(entity.getChemType());
        state.color = prop == null ? 0xFFFFFF : prop.color();
        state.entityId = entity.getId();
        state.yaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
        state.beamLength =
                entity.getDeltaMovement().length() * (entity.tickCount + partialTicks) * 0.75;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();

        switch (state.style) {
            case AMAT, LIGHTNING -> submitBeam(state, pose, collector);
            case GAS -> submitGasCloud(state, pose, collector, camera);
            case GASFLAME -> submitGasFire(state, pose, collector, camera);
            default -> {}
        }

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    private void submitGasFire(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float exp = state.progress;
        float size = exp * 2;
        int rgb =
                Color.HSBtoRGB(
                                Math.max((60 - exp * 100) / 360F, 0.0F),
                                1 - exp * 0.25F,
                                1 - exp * 0.5F)
                        & 0xFFFFFF;
        int alpha = Math.max((int) (255F * (1F - exp)), 0);

        pose.mulPose(camera.orientation);
        billboard(pose, collector, size, rgb, alpha, 1, 1, 0, 0, state.lightCoords);
    }

    private void submitGasCloud(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        double exp = state.gasProgress;
        float size = (float) (exp * 10D);
        int alpha = Math.max((int) (127D * (1D - exp)), 0);

        Random rand = new Random(state.entityId);
        int i = rand.nextInt(2);
        int j = rand.nextInt(2);

        pose.mulPose(camera.orientation);
        billboard(pose, collector, size, state.color, alpha, 1 - i, 1 - j, i, j, state.lightCoords);
    }

    private void billboard(
            PoseStack pose,
            SubmitNodeCollector collector,
            float size,
            int rgb,
            int alpha,
            float u0,
            float v0,
            float u1,
            float v1,
            int light) {
        int color = ARGB.color(alpha, rgb);
        collector.submitCustomGeometry(
                pose,
                WeaponRenderTypes.chemicalCloud(ResourceManager.particle_base_tex),
                (p, tess) -> {
                    Vertices.emit(tess, p, -size, -size, 0, color, u0, v0, light, 0, 1, 0);
                    Vertices.emit(tess, p, size, -size, 0, color, u1, v0, light, 0, 1, 0);
                    Vertices.emit(tess, p, size, size, 0, color, u1, v1, light, 0, 1, 0);
                    Vertices.emit(tess, p, -size, size, 0, color, u0, v1, light, 0, 1, 0);
                });
    }

    private void submitBeam(State state, PoseStack pose, SubmitNodeCollector collector) {
        pose.mulPose(Axis.YP.rotationDegrees(state.yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-state.pitch - 90));

        float length = (float) state.beamLength;
        float size = 0.0625F;
        float o = 0.2F;

        int root = ARGB.colorFromFloat(o, 1F, 1F, 1F);
        int tip = ARGB.colorFromFloat(0F, 1F, 1F, 1F);

        int light = state.lightCoords;
        collector.submitCustomGeometry(
                pose,
                BeamRenderTypes.ADDITIVE_SHADED,
                (p, tess) -> {
                    for (float[] side :
                            new float[][] {
                                {-size, -size, size, -size},
                                {-size, size, size, size},
                                {-size, -size, -size, size},
                                {size, -size, size, size}
                            }) {
                        Vertices.emit(tess, p, side[0], 0, side[1], root, 0, 0, light, 0, 0, 1);
                        Vertices.emit(tess, p, side[2], 0, side[3], root, 0, 0, light, 0, 0, 1);
                        Vertices.emit(tess, p, side[2], length, side[3], tip, 0, 0, light, 0, 0, 1);
                        Vertices.emit(tess, p, side[0], length, side[1], tip, 0, 0, light, 0, 0, 1);
                    }
                });
    }

    public static final class State extends EntityRenderState {
        public ChemicalStyle style;
        public float progress;
        public double gasProgress;
        public int color;
        public int entityId;
        public float yaw;
        public float pitch;
        public double beamLength;
    }
}
