// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.GunB92ItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.items.weapon.GunB92;
import com.hbm.main.ResourceManager;
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

public final class GunB92ItemRenderer
        implements SpecialModelRenderer<GunB92ItemRenderer.Argument>,
                ItemVisuals.Factory<GunB92ItemRenderer.Argument> {

    private static final float PX = 0.0625F;
    private static final List<String> PUMP = List.of("Pump1", "Pump2");
    public static final int[] PUMP_IDS = ResourceManager.b92.partIds(PUMP);
    public static final int[] BODY_IDS =
            ResourceManager.b92.partIds(
                    ResourceManager.b92.getGroupNames().stream()
                            .filter(name -> !PUMP.contains(name))
                            .toList());

    private static final float MAX_SLIDE = 0.25F;

    private final List<Vector3fc> extents = extentsOf();

    private static List<Vector3fc> extentsOf() {
        float[] bounds = ResourceManager.b92.getExtents();
        List<Vector3fc> corners = new ArrayList<>();
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    corners.add(
                            new Vector3f(
                                    bounds[x * 3] * PX + x * MAX_SLIDE,
                                    bounds[y * 3 + 1] * PX,
                                    bounds[z * 3 + 2] * PX));
                }
        return List.copyOf(corners);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        extents.forEach(output);
    }

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

        RenderType type = RenderTypes.entityCutoutCull(ResourceManager.b92_tex);

        pose.pushPose();
        tip(argument, pose.last());

        pose.pushPose();
        body(pose.last());
        draw(collector, pose, type, BODY_IDS, light);
        pose.popPose();

        pump(argument, pose.last());
        draw(collector, pose, type, PUMP_IDS, light);
        pose.popPose();
    }

    public static void tip(Argument argument, PoseStack.Pose pose) {
        if (!argument.tips()) return;
        float tip = GunB92.getRotationFromAnim(argument.frame());
        if (tip <= 0) return;
        float off = tip * 2;
        pose.rotate(Axis.ZP.rotationDegrees(tip * -90));
        pose.translate(off * -0.5F, off * -0.5F, 0F);
    }

    public static void body(PoseStack.Pose pose) {
        pose.scale(PX, PX, PX);
    }

    public static void pump(Argument argument, PoseStack.Pose pose) {

        pose.translate(GunB92.getTransFromAnim(argument.frame()), 0F, 0F);
        pose.scale(PX, PX, PX);
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Argument argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new GunB92ItemVisual(
                ctx, argument != null ? argument : argument(stack, HandPass.local(context, owner)));
    }

    public static Argument argument(ItemStack stack, boolean tips) {
        return new Argument(tips, GunB92.getAnimation(stack));
    }

    private static void draw(
            SubmitNodeCollector collector,
            PoseStack pose,
            RenderType type,
            int[] parts,
            int light) {
        collector.submitCustomGeometry(
                pose,
                type,
                (p, buffer) -> {
                    for (int part : parts)
                        ResourceManager.b92.renderPart(p, buffer, light, -1, part);
                });
    }

    public record Argument(boolean tips, int frame) {}

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
                    new GunB92ItemRenderer(),
                    (stack, display, owner) -> argument(stack, HandPass.local(display, owner)),
                    false,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
