// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.client.render.flywheel.BobbleItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.BlockEntityBobble;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
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

public class BobbleItemRenderer
        implements SpecialModelRenderer<BobbleType>, ItemVisuals.Factory<BobbleType> {

    private final RenderBobble geometry;
    private final float[] bounds;

    private BobbleItemRenderer(RenderBobble geometry) {
        this.geometry = geometry;
        this.bounds = ResourceManager.bobble.boundsExcluding(Set.of());
    }

    @Override
    public @Nullable BobbleType extractArgument(ItemStack stack) {
        return BlockEntityBobble.typeOf(stack);
    }

    @Override
    public void submit(
            @Nullable BobbleType type,
            PoseStack ps,
            SubmitNodeCollector col,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor) {
        BobbleType resolved = type == null ? BobbleType.NONE : type;
        ItemStackRenderState cigarette = null;
        if (resolved == BobbleType.VAER) {
            cigarette = new ItemStackRenderState();
            geometry.resolveCigarette(cigarette, Minecraft.getInstance().level);
        }
        ItemStackRenderState redBomb = null;
        if (resolved == BobbleType.ADAM29) {
            redBomb = new ItemStackRenderState();
            geometry.resolveRedBomb(redBomb, Minecraft.getInstance().level);
        }
        ItemStackRenderState doubloons = null;
        if (resolved == BobbleType.FRIZZLE) {
            doubloons = new ItemStackRenderState();
            geometry.resolveDoubloons(doubloons, Minecraft.getInstance().level);
        }
        geometry.renderBobble(
                resolved, GameTime.now(), ps, col, lightCoords, cigarette, redBomb, doubloons);
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable BobbleType type,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new BobbleItemVisual(ctx, type == null ? BobbleType.NONE : type);
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
                    new BobbleItemRenderer(RenderBobble.forItem()),
                    (stack, ctx, owner) -> BlockEntityBobble.typeOf(stack),
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
