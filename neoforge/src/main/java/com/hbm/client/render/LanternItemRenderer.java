// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.client.render.flywheel.LanternItemVisual;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
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

public class LanternItemRenderer implements SpecialModelRenderer<Void>, ItemVisuals.Factory<Void> {

    private static final Identifier BODY_TEXTURE = ResourceManager.lantern_tex;
    private static final Identifier LIGHT_TEXTURE = ResourceManager.white_tex;
    private static final RenderType BODY = RenderTypes.entityCutoutCull(BODY_TEXTURE);
    private static final RenderType LIGHT_TYPE = RenderTypes.entityCutoutCull(LIGHT_TEXTURE);

    private final float[] bounds = ResourceManager.lantern.boundsExcluding(Set.of());

    @Override
    public @Nullable Void extractArgument(ItemStack stack) {
        return null;
    }

    @Override
    public void submit(
            @Nullable Void argument,
            PoseStack ps,
            SubmitNodeCollector col,
            int light,
            int overlay,
            boolean hasFoil,
            int outlineColor) {
        col.submitCustomGeometry(
                ps,
                BODY,
                (pose, buffer) ->
                        ResourceManager.lantern.renderPart(
                                pose, buffer, light, -1, RenderLantern.LANTERN_PART));
        int color = RenderLantern.flicker(GameTime.now());
        col.submitCustomGeometry(
                ps,
                LIGHT_TYPE,
                (pose, buffer) ->
                        ResourceManager.lantern.renderPart(
                                pose, buffer, light, color, RenderLantern.LIGHT_PART));
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Void argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new LanternItemVisual(ctx, BODY_TEXTURE, LIGHT_TEXTURE);
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
                    new LanternItemRenderer(),
                    (stack, ctx, owner) -> null,
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
