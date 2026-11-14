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

package com.hbm.client.render.flywheel;

import com.hbm.client.render.GlyphidAnimationData;
import com.hbm.entity.mob.glyphid.GlyphidNuclearEntity;
import com.hbm.main.ResourceManager;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import java.util.Arrays;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class GlyphidNuclearVisual extends GlyphidVisual {
    private static final Identifier MINI_NUKE_TEX =
            RegistryUtil.id("textures/models/projectiles/mini_nuke.png");
    private static final PackedQuadMesh PAYLOAD_MESH =
            PackedQuadMesh.of(
                    ResourceManager.projectiles,
                    ResourceManager.projectiles.partId(
                            GlyphidAnimationData.group(GlyphidAnimationData.PAYLOAD_PART)));
    private static final Material FLASH_MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .backfaceCulling(false)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .depthTest(DepthTest.EQUAL)
                    .useLight(false)
                    .build();
    private static final Material PAYLOAD_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(MINI_NUKE_TEX)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .cardinalLightingMode(CardinalLightingMode.ENTITY)
                    .backfaceCulling(false)
                    .build();
    private static final Model[] MAIN_MODELS =
            withPayload(cutout(ResourceManager.glyphid_nuclear_tex), PAYLOAD_MATERIAL);
    private static final Model[] FLASH_MODELS = withPayload(FLASH_MATERIAL, FLASH_MATERIAL);

    private static Model[] withPayload(Material bodyMaterial, Material payloadMaterial) {
        var body = buildSkin(bodyMaterial);
        var models = Arrays.copyOf(body, body.length + 1);
        models[body.length] =
                new SimpleModel(List.of(new Model.ConfiguredMesh(payloadMaterial, PAYLOAD_MESH)));
        return models;
    }

    public static void initModels() {}

    public GlyphidNuclearVisual(
            VisualizationContext context, GlyphidNuclearEntity glyphid, float partialTick) {
        super(context, glyphid, partialTick, MAIN_MODELS, FLASH_MODELS, true);
    }

    @Override
    protected float deathAge(float partialTick) {
        return ((GlyphidNuclearEntity) entity).deathTicks + partialTick;
    }
}
