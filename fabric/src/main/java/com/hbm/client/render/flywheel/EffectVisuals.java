// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.effect.EntityCloudSolinium;
import com.hbm.entity.particle.EntityChlorineFX;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.lib.Library;
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

    public static final FogShader FADE = new SimpleFogShader(Library.id("fog/fade.glsl"));

    private EffectVisuals() {}

    public static void register() {
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
                                new NukeCloudVisual<EntityCloudFleija>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        NukeCloudVisual.Kind.FLEIJA,
                                        cloud -> cloud.age,
                                        EntityCloudFleija::getMaxAge))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.CLOUD_SOLINIUM.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new NukeCloudVisual<EntityCloudSolinium>(
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
                                new NukeCloudVisual<EntityCloudFleijaRainbow>(
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
                                new MultiCloudVisual<EntityOrangeFX>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        MultiCloudVisual.ORANGE,
                                        EntityOrangeFX::particleAge,
                                        EntityOrangeFX::maxAge))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.CHLORINE_FX.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new MultiCloudVisual<EntityChlorineFX>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        MultiCloudVisual.CHLORINE,
                                        EntityChlorineFX::particleAge,
                                        EntityChlorineFX::maxAge))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.CLOUD_FX.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new MultiCloudVisual<EntityCloudFX>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        MultiCloudVisual.CLOUD,
                                        EntityCloudFX::particleAge,
                                        EntityCloudFX::maxAge))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.PINK_CLOUD_FX.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new MultiCloudVisual<EntityPinkCloudFX>(
                                        ctx,
                                        entity,
                                        partialTick,
                                        MultiCloudVisual.PINK,
                                        EntityPinkCloudFX::particleAge,
                                        EntityPinkCloudFX::maxAge))
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
