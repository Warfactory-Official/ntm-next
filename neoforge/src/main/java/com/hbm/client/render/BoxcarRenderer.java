/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionfc;

public class BoxcarRenderer<T extends Entity> extends EntityRenderer<T, BoxcarRenderer.State>
        implements ConcurrentRenderStateExtraction {

    private final Kind kind;
    private final RenderType body;
    private final HFRWavefrontObject model;

    public BoxcarRenderer(EntityRendererProvider.Context context, Kind kind) {
        super(context);
        this.shadowRadius = 0F;
        this.kind = kind;
        this.body =
                switch (kind) {
                    case BOXCAR -> RenderTypes.entityCutoutCull(ResourceManager.boxcar_tex);
                    case DUCHESS_GAMBIT ->
                            RenderTypes.entityCutoutCull(ResourceManager.duchessgambit_tex);
                    case BUILDING ->
                            WorldRenderPipeline.oneSidedCutout(ResourceManager.building_tex);
                    case TORPEDO -> RenderTypes.entityCutoutCull(ResourceManager.torpedo_tex);
                };
        this.model =
                switch (kind) {
                    case BOXCAR -> ResourceManager.boxcar;
                    case DUCHESS_GAMBIT -> ResourceManager.duchessgambit;
                    case BUILDING -> ResourceManager.building;
                    case TORPEDO -> ResourceManager.torpedo;
                };
    }

    @Override
    protected boolean affectedByCulling(T entity) {
        return false;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.age = entity.tickCount + partialTicks;
    }

    @Override
    public void submit(
            State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();

        switch (this.kind) {
            case BOXCAR -> {
                pose.translate(0, 0, -1.5F);
                pose.rotate((Quaternionfc) Axis.ZP.rotationDegrees(180));
                pose.rotate((Quaternionfc) Axis.XP.rotationDegrees(90));
            }
            case DUCHESS_GAMBIT -> pose.translate(0, 0, -1.0F);
            case BUILDING -> {}
            case TORPEDO ->
                    pose.rotate(
                            (Quaternionfc) Axis.XP.rotationDegrees(Math.min(85, state.age * 3)));
        }

        collector.submitCustomGeometry(pose, body, (p, buf) -> model.render(p, buf, light, -1));

        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public enum Kind {
        BOXCAR,
        DUCHESS_GAMBIT,
        BUILDING,
        TORPEDO
    }

    public static final class State extends EntityRenderState {
        public float age;
    }
}
