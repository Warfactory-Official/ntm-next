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

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.CloudFleijaEntity;
import com.hbm.entity.effect.CloudFleijaRainbowEntity;
import com.hbm.entity.effect.CloudSoliniumEntity;
import com.hbm.entity.particle.ChlorineFXEntity;
import com.hbm.entity.particle.CloudFXEntity;
import com.hbm.entity.particle.OrangeFXEntity;
import com.hbm.entity.particle.PinkCloudFXEntity;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.FogShader;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.lib.material.SimpleFogShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import net.minecraft.resources.Identifier;

public final class EffectVisuals {
    public static final FogShader FADE = new SimpleFogShader(RegistryUtil.id("fog/fade.glsl"));

    private EffectVisuals() {}

    public static void register() {
        SimpleEntityVisualizer.builder(ModEntities.FALLING_BLOCK_BATCH.get())
                .factory(FallingBlockBatchVisual::new)
                .skipVanillaRender(batch -> false)
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.FALLING_BLOCK_NT.get())
                .factory(FallingBlockNTVisual::new)
                .skipVanillaRender(FallingBlockNTVisual::visualized)
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.FALLOUT_RAIN.get())
                .factory(FalloutRainVisual::new)
                .skipVanillaRender(FalloutRainVisuals::ready)
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.ORBITAL_LASER.get())
                .factory(OrbitalLaserVisual::new)
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.CHEMICAL.get())
                .factory(ChemicalVisual::new)
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.BLACK_HOLE.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new BlackHoleVisual(
                                        ctx, entity, partialTick, BlackHoleVisual.Kind.HOLE))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.VORTEX.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new BlackHoleVisual(
                                        ctx, entity, partialTick, BlackHoleVisual.Kind.VORTEX))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.RAGING_VORTEX.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new BlackHoleVisual(
                                        ctx, entity, partialTick, BlackHoleVisual.Kind.RAGING))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.DIGAMMA_QUASAR.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new BlackHoleVisual(
                                        ctx, entity, partialTick, BlackHoleVisual.Kind.QUASAR))
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.CLOUD_FLEIJA.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new NukeCloudVisual<CloudFleijaEntity>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        NukeCloudVisual.Kind.FLEIJA,
                                        cloud -> cloud.age,
                                        CloudFleijaEntity::getMaxAge))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.CLOUD_SOLINIUM.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new NukeCloudVisual<CloudSoliniumEntity>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        NukeCloudVisual.Kind.SOLINIUM,
                                        cloud -> cloud.age,
                                        null))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.CLOUD_RAINBOW.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new NukeCloudVisual<CloudFleijaRainbowEntity>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        NukeCloudVisual.Kind.RAINBOW,
                                        cloud -> cloud.age,
                                        null))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.MOONSTONE_BLAST.get())
                .factory((ctx, entity, partialTick) -> new CloudTomVisual(ctx, entity, partialTick))
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.AGENT_ORANGE.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new MultiCloudVisual<OrangeFXEntity>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        MultiCloudVisual.ORANGE,
                                        OrangeFXEntity::particleAge,
                                        OrangeFXEntity::maxAge))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.CHLORINE_FX.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new MultiCloudVisual<ChlorineFXEntity>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        MultiCloudVisual.CHLORINE,
                                        ChlorineFXEntity::particleAge,
                                        ChlorineFXEntity::maxAge))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.CLOUD_FX.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new MultiCloudVisual<CloudFXEntity>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        MultiCloudVisual.CLOUD,
                                        CloudFXEntity::particleAge,
                                        CloudFXEntity::maxAge))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.PINK_CLOUD_FX.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new MultiCloudVisual<PinkCloudFXEntity>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        MultiCloudVisual.PINK,
                                        PinkCloudFXEntity::particleAge,
                                        PinkCloudFXEntity::maxAge))
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.EMP_BLAST.get())
                .factory((ctx, entity, partialTick) -> new EmpBlastVisual(ctx, entity, partialTick))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.LASER_BLAST.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new DeathBlastVisual(ctx, entity, partialTick))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.BEAM_BOMB.get())
                .factory((ctx, entity, partialTick) -> new BeamBombVisual(ctx, entity, partialTick))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.SIEGE_LASER.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new SiegeLaserVisual(ctx, entity, partialTick))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.METEOR.get())
                .factory((ctx, entity, partialTick) -> new MeteorVisual(ctx, entity, partialTick))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.SPEAR.get())
                .factory((ctx, entity, partialTick) -> new SpearVisual(ctx, entity, partialTick))
                .apply();
    }

    static final class Shared {
        static final Identifier WHITE =
                Identifier.fromNamespaceAndPath("flywheel", "textures/flywheel/white.png");

        static final Material BEAM_ADDITIVE =
                unlit().transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                        .fog(FADE)
                        .writeMask(WriteMask.COLOR)
                        .backfaceCulling(false)
                        .build();

        static final Material CLOUD_OPAQUE =
                unlit().transparency(Transparency.OPAQUE)
                        .writeMask(WriteMask.COLOR_DEPTH)
                        .backfaceCulling(true)
                        .build();

        static final Material CLOUD_OPAQUE_UNCULLED =
                unlit().transparency(Transparency.OPAQUE)
                        .writeMask(WriteMask.COLOR_DEPTH)
                        .backfaceCulling(false)
                        .build();

        static final Material CLOUD_ADDITIVE =
                unlit().transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                        .fog(FADE)
                        .writeMask(WriteMask.COLOR)
                        .backfaceCulling(true)
                        .build();

        static final Material CLOUD_TRANSLUCENT =
                unlit().transparency(Transparency.ORDER_INDEPENDENT)
                        .writeMask(WriteMask.COLOR)
                        .backfaceCulling(true)
                        .build();

        private Shared() {}

        static SimpleMaterial.Builder unlit() {
            return SimpleMaterial.builder()
                    .texture(WHITE)
                    .mipmap(false)
                    .useLight(false)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF);
        }
    }
}
