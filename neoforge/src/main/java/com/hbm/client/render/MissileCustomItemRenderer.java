// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.client.render.flywheel.MissileCustomItemVisual;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class MissileCustomItemRenderer
        implements SpecialModelRenderer<MissileCustomItemRenderer.Argument>,
                ItemVisuals.Factory<MissileCustomItemRenderer.Argument> {

    private static final MeshItemRenderer.Spin CLOCK =
            new MeshItemRenderer.Spin(25L, 360L, 1F, Optional.empty());

    private static final float EMPTY_HEIGHT = 4F;
    private static final float GUI_SIZE = 20F;

    private final MissilePronter pronter = new MissilePronter();

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        return null;
    }

    @Override
    public void submit(
            @Nullable Argument argument,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        if (argument == null) return;
        pronter.pront(argument.parts(), pose, collector, light);
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Argument argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new MissileCustomItemVisual(
                ctx, pronter, argument == null ? null : argument.parts());
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        float[] bounds = pronter.bounds();
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    output.accept(
                            new Vector3f(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2]));
                }
    }

    private @Nullable Matrix4fc poseFor(
            Argument argument, ItemDisplayContext context, @Nullable Matrix4fc prefix) {
        if (context != ItemDisplayContext.GUI) return prefix;
        float height = argument.parts().height();
        if (height == 0F) height = EMPTY_HEIGHT;
        float scale = GUI_SIZE / height;
        return (prefix != null ? new Matrix4f(prefix) : new Matrix4f())
                .scale(-scale, -scale, -scale)
                .rotate((float) Math.toRadians(argument.spin()), 0F, -1F, 0F);
    }

    public record Argument(MissileStruct parts, float spin) {}

    public record Unbaked(Identifier base) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        instance ->
                                instance.group(
                                                Identifier.CODEC
                                                        .fieldOf("base")
                                                        .forGetter(Unbaked::base))
                                        .apply(instance, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            MissileCustomItemRenderer renderer = new MissileCustomItemRenderer();
            return new DynamicSpecialWrapper<>(
                    renderer,
                    (stack, display, owner) -> {
                        MissileStruct parts = ItemCustomMissile.getStruct(stack);
                        return parts == null ? null : new Argument(parts, CLOCK.degrees());
                    },
                    renderer::poseFor,
                    true,
                    DynamicSpecialWrapper.properties(context, base),
                    Map.of());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
