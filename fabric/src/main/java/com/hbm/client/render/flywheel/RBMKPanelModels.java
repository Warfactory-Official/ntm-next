// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
