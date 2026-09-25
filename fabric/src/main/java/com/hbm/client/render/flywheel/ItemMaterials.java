// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.RenderTextures;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ItemMaterials {
    private static final RendererReloadCache<Key, Material> MATERIALS =
            new RendererReloadCache<>(ItemMaterials::build);
    private static final RendererReloadCache<Part, Model> MODELS =
            new RendererReloadCache<>(
                    part ->
                            new SingleMeshModel(
                                    PackedQuadMesh.of(part.group(), part.smooth()),
                                    part.material()));
    private static final RendererReloadCache<Material, Material> ITEM_MATERIALS =
            new RendererReloadCache<>(ItemMaterials::itemMaterial);
    private static final RendererReloadCache<Model, Model> ITEM_MODELS =
            new RendererReloadCache<>(
                    model -> {
                        List<Model.ConfiguredMesh> meshes = new ArrayList<>();
                        for (Model.ConfiguredMesh mesh : model.meshes()) {
                            meshes.add(
                                    new Model.ConfiguredMesh(
                                            ITEM_MATERIALS.apply(mesh.material()), mesh.mesh()));
                        }
                        return new SimpleModel(meshes);
                    });

    private ItemMaterials() {}

    public static Material cutout(Identifier texture, boolean cull) {
        return MATERIALS.apply(new Key(Kind.CUTOUT, texture, cull));
    }

    public static Material translucent(Identifier texture) {
        return MATERIALS.apply(new Key(Kind.TRANSLUCENT, texture, false));
    }

    public static Material flatCutout(Identifier texture, boolean cull) {
        return MATERIALS.apply(new Key(Kind.FLAT_CUTOUT, texture, cull));
    }

    public static Material flash(Identifier texture, boolean depth) {
        return MATERIALS.apply(new Key(depth ? Kind.FLASH_DEPTH : Kind.FLASH, texture, false));
    }

    public static Material balefireGlint(Identifier texture) {
        return MATERIALS.apply(new Key(Kind.BALEFIRE_GLINT, texture, false));
    }

    public static Material lights() {
        return MATERIALS.apply(new Key(Kind.LIGHTS, RenderTextures.WHITE, false));
    }

    public static Model part(HFRWavefrontObject mesh, int part, Material material) {
        return group(mesh.groups[part], mesh.smoothing(), material);
    }

    public static Model group(GroupObject group, boolean smooth, Material material) {
        return MODELS.apply(new Part(group, smooth, material));
    }

    public static Model item(Model blockModel) {
        return ITEM_MODELS.apply(blockModel);
    }

    private static Material itemMaterial(Material block) {
        var builder = SimpleMaterial.builderOf(block);
        if (block.light() == LightShaders.SMOOTH) builder.light(LightShaders.SMOOTH_WHEN_EMBEDDED);
        if (block.cardinalLightingMode() == CardinalLightingMode.CHUNK) {
            builder.cardinalLightingMode(CardinalLightingMode.ENTITY);
        }
        return builder.build();
    }

    private static Material build(Key key) {
        var builder =
                SimpleMaterial.builder()
                        .texture(key.texture())
                        .mipmap(false)
                        .backfaceCulling(key.cull());
        return switch (key.kind()) {
            case CUTOUT -> builder.cutout(CutoutShaders.ONE_TENTH).build();
            case TRANSLUCENT ->
                    builder.cutout(CutoutShaders.ONE_TENTH)
                            .transparency(Transparency.ORDER_INDEPENDENT)
                            .writeMask(WriteMask.COLOR)
                            .build();
            case FLAT_CUTOUT ->
                    builder.cutout(CutoutShaders.ONE_TENTH)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .useOverlay(false)
                            .build();
            case FLASH ->
                    builder.transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                            .writeMask(WriteMask.COLOR)
                            .useOverlay(false)
                            .fog(EffectVisuals.FADE)
                            .build();

            case FLASH_DEPTH ->
                    builder.transparency(Transparency.LIGHTNING)
                            .writeMask(WriteMask.COLOR_DEPTH)
                            .useOverlay(false)
                            .fog(EffectVisuals.FADE)
                            .build();
            case LIGHTS ->
                    builder.transparency(Transparency.ORDER_INDEPENDENT)
                            .writeMask(WriteMask.COLOR)
                            .useLight(false)
                            .useOverlay(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .build();
            case BALEFIRE_GLINT ->
                    builder.transparency(Transparency.GLINT)
                            .writeMask(WriteMask.COLOR)
                            .depthTest(DepthTest.EQUAL)
                            .useOverlay(false)
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .fog(EffectVisuals.FADE)
                            .build();
        };
    }

    private enum Kind {
        CUTOUT,
        TRANSLUCENT,
        FLAT_CUTOUT,
        FLASH,
        FLASH_DEPTH,
        LIGHTS,
        BALEFIRE_GLINT
    }

    private record Key(Kind kind, Identifier texture, boolean cull) {}

    private record Part(GroupObject group, boolean smooth, Material material) {}
}
