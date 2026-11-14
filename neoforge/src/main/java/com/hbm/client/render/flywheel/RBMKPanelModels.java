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

import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;

final class RBMKPanelModels {
    static final Material MATERIAL =
            SimpleMaterial.builder()
                    .texture(ResourceManager.white_tex)
                    .mipmap(false)
                    .useOverlay(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .transparency(Transparency.OPAQUE)
                    .writeMask(WriteMask.COLOR_DEPTH)
                    .build();
    static final Model COLUMN = new SingleMeshModel(columnMesh(), MATERIAL);
    static final Model DOT = new SingleMeshModel(dotMesh(), MATERIAL);

    private RBMKPanelModels() {}

    static void initModels() {}

    private static PackedQuadMesh columnMesh() {
        float w = .0625F * .75F;
        return PackedQuadMesh.of(
                new float[] {
                    0F, w, -w, 0F, 0F, 1F, 0F, 0F,
                    0F, w, w, 0F, 0F, 1F, 0F, 0F,
                    0F, -w, w, 0F, 0F, 1F, 0F, 0F,
                    0F, -w, -w, 0F, 0F, 1F, 0F, 0F
                },
                null,
                new int[4]);
    }

    private static PackedQuadMesh dotMesh() {
        float w = .03125F;
        float e = .022097F;
        return PackedQuadMesh.of(
                new float[] {
                    0F, w, 0F, 0F, 0F, 1F, 0F, 0F,
                    0F, e, e, 0F, 0F, 1F, 0F, 0F,
                    0F, 0F, w, 0F, 0F, 1F, 0F, 0F,
                    0F, -e, e, 0F, 0F, 1F, 0F, 0F,
                    0F, e, -e, 0F, 0F, 1F, 0F, 0F,
                    0F, w, 0F, 0F, 0F, 1F, 0F, 0F,
                    0F, -e, -e, 0F, 0F, 1F, 0F, 0F,
                    0F, 0F, -w, 0F, 0F, 1F, 0F, 0F,
                    0F, w, 0F, 0F, 0F, 1F, 0F, 0F,
                    0F, -e, e, 0F, 0F, 1F, 0F, 0F,
                    0F, -w, 0F, 0F, 0F, 1F, 0F, 0F,
                    0F, -e, -e, 0F, 0F, 1F, 0F, 0F
                },
                null,
                new int[12]);
    }
}
