// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class RadarItemRenderer implements SpecialModelRenderer<ItemDisplayContext> {

    private static final int BASE = ResourceManager.radar.partId("Base");
    private static final int DISH = ResourceManager.radar.partId("Dish");
    private static final long SPIN_PERIOD = 3_600L;
    private static final float SPIN_RATE = 0.1F;

    private final List<Vector3fc> extents = extentsOf();

    private static List<Vector3fc> extentsOf() {
        float[] bounds = ResourceManager.radar.boundsOfParts("Base", "Dish");
        List<Vector3fc> corners = new ArrayList<>();
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    corners.add(new Vector3f(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2]));
                }
        return List.copyOf(corners);
    }

    private static void part(
            SubmitNodeCollector collector, PoseStack pose, RenderType type, int name, int light) {
        collector.submitCustomGeometry(
                pose,
                type,
                (p, buffer) -> ResourceManager.radar.renderPart(p, buffer, light, -1, name));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        extents.forEach(output);
    }

    @Override
    public @Nullable ItemDisplayContext extractArgument(ItemStack stack) {
        return ItemDisplayContext.NONE;
    }

    @Override
    public void submit(
            @Nullable ItemDisplayContext context,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        part(
                collector,
                pose,
                WorldRenderPipeline.oneSidedCutout(ResourceManager.radar_base_tex),
                BASE,
                light);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-(GameTime.now() % SPIN_PERIOD) * SPIN_RATE));
        pose.translate(-0.125D, 0D, 0D);
        part(
                collector,
                pose,
                WorldRenderPipeline.oneSidedCutout(ResourceManager.radar_dish_tex),
                DISH,
                light);
        pose.popPose();
    }

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
            return new DynamicSpecialWrapper<>(
                    new RadarItemRenderer(),
                    (stack, display, owner) -> display,
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
