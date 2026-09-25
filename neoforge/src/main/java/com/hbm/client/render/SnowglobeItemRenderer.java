// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.client.render.flywheel.SnowglobeItemVisual;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntitySnowglobe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class SnowglobeItemRenderer
        implements SpecialModelRenderer<SnowglobeType>, ItemVisuals.Factory<SnowglobeType> {

    private final RenderSnowglobe geometry;
    private final float[] bounds;

    private SnowglobeItemRenderer(RenderSnowglobe geometry) {
        this.geometry = geometry;
        this.bounds = ResourceManager.snowglobe.boundsExcluding(Set.of());
    }

    @Override
    public @Nullable SnowglobeType extractArgument(ItemStack stack) {
        return BlockEntitySnowglobe.typeOf(stack);
    }

    @Override
    public void submit(
            @Nullable SnowglobeType type,
            PoseStack ps,
            SubmitNodeCollector col,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor) {
        geometry.renderSnowglobe(type == null ? SnowglobeType.NONE : type, ps, col, lightCoords);
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable SnowglobeType type,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new SnowglobeItemVisual(ctx, type == null ? SnowglobeType.NONE : type);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int xi = 0; xi < 2; xi++)
            for (int yi = 0; yi < 2; yi++)
                for (int zi = 0; zi < 2; zi++) {
                    output.accept(
                            new Vector3f(bounds[xi * 3], bounds[yi * 3 + 1], bounds[zi * 3 + 2]));
                }
    }

    public record Unbaked(Identifier base) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new DynamicSpecialWrapper<>(
                    new SnowglobeItemRenderer(RenderSnowglobe.forItem()),
                    (stack, ctx, owner) -> BlockEntitySnowglobe.typeOf(stack),
                    false,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
