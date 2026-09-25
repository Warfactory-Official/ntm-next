// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.Arrays;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.ARGB;

final class PreviewCube {
    private static final float LO = 11F / 32F;
    private static final float HI = 1F - LO;
    private static final int COLOR = ARGB.color(191, 255, 255, 255);
    private static final Material MATERIAL =
            SimpleMaterial.builderOf(Materials.TRANSLUCENT_BLOCK_ITEM)
                    .texture(TextureAtlas.LOCATION_BLOCKS)
                    .mipmap(false)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .backfaceCulling(true)
                    .build();
    static final Model MODEL = buildCube(0F, 1F, 0F);
    static final Model SOUTH_MODEL = buildCube(0F, 0F, 1F);

    private PreviewCube() {}

    static void initModels() {}

    private static Model buildCube(float nx, float ny, float nz) {
        float u0 = 0, u1 = 1, v0 = 0, v1 = 1;
        float[] data = new float[24 * 8];
        int vertex = 0;

        vertex =
                face(
                        data, vertex, u0, u1, v0, v1, HI, HI, HI, u1, v0, LO, HI, HI, u0, v0, LO,
                        LO, HI, u0, v1, HI, LO, HI, u1, v1);
        vertex =
                face(
                        data, vertex, u0, u1, v0, v1, HI, HI, LO, u1, v0, HI, HI, HI, u0, v0, HI,
                        LO, HI, u0, v1, HI, LO, LO, u1, v1);
        vertex =
                face(
                        data, vertex, u0, u1, v0, v1, LO, HI, LO, u1, v0, HI, HI, LO, u0, v0, HI,
                        LO, LO, u0, v1, LO, LO, LO, u1, v1);
        vertex =
                face(
                        data, vertex, u0, u1, v0, v1, LO, HI, HI, u1, v0, LO, HI, LO, u0, v0, LO,
                        LO, LO, u0, v1, LO, LO, HI, u1, v1);
        vertex =
                face(
                        data, vertex, u0, u1, v0, v1, HI, HI, LO, u1, v0, LO, HI, LO, u0, v0, LO,
                        HI, HI, u0, v1, HI, HI, HI, u1, v1);
        face(
                data, vertex, u0, u1, v0, v1, LO, LO, LO, u1, v0, HI, LO, LO, u0, v0, HI, LO, HI,
                u0, v1, LO, LO, HI, u1, v1);
        for (int at = 5; at < data.length; at += 8) {
            data[at] = nx;
            data[at + 1] = ny;
            data[at + 2] = nz;
        }

        int[] colors = new int[24];
        int[] light = new int[24];
        Arrays.fill(colors, COLOR);
        return new SingleMeshModel(PackedQuadMesh.of(data, colors, light), MATERIAL);
    }

    private static int face(
            float[] data,
            int vertex,
            float u0,
            float u1,
            float v0,
            float v1,
            float x0,
            float y0,
            float z0,
            float tx0,
            float ty0,
            float x1,
            float y1,
            float z1,
            float tx1,
            float ty1,
            float x2,
            float y2,
            float z2,
            float tx2,
            float ty2,
            float x3,
            float y3,
            float z3,
            float tx3,
            float ty3) {
        vertex(data, vertex++, x0, y0, z0, tx0, ty0);
        vertex(data, vertex++, x1, y1, z1, tx1, ty1);
        vertex(data, vertex++, x2, y2, z2, tx2, ty2);
        vertex(data, vertex++, x3, y3, z3, tx3, ty3);
        return vertex;
    }

    private static void vertex(
            float[] data, int vertex, float x, float y, float z, float u, float v) {
        int at = vertex * 8;
        data[at] = x;
        data[at + 1] = y;
        data[at + 2] = z;
        data[at + 3] = u;
        data[at + 4] = v;
    }
}
