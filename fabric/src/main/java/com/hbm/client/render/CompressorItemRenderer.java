// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.CompressorItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.ArrayList;
import java.util.List;
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

public final class CompressorItemRenderer
        implements SpecialModelRenderer<ItemDisplayContext>,
                ItemVisuals.Factory<ItemDisplayContext> {

    public static final int BODY = ResourceManager.compressor.partId("Compressor");
    public static final int PUMP = ResourceManager.compressor.partId("Pump");
    public static final int FAN = ResourceManager.compressor.partId("Fan");
    private static final float FAN_PIVOT_Y = 1.5F;

    private final List<Vector3fc> extents = extentsOf();

    private static List<Vector3fc> extentsOf() {
        float[] body = ResourceManager.compressor.boundsOfParts("Compressor", "Pump");
        float[] fan = ResourceManager.compressor.boundsOfParts("Fan");
        float radius = 0F;
        for (int y = 0; y < 2; y++)
            for (int z = 0; z < 2; z++) {
                radius =
                        Math.max(
                                radius,
                                (float) Math.hypot(fan[y * 3 + 1] - FAN_PIVOT_Y, fan[z * 3 + 2]));
            }
        float[] lo = {
            Math.min(body[0], fan[0]),
            Math.min(body[1] - 3F, FAN_PIVOT_Y - radius),
            Math.min(body[2], -radius)
        };
        float[] hi = {
            Math.max(body[3], fan[3]),
            Math.max(body[4], FAN_PIVOT_Y + radius),
            Math.max(body[5], radius)
        };
        List<Vector3fc> corners = new ArrayList<>();
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    corners.add(
                            new Vector3f(
                                    x == 0 ? lo[0] : hi[0],
                                    y == 0 ? lo[1] : hi[1],
                                    z == 0 ? lo[2] : hi[2]));
                }
        return List.copyOf(corners);
    }

    public static double lift(long millis) {
        double lift = (millis * 0.005D) % 9D;
        return lift > 3D ? 3D - (lift - 3D) / 2D : lift;
    }

    public static double fanDegrees(long millis) {
        return (millis * 0.25D) % 360D;
    }

    public static void pumpPose(PoseStack.Pose pose, long millis) {
        pose.translate(0F, (float) -lift(millis), 0F);
    }

    public static void fanPose(PoseStack.Pose pose, long millis) {
        pose.translate(0F, FAN_PIVOT_Y, 0F);
        pose.rotate(Axis.XP.rotationDegrees((float) fanDegrees(millis)));
        pose.translate(0F, -FAN_PIVOT_Y, 0F);
    }

    private static void part(
            SubmitNodeCollector collector, PoseStack pose, RenderType type, int part, int light) {
        collector.submitCustomGeometry(
                pose,
                type,
                (p, buffer) -> ResourceManager.compressor.renderPart(p, buffer, light, -1, part));
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
        RenderType type = RenderTypes.entityCutout(ResourceManager.compressor_tex);
        long now = GameTime.now();

        part(collector, pose, type, BODY, light);

        pose.pushPose();
        pumpPose(pose.last(), now);
        part(collector, pose, type, PUMP, light);
        pose.popPose();

        pose.pushPose();
        fanPose(pose.last(), now);
        part(collector, pose, type, FAN, light);
        pose.popPose();
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable ItemDisplayContext argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new CompressorItemVisual(ctx);
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
                    new CompressorItemRenderer(),
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
