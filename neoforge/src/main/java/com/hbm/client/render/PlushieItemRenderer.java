// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blockentity.BlockEntityPlushie;
import com.hbm.blocks.generic.BlockPlushie.Plushie;
import com.hbm.blocks.generic.BlockPlushie.PlushieType;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.client.render.flywheel.PlushieItemVisual;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
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

public class PlushieItemRenderer
        implements SpecialModelRenderer<Plushie>, ItemVisuals.Factory<Plushie> {

    private final RenderPlushie geometry;
    private final float[] bounds;

    private PlushieItemRenderer(RenderPlushie geometry) {
        this.geometry = geometry;
        List<HFRWavefrontObject> meshes =
                new ArrayList<>(
                        List.of(
                                ResourceManager.yomi,
                                ResourceManager.hundun,
                                ResourceManager.derg,
                                ResourceManager.horse));
        for (RenderPlushie.Figure figure : PlushieFigures.all()) meshes.addAll(figure.meshes());
        this.bounds = union(meshes);
    }

    private static float[] union(List<HFRWavefrontObject> meshes) {
        float[] box = null;
        for (HFRWavefrontObject mesh : meshes) {
            float[] own = mesh.boundsExcluding(Set.of());
            if (box == null) {
                box = own.clone();
                continue;
            }
            for (int i = 0; i < 3; i++) box[i] = Math.min(box[i], own[i]);
            for (int i = 3; i < 6; i++) box[i] = Math.max(box[i], own[i]);
        }
        return box;
    }

    @Override
    public @Nullable Plushie extractArgument(ItemStack stack) {
        return BlockEntityPlushie.typeOf(stack);
    }

    @Override
    public void submit(
            @Nullable Plushie type,
            PoseStack ps,
            SubmitNodeCollector col,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor) {
        Plushie resolved = type == null ? PlushieType.NONE : type;
        RenderPlushie.itemPose(resolved, ps);
        ItemStackRenderState cigarette = null;
        if (resolved == PlushieType.NUMBERNINE) {
            cigarette = new ItemStackRenderState();
            geometry.resolveCigarette(cigarette, Minecraft.getInstance().level);
        }
        geometry.renderPlushie(resolved, 0L, ps, col, lightCoords, cigarette);
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Plushie type,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new PlushieItemVisual(ctx, type == null ? PlushieType.NONE : type);
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
                    new PlushieItemRenderer(RenderPlushie.forItem()),
                    (stack, ctx, owner) -> BlockEntityPlushie.typeOf(stack),
                    false,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
