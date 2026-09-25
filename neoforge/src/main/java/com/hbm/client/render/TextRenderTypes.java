// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public final class TextRenderTypes {

    public static final RenderPipeline ADDITIVE_PIPELINE =
            pipeline("pipeline/ntm_text_additive", false);
    public static final RenderPipeline ADDITIVE_GRAYSCALE_PIPELINE =
            pipeline("pipeline/ntm_text_additive_grayscale", true);
    private static final Map<Identifier, RenderType> ADDITIVE = new HashMap<>();
    private static final Map<Identifier, RenderType> ADDITIVE_GRAYSCALE = new HashMap<>();

    private TextRenderTypes() {}

    public static RenderType additive(Identifier sheet) {
        return ADDITIVE.computeIfAbsent(
                sheet, s -> type("ntm_text_additive", ADDITIVE_PIPELINE, s));
    }

    public static RenderType additiveGrayscale(Identifier sheet) {
        return ADDITIVE_GRAYSCALE.computeIfAbsent(
                sheet, s -> type("ntm_text_additive_grayscale", ADDITIVE_GRAYSCALE_PIPELINE, s));
    }

    public static void submitAdditive(
            SubmitNodeCollector collector,
            PoseStack pose,
            Font font,
            FormattedCharSequence text,
            int color,
            int lightCoords) {
        font.prepareText(text, 0F, 0F, color, false, false, 0)
                .visit(
                        new Font.GlyphVisitor() {
                            @Override
                            public void acceptRenderable(TextRenderable glyph) {
                                RenderType sheet = glyph.renderType(Font.DisplayMode.NORMAL);
                                Identifier texture =
                                        sheet.state.textures.get("Sampler0").location();
                                RenderType type;
                                if (sheet.pipeline() == RenderPipelines.TEXT)
                                    type = additive(texture);
                                else if (sheet.pipeline() == RenderPipelines.TEXT_GRAYSCALE)
                                    type = additiveGrayscale(texture);
                                else throw new IllegalStateException(sheet.toString());
                                collector.submitCustomGeometry(
                                        pose,
                                        type,
                                        (p, buffer) ->
                                                glyph.render(p.pose(), buffer, lightCoords, false));
                            }
                        });
    }

    private static RenderType type(String name, RenderPipeline pipeline, Identifier sheet) {
        return RenderType.create(
                name,
                RenderSetup.builder(pipeline)
                        .withTexture("Sampler0", sheet)
                        .useLightmap()
                        .createRenderSetup());
    }

    private static RenderPipeline pipeline(String location, boolean grayscale) {
        RenderPipeline.Builder builder =
                RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
                        .withLocation(location)
                        .withVertexShader("core/text")
                        .withFragmentShader(ParticleRenderTypes.TEXT_FADE)
                        .withShaderDefine("ALPHA_CUTOUT", 0F)
                        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                        .withCull(false);
        if (grayscale) builder.withShaderDefine("IS_GRAYSCALE");
        return WorldRenderPipeline.of(builder);
    }
}
