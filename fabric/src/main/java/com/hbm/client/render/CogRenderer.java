/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render;

import com.hbm.entity.projectile.CogEntity;
import com.hbm.entity.projectile.InterpolatedProjectileEntity;
import com.hbm.entity.projectile.SawbladeEntity;
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
import org.joml.Quaternionfc;

public class CogRenderer<T extends InterpolatedProjectileEntity>
        extends EntityRenderer<T, CogRenderer.State> implements ConcurrentRenderStateExtraction {
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

    private CogRenderer(
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

    public static CogRenderer<CogEntity> cog(EntityRendererProvider.Context context) {
        return new CogRenderer<>(
                context,
                ResourceManager.stirling,
                "Cog",
                COG_SPIN_PERIOD,
                CogEntity::getOrientation,
                CogRenderer::steel,
                ResourceManager.stirling_tex,
                ResourceManager.stirling_steel_tex);
    }

    public static int steel(CogEntity cog) {
        return cog.getMeta() == 0 ? 0 : 1;
    }

    public static CogRenderer<SawbladeEntity> sawblade(EntityRendererProvider.Context context) {
        return new CogRenderer<>(
                context,
                ResourceManager.sawmill,
                "Blade",
                BLADE_SPIN_PERIOD,
                SawbladeEntity::getOrientation,
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
            case 5 -> pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(90F));
            case 2 -> pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(180F));
            case 4 -> pose.rotate((Quaternionfc) Axis.YP.rotationDegrees(270F));
            default -> {}
        }

        pose.translate(0D, 0D, -1D);

        if (state.orientation < LANDED) {
            pose.rotate(
                    (Quaternionfc)
                            Axis.ZN.rotationDegrees((GameTime.now() % spinPeriod) / SPIN_DIVISOR));
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
