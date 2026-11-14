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

import com.hbm.entity.mob.DummyEntity;
import com.hbm.entity.mob.FBIEntity;
import com.hbm.entity.mob.GhostEntity;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public class HumanoidMobRenderer<T extends Mob>
        extends net.minecraft.client.renderer.entity.HumanoidMobRenderer<
                T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>
        implements ConcurrentRenderStateExtraction {

    private final Identifier texture;

    private HumanoidMobRenderer(
            EntityRendererProvider.Context context,
            Identifier texture,
            Function<Identifier, RenderType> renderType) {
        super(
                context,
                new HumanoidModel<>(
                        LayerDefinition.create(
                                        HumanoidModel.createMesh(CubeDeformation.NONE, 0F), 64, 32)
                                .bakeRoot(),
                        renderType),
                0.5F);
        this.texture = texture;
    }

    public static HumanoidMobRenderer<GhostEntity> ghost(EntityRendererProvider.Context context) {
        return new HumanoidMobRenderer<>(
                context, ResourceManager.ghost_tex, RenderTypes::entityTranslucent);
    }

    public static HumanoidMobRenderer<DummyEntity> dummy(EntityRendererProvider.Context context) {
        return new HumanoidMobRenderer<>(
                context, ResourceManager.dummy_tex, RenderTypes::entityCutout);
    }

    public static HumanoidMobRenderer<FBIEntity> fbi(EntityRendererProvider.Context context) {
        return new HumanoidMobRenderer<>(
                context, ResourceManager.fbi_tex, RenderTypes::entityCutout);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return texture;
    }
}
