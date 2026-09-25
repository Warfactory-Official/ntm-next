// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.google.common.base.Suppliers;
import com.hbm.main.ResourceManager;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class TintedGlintItemModel implements ItemModel {

    private static final float GLINT_COLOR = 0.36F;
    private static final int GLINT_LAYERS = 2;
    private static final int TINT_INDEX = 0;

    private final QuadCollection quads;
    private final Supplier<List<List<BakedQuad>>> glint;
    private final int tint;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;

    private TintedGlintItemModel(
            QuadCollection quads,
            Vector3fc color,
            ModelRenderProperties properties,
            Matrix4fc transformation) {
        this.quads = quads;
        this.properties = properties;
        this.transformation = transformation;
        this.tint =
                ARGB.colorFromFloat(
                        1F,
                        color.x() * GLINT_COLOR,
                        color.y() * GLINT_COLOR,
                        color.z() * GLINT_COLOR);
        this.glint =
                Suppliers.memoize(
                        () -> {
                            List<List<BakedQuad>> passes = new ArrayList<>(GLINT_LAYERS);
                            for (int layer = 0; layer < GLINT_LAYERS; layer++)
                                passes.add(glintPass(quads.getAll(), layer));
                            return List.copyOf(passes);
                        });
        this.extents =
                Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads.getAll()));
    }

    private static List<BakedQuad> glintPass(List<BakedQuad> quads, int layer) {
        RenderType type = WeaponRenderTypes.itemGlint(ResourceManager.glint_tex, layer);
        List<BakedQuad> pass = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            BakedQuad.MaterialInfo original = quad.materialInfo();
            BakedQuad.MaterialInfo material =
                    new BakedQuad.MaterialInfo(
                            original.sprite(),
                            original.layer(),
                            type,
                            TINT_INDEX,
                            original.shade(),
                            0);

            pass.add(
                    new BakedQuad(
                            quad.position0(),
                            quad.position1(),
                            quad.position2(),
                            quad.position3(),
                            uv(quad.position0()),
                            uv(quad.position1()),
                            uv(quad.position2()),
                            uv(quad.position3()),
                            quad.direction(),
                            material,
                            quad.bakedNormals(),
                            quad.bakedColors()));
        }
        return List.copyOf(pass);
    }

    private static long uv(Vector3fc position) {
        return UVPair.pack(position.x(), 1F - position.y());
    }

    private static boolean glints(ItemDisplayContext context) {
        return switch (context) {
            case NONE, GROUND, FIXED, ON_SHELF -> false;
            default -> true;
        };
    }

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed) {
        output.appendModelIdentityElement(this);
        boolean glinting = glints(displayContext);
        output.ensureCapacity(glinting ? 1 + GLINT_LAYERS : 1);

        layer(output, displayContext).prepareQuadList().addAll(quads.getAll());
        if (quads.hasMaterialFlag(BakedQuad.FLAG_ANIMATED)) output.setAnimated();
        if (!glinting) return;

        for (List<BakedQuad> pass : glint.get()) {
            ItemStackRenderState.LayerRenderState layer = layer(output, displayContext);
            layer.tintLayers().add(tint);
            layer.prepareQuadList().addAll(pass);
        }
        output.setAnimated();
    }

    private ItemStackRenderState.LayerRenderState layer(
            ItemStackRenderState output, ItemDisplayContext context) {
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.setExtents(extents);
        layer.setLocalTransform(transformation);
        properties.applyToLayer(layer, context);
        return layer;
    }

    public record Unbaked(Identifier base, Vector3fc color) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                Identifier.CODEC
                                                        .fieldOf("base")
                                                        .forGetter(Unbaked::base),
                                                ExtraCodecs.VECTOR3F
                                                        .fieldOf("color")
                                                        .forGetter(Unbaked::color))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel model = baker.getModel(base);
            TextureSlots slots = model.getTopTextureSlots();
            return new TintedGlintItemModel(
                    model.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY),
                    color,
                    ModelRenderProperties.fromResolvedModel(baker, model, slots),
                    transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
