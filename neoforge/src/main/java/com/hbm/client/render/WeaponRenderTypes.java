// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.lib.Library;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public final class WeaponRenderTypes {
    public static final RenderPipeline FLASH_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_weapon_flash")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("EMISSIVE")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false))
                            .withCull(false));

    public static final RenderPipeline FLASH_LIT_PIPELINE =
            flashLitPipeline("pipeline/ntm_weapon_flash_lit", false);
    public static final RenderPipeline FLASH_LIT_DEPTH_PIPELINE =
            flashLitPipeline("pipeline/ntm_weapon_flash_lit_depth", true);

    public static final RenderPipeline SMOKE_PIPELINE =
            smokePipeline("pipeline/ntm_weapon_smoke", false);
    public static final RenderPipeline SMOKE_DEPTH_PIPELINE =
            smokePipeline("pipeline/ntm_weapon_smoke_depth", true);

    public static final RenderType SMOKE =
            RenderType.create(
                    "ntm_weapon_smoke",
                    RenderSetup.builder(SMOKE_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());
    public static final RenderType SMOKE_DEPTH =
            RenderType.create(
                    "ntm_weapon_smoke_depth",
                    RenderSetup.builder(SMOKE_DEPTH_PIPELINE)
                            .withTexture("Sampler0", RenderTextures.WHITE)
                            .useLightmap()
                            .createRenderSetup());
    public static final RenderPipeline TRACER_PIPELINE = tracerPipeline(false, true);
    public static final RenderPipeline TRACER_FULLBRIGHT_PIPELINE = tracerPipeline(true, true);
    public static final RenderPipeline TRACER_FULLBRIGHT_NO_FOG_PIPELINE =
            tracerPipeline(true, false);
    public static final RenderPipeline CLOUD_PIPELINE =
            cloudPipeline(
                    "pipeline/ntm_weapon_cloud",
                    new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE_MINUS_SRC_ALPHA));
    public static final RenderPipeline CLOUD_SEPARATE_PIPELINE =
            cloudPipeline(
                    "pipeline/ntm_weapon_cloud_separate",
                    new BlendFunction(
                            BlendFactor.SRC_ALPHA,
                            BlendFactor.ONE_MINUS_SRC_ALPHA,
                            BlendFactor.ONE,
                            BlendFactor.ZERO));

    public static final RenderPipeline CHEMICAL_CLOUD_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                            .withLocation("pipeline/ntm_chemical_cloud")
                            .withVertexShader("core/entity")
                            .withFragmentShader("core/entity")
                            .withShaderDefine("NO_OVERLAY")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(
                                    new ColorTargetState(
                                            new BlendFunction(
                                                    BlendFactor.SRC_ALPHA,
                                                    BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                    BlendFactor.ONE,
                                                    BlendFactor.ZERO)))
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));

    public static final RenderPipeline ROCKET_FLAME_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation(Library.id("pipeline/rocket_flame"))
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(
                                    new DepthStencilState(
                                            DepthStencilState.DEFAULT.depthTest(), false)));
    public static final RenderType ROCKET_FLAME =
            RenderType.create(
                    "rocket_flame",
                    RenderSetup.builder(ROCKET_FLAME_PIPELINE)
                            .withTexture(
                                    "Sampler0", Library.id("textures/particle/particle_base.png"))
                            .useLightmap()
                            .createRenderSetup());

    public static final RenderPipeline TINTED_GLINT_PIPELINE =
            WorldRenderPipeline.of(
                    RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                            .withLocation("pipeline/ntm_tinted_glint")
                            .withVertexShader("core/entity")
                            .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                            .withShaderDefine("APPLY_TEXTURE_MATRIX")
                            .withShaderDefine("NO_OVERLAY")
                            .withShaderDefine("NO_CARDINAL_LIGHTING")
                            .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                            .withColorTargetState(new ColorTargetState(BlendFunction.GLINT))
                            .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                            .withPrimitiveTopology(PrimitiveTopology.QUADS)
                            .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false))
                            .withCull(false));

    public static final RenderType UNTEXTURED = RenderTypes.entityCutout(RenderTextures.WHITE);
    public static final RenderType TRACER =
            RenderType.create(
                    "ntm_weapon_tracer",
                    RenderSetup.builder(TRACER_PIPELINE)
                            .useLightmap()
                            .sortOnUpload()
                            .createRenderSetup());
    public static final RenderType TRACER_FULLBRIGHT =
            RenderType.create(
                    "ntm_weapon_tracer_fullbright",
                    RenderSetup.builder(TRACER_FULLBRIGHT_PIPELINE)
                            .sortOnUpload()
                            .createRenderSetup());
    public static final RenderType TRACER_FULLBRIGHT_NO_FOG =
            RenderType.create(
                    "ntm_weapon_tracer_fullbright_no_fog",
                    RenderSetup.builder(TRACER_FULLBRIGHT_NO_FOG_PIPELINE)
                            .sortOnUpload()
                            .createRenderSetup());
    private static final Map<Identifier, RenderType> FLASH_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> FLASH_LIT_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> FLASH_LIT_DEPTH_CACHE =
            new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> CLOUD_CACHE = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> CLOUD_SEPARATE_CACHE =
            new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderType> CHEMICAL_CLOUD_CACHE =
            new ConcurrentHashMap<>();
    private static final Map<GlintKey, RenderType> BALEFIRE_GLINT = new ConcurrentHashMap<>();

    private static final Map<ItemGlintKey, RenderType> ITEM_GLINT = new ConcurrentHashMap<>();

    public static final float FATMAN_GLINT_SPEED = -6F;

    public static final int BALEFIRE_GLINT_TINT =
            0xFF000000
                    | ((int) (0F * 0.76F * 255F) << 16)
                    | ((int) (0.8F * 0.76F * 255F) << 8)
                    | (int) (0.15F * 0.76F * 255F);

    private WeaponRenderTypes() {}

    private static RenderPipeline tracerPipeline(boolean fullbright, boolean fog) {
        Identifier shader =
                Identifier.parse(fog ? "hbm:core/tracer_ribbon" : "hbm:core/tracer_ribbon_nofog");
        var builder =
                RenderPipeline.builder(
                                fog
                                        ? RenderPipelines.MATRICES_FOG_SNIPPET
                                        : ParticleRenderTypes.MATRICES_NO_FOG_SNIPPET)
                        .withLocation(
                                (fullbright
                                                ? "pipeline/ntm_weapon_tracer_fullbright"
                                                : "pipeline/ntm_weapon_tracer")
                                        + (fog ? "" : "_no_fog"))
                        .withVertexShader(shader)
                        .withFragmentShader(shader)
                        .withShaderDefine("MIN_RIBBON_WIDTH", TracerRibbon.MIN_WIDTH_PIXELS)
                        .withShaderDefine(
                                "RIBBON_FILTER_PADDING", TracerRibbon.FILTER_PADDING_PIXELS)
                        .withVertexBinding(0, TracerRibbon.FORMAT)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withColorTargetState(
                                new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
                        .withDepthStencilState(
                                new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
                        .withCull(false);
        if (fullbright) builder.withShaderDefine("EMISSIVE");
        else builder.withBindGroupLayout(BindGroupLayouts.SAMPLER2);
        return TracerRibbon.pipeline(builder);
    }

    public static RenderType flash(Identifier texture) {
        return FLASH_CACHE.computeIfAbsent(
                texture,
                tex ->
                        RenderType.create(
                                "ntm_weapon_flash",
                                RenderSetup.builder(FLASH_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }

    public static RenderType flashLit(Identifier texture) {
        return FLASH_LIT_CACHE.computeIfAbsent(
                texture,
                tex ->
                        RenderType.create(
                                "ntm_weapon_flash_lit",
                                RenderSetup.builder(FLASH_LIT_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }

    public static RenderType flashLitDepth(Identifier texture) {
        return FLASH_LIT_DEPTH_CACHE.computeIfAbsent(
                texture,
                tex ->
                        RenderType.create(
                                "ntm_weapon_flash_lit_depth",
                                RenderSetup.builder(FLASH_LIT_DEPTH_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .createRenderSetup()));
    }

    private static RenderPipeline flashLitPipeline(String location, boolean writeDepth) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader(ParticleRenderTypes.ENTITY_FADE)
                        .withShaderDefine("NO_OVERLAY")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(
                                new DepthStencilState(
                                        DepthStencilState.DEFAULT.depthTest(), writeDepth))
                        .withCull(false));
    }

    private static RenderPipeline smokePipeline(String location, boolean writeDepth) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_LIGHT_DIR_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader("core/entity")
                        .withShaderDefine("NO_OVERLAY")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withColorTargetState(
                                new ColorTargetState(
                                        new BlendFunction(
                                                BlendFactor.SRC_ALPHA,
                                                        BlendFactor.ONE_MINUS_SRC_ALPHA,
                                                BlendFactor.ONE, BlendFactor.ZERO)))
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(
                                new DepthStencilState(
                                        DepthStencilState.DEFAULT.depthTest(), writeDepth))
                        .withCull(false));
    }

    public static RenderType cloud(Identifier texture) {
        return CLOUD_CACHE.computeIfAbsent(
                texture,
                tex ->
                        RenderType.create(
                                "ntm_weapon_cloud",
                                RenderSetup.builder(CLOUD_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }

    public static RenderType cloudSeparate(Identifier texture) {
        return CLOUD_SEPARATE_CACHE.computeIfAbsent(
                texture,
                tex ->
                        RenderType.create(
                                "ntm_weapon_cloud_separate",
                                RenderSetup.builder(CLOUD_SEPARATE_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }

    private static RenderPipeline cloudPipeline(String location, BlendFunction blend) {
        return WorldRenderPipeline.of(
                RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/entity")
                        .withFragmentShader("core/entity")
                        .withShaderDefine("NO_OVERLAY")
                        .withShaderDefine("NO_CARDINAL_LIGHTING")
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0_SAMPLER2)
                        .withColorTargetState(new ColorTargetState(blend))
                        .withVertexBinding(0, DefaultVertexFormat.ENTITY)
                        .withPrimitiveTopology(PrimitiveTopology.QUADS)
                        .withDepthStencilState(
                                new DepthStencilState(
                                        DepthStencilState.DEFAULT.depthTest(), false)));
    }

    public static RenderType chemicalCloud(Identifier texture) {
        return CHEMICAL_CLOUD_CACHE.computeIfAbsent(
                texture,
                tex ->
                        RenderType.create(
                                "ntm_chemical_cloud",
                                RenderSetup.builder(CHEMICAL_CLOUD_PIPELINE)
                                        .withTexture("Sampler0", tex)
                                        .useLightmap()
                                        .sortOnUpload()
                                        .createRenderSetup()));
    }

    public static RenderType balefireGlint(Identifier texture, int layer) {
        return balefireGlint(texture, layer, FATMAN_GLINT_SPEED);
    }

    public static RenderType balefireGlint(Identifier texture, int layer, float speed) {
        return BALEFIRE_GLINT.computeIfAbsent(
                new GlintKey(texture, speed, layer),
                key -> {
                    String name = "ntm_balefire_glint_" + layer + "_" + Float.floatToIntBits(speed);
                    return RenderType.create(
                            name,
                            RenderSetup.builder(TINTED_GLINT_PIPELINE)
                                    .withTexture("Sampler0", texture)
                                    .useLightmap()
                                    .setTextureTransform(
                                            new TextureTransform(
                                                    name, () -> balefireGlintMatrix(layer, speed)))
                                    .createRenderSetup());
                });
    }

    public static RenderType itemGlint(Identifier texture, int layer) {
        return ITEM_GLINT.computeIfAbsent(
                new ItemGlintKey(texture, layer),
                key ->
                        RenderType.create(
                                "ntm_item_glint_" + layer,
                                RenderSetup.builder(TINTED_GLINT_PIPELINE)
                                        .withTexture("Sampler0", texture)
                                        .useLightmap()
                                        .setTextureTransform(
                                                new TextureTransform(
                                                        "ntm_item_glint_" + layer,
                                                        () -> itemGlintMatrix(layer)))
                                        .createRenderSetup()));
    }

    private static Matrix4f itemGlintMatrix(int k) {
        long modulus = k == 0 ? 3000L : 4873L;
        float offset = (float) (GameTime.now() % modulus) / modulus * 8F;
        return new Matrix4f()
                .scale(0.125F)
                .translate(k == 0 ? offset : -offset, 0F, 0F)
                .rotateZ((float) Math.toRadians(k == 0 ? -50F : 10F));
    }

    public static Matrix4f balefireGlintMatrix(int k, float speed) {
        float offset = (float) (GameTime.now() / 50.0D);
        float movement = offset * (0.001F + k * 0.003F) * speed;
        return new Matrix4f()
                .scale(2F)
                .rotateZ((float) Math.toRadians(30F - k * 60F))
                .translate(0F, movement, 0F);
    }

    private record GlintKey(Identifier texture, float speed, int layer) {}

    private record ItemGlintKey(Identifier texture, int layer) {}
}
