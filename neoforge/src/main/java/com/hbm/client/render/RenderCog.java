// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityCog;
import com.hbm.entity.projectile.EntitySawblade;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.function.ToIntFunction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

public class RenderCog<T extends EntityThrowableInterp> extends EntityRenderer<T, RenderCog.State>
        implements ConcurrentRenderStateExtraction {

    public static final float SPIN_DIVISOR = 3F;
    public static final long COG_SPIN_PERIOD = 360L * 3L;
    public static final long BLADE_SPIN_PERIOD = 360L * 5L;
    public static final int LANDED = 6;

    private final HFRWavefrontObject model;
    private final int part;
    private final long spinPeriod;
    private final ToIntFunction<T> orientation;
    private final ToIntFunction<T> skin;
    private final RenderType[] types;

    private RenderCog(
            EntityRendererProvider.Context context,
            HFRWavefrontObject model,
            String part,
            long spinPeriod,
            ToIntFunction<T> orientation,
            ToIntFunction<T> skin,
            Identifier... textures) {
        super(context);
        this.model = model;
        this.part = model.partId(part);
        this.spinPeriod = spinPeriod;
        this.orientation = orientation;
        this.skin = skin;
        this.types = new RenderType[textures.length];
        for (int i = 0; i < textures.length; i++)
            this.types[i] = RenderTypes.entityCutoutCull(textures[i]);
        this.shadowRadius = 0F;
    }

    public static RenderCog<EntityCog> cog(EntityRendererProvider.Context context) {
        return new RenderCog<>(
                context,
                ResourceManager.stirling,
                "Cog",
                COG_SPIN_PERIOD,
                EntityCog::getOrientation,
                RenderCog::steel,
                ResourceManager.stirling_tex,
                ResourceManager.stirling_steel_tex);
    }

    public static int steel(EntityCog cog) {
        return cog.getMeta() == 0 ? 0 : 1;
    }

    public static RenderCog<EntitySawblade> sawblade(EntityRendererProvider.Context context) {
        return new RenderCog<>(
                context,
                ResourceManager.sawmill,
                "Blade",
                BLADE_SPIN_PERIOD,
                EntitySawblade::getOrientation,
                entity -> 0,
                ResourceManager.sawmill_tex);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.orientation = orientation.applyAsInt(entity);
        state.skin = skin.applyAsInt(entity);
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();

        switch (state.orientation % 6) {
            case 5 -> pose.mulPose(Axis.YP.rotationDegrees(90F));
            case 2 -> pose.mulPose(Axis.YP.rotationDegrees(180F));
            case 4 -> pose.mulPose(Axis.YP.rotationDegrees(270F));
            default -> {}
        }

        pose.translate(0D, 0D, -1D);

        if (state.orientation < LANDED) {
            pose.mulPose(Axis.ZN.rotationDegrees((GameTime.now() % spinPeriod) / SPIN_DIVISOR));
        }

        pose.translate(0D, -1.375D, 0D);

        collector.submitCustomGeometry(
                pose,
                types[state.skin],
                (p, buffer) -> model.renderPart(p, buffer, light, -1, part));
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class State extends EntityRenderState {
        public int orientation;
        public int skin;
    }
}
