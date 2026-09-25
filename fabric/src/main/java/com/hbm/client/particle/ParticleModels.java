// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.client.render.flywheel.EffectVisuals;
import com.hbm.lib.Library;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.FogShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleCutoutShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;

final class ParticleModels {
    private static final CutoutShader EFFECT_RENDERER_CUTOUT =
            new SimpleCutoutShader(Library.id("cutout/particle.glsl"));
    static final Model LIT = build(ParticleLayers.TRANSLUCENT, true);
    static final Model UNLIT = build(ParticleLayers.TRANSLUCENT, false);
    static final Model ADDITIVE = build(ParticleLayers.ADDITIVE, false);
    static final Model ADDITIVE_NO_FOG = build(ParticleLayers.ADDITIVE_NO_FOG, false);

    private ParticleModels() {}

    public static void initModels() {}

    private static Model build(SingleQuadParticle.Layer layer, boolean worldLit) {
        boolean additive =
                layer == ParticleLayers.ADDITIVE || layer == ParticleLayers.ADDITIVE_NO_FOG;
        boolean opaque = layer == SingleQuadParticle.Layer.OPAQUE;
        return new SingleMeshModel(
                FogQuadMesh.INSTANCE,
                SimpleMaterial.builder()
                        .texture(TextureAtlas.LOCATION_PARTICLES)
                        .mipmap(false)
                        .useOverlay(false)
                        .transparency(
                                opaque
                                        ? Transparency.OPAQUE
                                        : additive
                                                ? Transparency.ORDER_INDEPENDENT_ADDITIVE
                                                : Transparency.ORDER_INDEPENDENT)
                        .writeMask(opaque ? WriteMask.COLOR_DEPTH : WriteMask.COLOR)
                        .cutout(additive ? CutoutShaders.OFF : EFFECT_RENDERER_CUTOUT)
                        .light(worldLit ? LightShaders.FLAT : LightShaders.NONE)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .backfaceCulling(opaque)
                        .fog(
                                layer == ParticleLayers.ADDITIVE_NO_FOG
                                        ? FogShaders.NONE
                                        : additive ? EffectVisuals.FADE : FogShaders.LINEAR)
                        .build());
    }
}
