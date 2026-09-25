// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.google.common.base.Suppliers;
import com.hbm.items.special.ItemHot;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class HotItemRenderer implements ItemModel {

    private static final int OVERLAY_TINT = 1;

    private final List<BakedQuad> icon;
    private final List<BakedQuad> overlay;
    private final boolean animated;
    private final ModelRenderProperties properties;
    private final Matrix4fc transformation;
    private final Supplier<Vector3fc[]> extents;

    private HotItemRenderer(
            List<BakedQuad> icon,
            List<BakedQuad> overlay,
            boolean animated,
            ModelRenderProperties properties,
            Matrix4fc transformation) {
        this.icon = icon;
        this.overlay = overlay;
        this.animated = animated;
        this.properties = properties;
        this.transformation = transformation;
        List<BakedQuad> all = new ArrayList<>(icon);
        all.addAll(overlay);
        this.extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(all));
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
        long gameTime = level != null ? level.getGameTime() : GameTime.ticks();
        int tint = ARGB.white(Mth.clamp((float) ItemHot.getHeat(item, gameTime), 0.0F, 1.0F));
        output.appendModelIdentityElement(tint);

        ItemStackRenderState.LayerRenderState base = layer(output, displayContext, icon);
        if (item.hasFoil()) {
            base.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            output.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
        }
        if (ARGB.alpha(tint) > 0) {
            ItemStackRenderState.LayerRenderState hot = layer(output, displayContext, overlay);
            hot.tintLayers().add(-1);
            hot.tintLayers().add(tint);
        }
        if (animated) output.setAnimated();
    }

    private ItemStackRenderState.LayerRenderState layer(
            ItemStackRenderState output, ItemDisplayContext displayContext, List<BakedQuad> quads) {
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.setExtents(extents);
        layer.setLocalTransform(transformation);
        properties.applyToLayer(layer, displayContext);
        layer.prepareQuadList().addAll(quads);
        return layer;
    }

    private static BakedQuad blended(BakedQuad quad) {
        BakedQuad.MaterialInfo info = quad.materialInfo();
        Material.Baked material = new Material.Baked(info.sprite(), true);

        BakedQuad.MaterialInfo translucent =
                BakedQuad.MaterialInfo.of(
                        material,
                        Transparency.TRANSLUCENT,
                        info.tintIndex(),
                        info.shade(),
                        info.lightEmission());
        return new BakedQuad(
                quad.position0(),
                quad.position1(),
                quad.position2(),
                quad.position3(),
                quad.packedUV0(),
                quad.packedUV1(),
                quad.packedUV2(),
                quad.packedUV3(),
                quad.direction(),
                translucent);
    }

    public record Unbaked(Identifier model) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(Identifier.CODEC.fieldOf("model").forGetter(Unbaked::model))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolved = baker.getModel(model);
            TextureSlots slots = resolved.getTopTextureSlots();
            QuadCollection quads =
                    resolved.bakeTopGeometry(slots, baker, BlockModelRotation.IDENTITY);
            List<BakedQuad> icon = new ArrayList<>();
            List<BakedQuad> overlay = new ArrayList<>();
            for (BakedQuad quad : quads.getAll()) {
                if (quad.materialInfo().tintIndex() == OVERLAY_TINT) overlay.add(blended(quad));
                else icon.add(quad);
            }
            if (overlay.isEmpty()) {
                throw new IllegalStateException(
                        model + " states no layer " + OVERLAY_TINT + " to draw hot");
            }
            return new HotItemRenderer(
                    icon,
                    overlay,
                    quads.hasMaterialFlag(BakedQuad.FLAG_ANIMATED),
                    ModelRenderProperties.fromResolvedModel(baker, resolved, slots),
                    transformation);
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
