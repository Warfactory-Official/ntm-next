// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.client.render.flywheel.EffectVisuals;
import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.lib.Library;
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
                    material(Library.id("textures/particle/shockwave.png"), false, false));
    static final Model FLARE =
            new SingleMeshModel(
                    FogQuadMesh.INSTANCE,
                    material(Library.id("textures/particle/flare.png"), true, false));
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
                    material(Library.id("textures/particle/rbmk_fire.png"), true, false));

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
