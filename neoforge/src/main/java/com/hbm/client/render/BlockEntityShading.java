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

package com.hbm.client.render;

import com.hbm.NuclearTech;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class BlockEntityShading {
    private static final Identifier VERTEX = RegistryUtil.id("core/entity_chunk");
    private static final CardinalLighting.Type[] CARDINALS = CardinalLighting.Type.values();
    private static final Map<RenderPipeline, RenderPipeline[]> VARIANTS = new IdentityHashMap<>();
    public static final List<RenderPipeline> PIPELINES;

    @SuppressWarnings("unchecked")
    private static final Map<RenderType, RenderType>[] TYPES = new Map[CARDINALS.length];

    private static final Set<String> NAMESPACES = ConcurrentHashMap.newKeySet();
    private static boolean submitting;

    static {
        List<RenderPipeline> pipelines = new ArrayList<>();
        for (RenderPipeline source :
                List.of(
                        RenderPipelines.ENTITY_SOLID,
                        RenderPipelines.ENTITY_CUTOUT_CULL,
                        RenderPipelines.ENTITY_CUTOUT,
                        WorldRenderPipeline.ONE_SIDED_CUTOUT_PIPELINE,
                        WorldRenderPipeline.ONE_SIDED_TRANSLUCENT_PIPELINE,
                        DoorGenericRenderer.TRANSLUCENT_CULL_PIPELINE)) {
            var variants = new RenderPipeline[CARDINALS.length];
            for (var cardinal : CARDINALS)
                variants[cardinal.ordinal()] = chunkShaded(source, cardinal);
            VARIANTS.put(source, variants);
            pipelines.addAll(List.of(variants));
        }
        PIPELINES = List.copyOf(pipelines);
        for (int i = 0; i < TYPES.length; i++) TYPES[i] = new ConcurrentHashMap<>();
        claim(NuclearTech.MOD_ID);
    }

    private BlockEntityShading() {}

    public static void claim(String namespace) {
        NAMESPACES.add(namespace);
    }

    public static boolean begin(BlockEntityType<?> type) {
        boolean outer = submitting;
        submitting =
                NAMESPACES.contains(
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type).getNamespace());
        return outer;
    }

    public static void end(boolean outer) {
        submitting = outer;
    }

    public static RenderType shade(RenderType type) {
        if (!submitting) return type;
        RenderPipeline[] variants = VARIANTS.get(type.pipeline());
        if (variants == null) return type;
        int cardinal = Minecraft.getInstance().level.dimensionType().cardinalLightType().ordinal();
        return TYPES[cardinal].computeIfAbsent(type, source -> variant(source, variants[cardinal]));
    }

    private static RenderType variant(RenderType source, RenderPipeline pipeline) {
        var texture = source.state.textures.get("Sampler0");
        var setup =
                RenderSetup.builder(pipeline)
                        .withTexture("Sampler0", texture.location(), texture.sampler())
                        .useLightmap()
                        .useOverlay()
                        .affectsCrumbling()
                        .setOutline(
                                source.outline().isPresent()
                                        ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE
                                        : RenderSetup.OutlineProperty.NONE);
        if (source.sortOnUpload()) setup.sortOnUpload();
        return RenderType.create("ntm_chunk_shaded", setup.createRenderSetup());
    }

    private static RenderPipeline chunkShaded(
            RenderPipeline source, CardinalLighting.Type cardinal) {
        CardinalLighting light = cardinal.get();
        var builder =
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(
                                RegistryUtil.id(
                                        "pipeline/chunk_shaded_"
                                                + source.getLocation()
                                                        .getPath()
                                                        .substring("pipeline/".length())
                                                + "_"
                                                + cardinal.getSerializedName()))
                        .withVertexShader(VERTEX)
                        .withFragmentShader("core/entity")
                        .withShaderDefine("CARDINAL_DOWN", light.down())
                        .withShaderDefine("CARDINAL_UP", light.up())
                        .withShaderDefine("CARDINAL_NORTH", light.north())
                        .withShaderDefine("CARDINAL_SOUTH", light.south())
                        .withShaderDefine("CARDINAL_WEST", light.west())
                        .withShaderDefine("CARDINAL_EAST", light.east())
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                        .withColorTargetState(
                                ((com.mojang.renderpearl.api.pipeline.ColorTargetState)
                                        (source.getColorTargetStates().isEmpty()
                                                ? ColorTargetState.DEFAULT
                                                : source.getColorTargetStates().getFirst())))
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(source.getDepthStencilState())
                        .withCull(source.isCull());
        String cutout = source.getShaderDefines().values().get("ALPHA_CUTOUT");
        if (cutout != null) builder.withShaderDefine("ALPHA_CUTOUT", Float.parseFloat(cutout));
        return WorldRenderPipeline.of(builder);
    }
}
