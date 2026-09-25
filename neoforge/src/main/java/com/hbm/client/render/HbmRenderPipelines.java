// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.particle.ParticleLayers;
import com.hbm.render.util.RenderSparks;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class HbmRenderPipelines {

    public static final RenderPipeline GUI_TEXTURED_ADDITIVE =
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/ntm_gui_textured_additive")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .build();
    public static final RenderPipeline GUI_ADDITIVE =
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation("pipeline/ntm_gui_additive")
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .build();

    private static final List<RenderPipeline> ALL =
            List.of(
                    GUI_TEXTURED_ADDITIVE,
                    GUI_ADDITIVE,
                    ArmorOverheadRenderer.THERMAL_LINES_PIPELINE,
                    ArmorRenderTypes.LAMP_PIPELINE,
                    ArmorRenderTypes.LOOT_GLOW_PIPELINE,
                    ArmorRenderTypes.LOOT_HELMET_PIPELINE,
                    AssemblyMarkers.MARKER_LINES_PIPELINE,
                    BeamRenderTypes.ADDITIVE_CULL_PIPELINE,
                    BeamRenderTypes.ADDITIVE_NO_FOG_PIPELINE,
                    BeamRenderTypes.ADDITIVE_SHADED_PIPELINE,
                    BeamRenderTypes.ADDITIVE_PIPELINE,
                    BeamRenderTypes.LINE_PIPELINE,
                    BobbleRenderTypes.FIGURINE_PIPELINE,
                    BobbleRenderTypes.GLOW_PIPELINE,
                    CargoElevatorOutline.PIPELINE,
                    CloudRenderTypes.ADDITIVE_LIT_UNCULLED_PIPELINE,
                    CloudRenderTypes.ADDITIVE_PIPELINE,
                    CloudRenderTypes.OPAQUE_PIPELINE,
                    CloudRenderTypes.OPAQUE_UNCULLED_PIPELINE,
                    CloudRenderTypes.TRANSLUCENT_PIPELINE,
                    ConveyorPreviewRenderer.PREVIEW_PIPELINE,
                    DetonatorLaserRenderTypes.LIGHTS_PIPELINE,
                    FlatCutout.CULL_PIPELINE,
                    FlatCutout.PIPELINE,
                    FlatTranslucent.CULL,
                    FlatTranslucent.DEPTH_WRITE,
                    FlatTranslucent.LIT_CULL,
                    FlatTranslucent.LIT_CUTOUT,
                    FlatTranslucent.LIT_NO_CULL,
                    FlatTranslucent.LIT_PLAIN,
                    FlatTranslucent.NO_CULL,
                    FusionPlasmaRenderTypes.BASE_PIPELINE,
                    FusionPlasmaRenderTypes.BEAM_PIPELINE,
                    FusionPlasmaRenderTypes.OPAQUE_PIPELINE,
                    FusionPlasmaRenderTypes.PIPELINE,
                    GlyphidPathRenderer.PATH_LINES_PIPELINE,
                    HologramRenderTypes.GHOST_PIPELINE,
                    NTMSkybox.IMPACT_STARS,
                    ParticleLayers.ADDITIVE_NO_FOG_PIPELINE,
                    ParticleLayers.ADDITIVE_PIPELINE,
                    ParticleLayers.TRANSLUCENT_PIPELINE,
                    ParticleLayers.TRANSLUCENT_SEPARATE_PIPELINE,
                    ParticleRenderTypes.AMAT_FLASH_PIPELINE,
                    ParticleRenderTypes.FLASH_NO_FOG_CULL_PIPELINE,
                    ParticleRenderTypes.FLASH_NO_FOG_PIPELINE,
                    ParticleRenderTypes.LINES_NO_DEPTH_PIPELINE,
                    ParticleRenderTypes.LIT_TRANSLUCENT_PIPELINE,
                    ParticleRenderTypes.OPAQUE_PIPELINE,
                    ParticleRenderTypes.RIFT_PIPELINE,
                    ParticleRenderTypes.SKELETON_PIPELINE,
                    RBMKColumnGrid.PANEL_PIPELINE,
                    RenderDoorGeneric.TRANSLUCENT_CULL_PIPELINE,
                    RenderFoundry.SHEEN_PIPELINE,
                    RenderFurnaceCombination.FLAME_PIPELINE,
                    RenderMachineForceField.SHELL_PIPELINE,
                    RenderRBMKGraph.PLOT_PIPELINE,
                    RenderRBMKFuelRod.CHERENKOV_PIPELINE,
                    RenderRefueler.FLUID_PIPELINE,
                    RenderSpear.FLASH_PIPELINE,
                    RenderSpear.GHOST_PIPELINE,
                    RenderSparks.SPARK_LINES_PIPELINE,
                    TextRenderTypes.ADDITIVE_GRAYSCALE_PIPELINE,
                    TextRenderTypes.ADDITIVE_PIPELINE,
                    TomRenderTypes.FLAME_PIPELINE,
                    TorexRenderTypes.CLOUDLET_PIPELINE,
                    TorexRenderTypes.FLARE_PIPELINE,
                    TorexRenderTypes.FLASH_PIPELINE,
                    VortexRenderTypes.ADDITIVE_PIPELINE,
                    VortexRenderTypes.CUTOUT_PIPELINE,
                    VortexRenderTypes.TRANSLUCENT_PIPELINE,
                    WeaponRenderTypes.TINTED_GLINT_PIPELINE,
                    WeaponRenderTypes.CLOUD_PIPELINE,
                    WeaponRenderTypes.CLOUD_SEPARATE_PIPELINE,
                    WeaponRenderTypes.CHEMICAL_CLOUD_PIPELINE,
                    WeaponRenderTypes.ROCKET_FLAME_PIPELINE,
                    WeaponRenderTypes.FLASH_PIPELINE,
                    WeaponRenderTypes.SMOKE_PIPELINE,
                    WeaponRenderTypes.SMOKE_DEPTH_PIPELINE,
                    WeaponRenderTypes.FLASH_LIT_PIPELINE,
                    WeaponRenderTypes.FLASH_LIT_DEPTH_PIPELINE,
                    WeaponRenderTypes.TRACER_FULLBRIGHT_NO_FOG_PIPELINE,
                    WeaponRenderTypes.TRACER_FULLBRIGHT_PIPELINE,
                    WeaponRenderTypes.TRACER_PIPELINE,
                    WorldRenderPipeline.ONE_SIDED_CUTOUT_PIPELINE,
                    WorldRenderPipeline.ONE_SIDED_TRANSLUCENT_PIPELINE,
                    WorldRenderPipeline.UNTEXTURED_CULL_PIPELINE,
                    WorldRenderPipeline.UNTEXTURED_PIPELINE);

    static {
        Set<Identifier> locations = new HashSet<>();
        for (RenderPipeline pipeline : ALL) {
            if (!locations.add(pipeline.getLocation())) {
                throw new IllegalStateException(
                        "Two render pipelines share the location " + pipeline.getLocation());
            }
        }
    }

    private HbmRenderPipelines() {}

    public static void forEach(Consumer<RenderPipeline> registrar) {
        ALL.forEach(registrar);
    }

    public static List<RenderPipeline> all() {
        return ALL;
    }
}
