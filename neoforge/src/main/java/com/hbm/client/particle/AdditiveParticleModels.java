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

package com.hbm.client.particle;

import com.hbm.client.render.flywheel.EffectVisuals;
import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.FogShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import net.minecraft.resources.Identifier;

final class AdditiveParticleModels {
    private static final int WHITE = 0xFFFFFFFF;

    static final Model SHOCKWAVE =
            new SingleMeshModel(
                    PackedQuadMesh.builder(1)
                            .normal(0F, 1F, 0F)
                            .vertex(-1D, 0D, -1D, 1D, 1D, WHITE)
                            .vertex(-1D, 0D, 1D, 1D, 0D, WHITE)
                            .vertex(1D, 0D, 1D, 0D, 0D, WHITE)
                            .vertex(1D, 0D, -1D, 0D, 1D, WHITE)
                            .build(),
                    material(RegistryUtil.id("textures/particle/shockwave.png"), false, false));
    static final Model FLARE =
            new SingleMeshModel(
                    FogQuadMesh.INSTANCE,
                    material(RegistryUtil.id("textures/particle/flare.png"), true, false));
    static final PackedQuadMesh RBMK_QUAD =
            PackedQuadMesh.builder(1)
                    .normal(0F, 1F, 0F)
                    .vertex(-.5D, -.5D, 0D, 0D, 1D, WHITE)
                    .vertex(-.5D, .5D, 0D, 0D, 0D, WHITE)
                    .vertex(.5D, .5D, 0D, 1D, 0D, WHITE)
                    .vertex(.5D, -.5D, 0D, 1D, 1D, WHITE)
                    .build();
    static final Model RBMK_FLAME =
            new SingleMeshModel(
                    RBMK_QUAD,
                    material(RegistryUtil.id("textures/particle/rbmk_fire.png"), true, false));

    private AdditiveParticleModels() {}

    static Material material(Identifier texture, boolean cull, boolean fog) {
        return SimpleMaterial.builder()
                .texture(texture)
                .mipmap(false)
                .useOverlay(false)
                .transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                .writeMask(WriteMask.COLOR)
                .cutout(CutoutShaders.OFF)
                .light(LightShaders.FLAT)
                .ambientOcclusion(false)
                .cardinalLightingMode(CardinalLightingMode.OFF)
                .backfaceCulling(cull)
                .fog(fog ? EffectVisuals.FADE : FogShaders.NONE)
                .build();
    }
}
