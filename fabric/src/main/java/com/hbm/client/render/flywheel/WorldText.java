// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.MaterialShaders;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.SimpleMaterialShaders;
import dev.engine_room.flywheel.lib.model.QuadMesh;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.util.OverlayTexture;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.jspecify.annotations.Nullable;

public final class WorldText {
    private static final MaterialShaders GRAYSCALE =
            new SimpleMaterialShaders(
                    ResourceUtil.rl("material/default.vert"),
                    ResourceUtil.rl("material/nametag.frag"));
    private static final RendererReloadCache<Sheet, Model> MODELS =
            new RendererReloadCache<>(WorldText::model);
    private static final Matrix4f IDENTITY = new Matrix4f();

    private final InstancerProvider instancers;
    private final Style style;
    private final int light;
    private final boolean shadow;
    private final ArrayList<UvTransformedInstance> glyphs = new ArrayList<>();
    private final Matrix4f pose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final Matrix4f widthPose = new Matrix4f();
    private @Nullable Line requested;
    private volatile @Nullable Layout captured;
    private @Nullable Layout applied;
    private boolean posed;
    private boolean hidden;

    public WorldText(InstancerProvider instancers, Style style, boolean fullBright) {
        this(instancers, style, fullBright, false);
    }

    public WorldText(
            InstancerProvider instancers, Style style, boolean fullBright, boolean shadow) {
        assert !shadow || style != Style.POLYGON_OFFSET;
        this.instancers = instancers;
        this.style = style;
        this.shadow = shadow;
        light = fullBright ? LightCoordsUtil.FULL_BRIGHT : 0;
    }

    private static Model model(Sheet sheet) {
        var builder =
                SimpleMaterial.builder()
                        .texture(sheet.atlas())
                        .light(LightShaders.SMOOTH)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .transparency(Transparency.TRANSLUCENT)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .useOverlay(false)
                        .mipmap(false)
                        .polygonOffset(sheet.style() == Style.POLYGON_OFFSET || sheet.front());

        if (sheet.style() == Style.ADDITIVE)
            builder.transparency(Transparency.ORDER_INDEPENDENT_ADDITIVE)
                    .cutout(CutoutShaders.OFF)
                    .backfaceCulling(false)
                    .fog(EffectVisuals.FADE);
        if (sheet.grayscale()) builder.shaders(GRAYSCALE);
        Material material = builder.build();
        return new SingleMeshModel(UnitQuad.INSTANCE, material);
    }

    public void set(@Nullable Component text, float x, float y, int argb) {
        set(text, null, x, y, argb);
    }

    public void set(
            @Nullable Component text, @Nullable Component measure, float x, float y, int argb) {
        Line line = text == null ? null : new Line(text, measure, x, y, argb);
        if (line == null ? requested == null : line.equals(requested)) return;
        requested = line;
        if (line == null) {
            captured = null;
            return;
        }
        Minecraft.getInstance().execute(() -> captured = capture(line, style, shadow));
    }

    public void write(Matrix4fc textPose) {
        apply(target(), textPose);
    }

    public void write(Posing posing) {
        Layout layout = target();
        posing.pose(layout == null ? 0 : layout.width(), widthPose);
        apply(layout, widthPose);
    }

    private @Nullable Layout target() {
        Layout layout = requested == null ? null : captured;
        return layout == null || layout.line().equals(requested) ? layout : applied;
    }

    private void apply(@Nullable Layout layout, Matrix4fc textPose) {
        boolean relaid = layout != applied;
        if (relaid) {
            applied = layout;
            rebuild();
        }
        if (!relaid && posed && pose.equals(textPose)) return;
        pose.set(textPose);
        posed = true;
        if (applied == null) return;
        List<Quad> quads = applied.quads();
        for (int i = 0; i < quads.size(); i++) {
            Quad quad = quads.get(i);
            world.set(pose).mul(quad.corner());
            glyphs.get(i).setTransform(world).setChanged();
        }
    }

    private void rebuild() {
        for (UvTransformedInstance glyph : glyphs) glyph.delete();
        glyphs.clear();
        if (applied == null) return;
        for (Quad quad : applied.quads()) {
            UvTransformedInstance glyph =
                    instancers
                            .instancer(InstanceTypes.UV_TRANSFORMED, MODELS.get(quad.sheet()))
                            .createInstance();
            if (hidden) glyph.setVisible(false);
            glyphs.add(glyph);
        }
        seed();
    }

    private void seed() {
        if (applied == null || hidden) return;
        List<Quad> quads = applied.quads();
        for (int i = 0; i < quads.size(); i++) {
            Quad quad = quads.get(i);
            glyphs.get(i)
                    .uvRegion(quad.u0(), quad.v0(), quad.uw(), quad.vh())
                    .colorArgb(quad.argb())
                    .light(light);
        }
    }

    public void setVisible(boolean visible) {
        if (visible != hidden) return;
        hidden = !visible;
        for (UvTransformedInstance glyph : glyphs) glyph.setVisible(visible);
        if (!visible) return;
        seed();
        posed = false;
    }

    public void delete() {
        for (UvTransformedInstance glyph : glyphs) glyph.delete();
        glyphs.clear();
    }

    private static Layout capture(Line line, Style style, boolean shadow) {
        Font font = Minecraft.getInstance().font;
        List<Quad> quads = new ArrayList<>();
        Capture capture = new Capture();
        FormattedCharSequence text = line.text().getVisualOrderText();
        font.prepareText(text, line.x(), line.y(), line.argb(), shadow, false, 0)
                .visit(
                        new Font.GlyphVisitor() {
                            @Override
                            public void acceptRenderable(TextRenderable renderable) {
                                RenderType type = renderable.renderType(Font.DisplayMode.NORMAL);
                                Identifier atlas = type.state.textures.get("Sampler0").location();
                                capture.count = 0;
                                renderable.render(
                                        IDENTITY, capture, LightCoordsUtil.FULL_BRIGHT, false);
                                boolean grayscale =
                                        type.pipeline() == RenderPipelines.TEXT_GRAYSCALE;
                                int count = Math.min(capture.count, Capture.CAPACITY) / 4 * 4;
                                float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
                                for (int q = 0; q < count; q += 4) {
                                    minZ = Math.min(minZ, capture.z[q]);
                                    maxZ = Math.max(maxZ, capture.z[q]);
                                }

                                float front =
                                        maxZ - minZ > Font.SHADOW_DEPTH / 2F
                                                ? minZ + Font.SHADOW_DEPTH / 2F
                                                : Float.MAX_VALUE;
                                for (int q = 0; q < count; q += 4) {
                                    quads.add(
                                            capture.quad(
                                                    q,
                                                    new Sheet(
                                                            atlas,
                                                            grayscale,
                                                            style,
                                                            capture.z[q] >= front)));
                                }
                            }
                        });
        int width = line.measure() == null ? font.width(text) : font.width(line.measure());
        return new Layout(line, width, List.copyOf(quads));
    }

    private record Line(Component text, @Nullable Component measure, float x, float y, int argb) {}

    private record Sheet(Identifier atlas, boolean grayscale, Style style, boolean front) {}

    public enum Style {
        NORMAL,
        POLYGON_OFFSET,
        ADDITIVE
    }

    private record Quad(
            Matrix4fc corner, float u0, float v0, float uw, float vh, int argb, Sheet sheet) {}

    private record Layout(Line line, int width, List<Quad> quads) {}

    @FunctionalInterface
    public interface Posing {

        void pose(int width, Matrix4f out);
    }

    private static final class UnitQuad implements QuadMesh {
        static final UnitQuad INSTANCE = new UnitQuad();
        private static final Vector4fc SPHERE = new Vector4f(.5F, .5F, 0F, .7072F);

        @Override
        public int vertexCount() {
            return 4;
        }

        @Override
        public void write(MutableVertexList vertices) {
            vertex(vertices, 0, 0F, 0F);
            vertex(vertices, 1, 0F, 1F);
            vertex(vertices, 2, 1F, 1F);
            vertex(vertices, 3, 1F, 0F);
        }

        private static void vertex(MutableVertexList vertices, int i, float x, float y) {
            vertices.x(i, x);
            vertices.y(i, y);
            vertices.z(i, 0F);
            vertices.r(i, 1F);
            vertices.g(i, 1F);
            vertices.b(i, 1F);
            vertices.a(i, 1F);
            vertices.u(i, x);
            vertices.v(i, y);
            vertices.light(i, 0);
            vertices.overlay(i, OverlayTexture.NO_OVERLAY);
            vertices.normalX(i, 0F);
            vertices.normalY(i, 0F);
            vertices.normalZ(i, 1F);
        }

        @Override
        public Vector4fc boundingSphere() {
            return SPHERE;
        }
    }

    private static final class Capture implements VertexConsumer {
        static final int CAPACITY = 32;
        private final float[] x = new float[CAPACITY];
        private final float[] y = new float[CAPACITY];
        private final float[] z = new float[CAPACITY];
        private final float[] u = new float[CAPACITY];
        private final float[] v = new float[CAPACITY];
        private final int[] color = new int[CAPACITY];
        private int count;

        Quad quad(int i, Sheet sheet) {
            Matrix4f corner =
                    new Matrix4f(
                            x[i + 3] - x[i],
                            y[i + 3] - y[i],
                            z[i + 3] - z[i],
                            0F,
                            x[i + 1] - x[i],
                            y[i + 1] - y[i],
                            z[i + 1] - z[i],
                            0F,
                            0F,
                            0F,
                            1F,
                            0F,
                            x[i],
                            y[i],
                            z[i],
                            1F);
            return new Quad(corner, u[i], v[i], u[i + 2] - u[i], v[i + 2] - v[i], color[i], sheet);
        }

        @Override
        public VertexConsumer addVertex(float px, float py, float pz) {
            if (count < CAPACITY) {
                x[count] = px;
                y[count] = py;
                z[count] = pz;
            }
            count++;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            return setColor(ARGB.color(a, r, g, b));
        }

        @Override
        public VertexConsumer setColor(int argb) {
            if (count - 1 < CAPACITY) color[count - 1] = argb;
            return this;
        }

        @Override
        public VertexConsumer setUv(float pu, float pv) {
            if (count - 1 < CAPACITY) {
                u[count - 1] = pu;
                v[count - 1] = pv;
            }
            return this;
        }

        @Override
        public VertexConsumer setUv1(int pu, int pv) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int pu, int pv) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float nx, float ny, float nz) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
