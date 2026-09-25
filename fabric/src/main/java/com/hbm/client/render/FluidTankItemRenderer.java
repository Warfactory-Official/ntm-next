// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.FluidTankItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.items.ModDataComponents;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.storage.FluidTankContents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class FluidTankItemRenderer
        implements SpecialModelRenderer<FluidTankContents>, ItemVisuals.Factory<FluidTankContents> {
    private final Map<Fluid, Identifier> textures = new ConcurrentHashMap<>();

    @Override
    public @Nullable FluidTankContents extractArgument(ItemStack stack) {
        return stack.get(ModDataComponents.FLUID_TANK_CONTENTS.get());
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (boolean damaged : new boolean[] {false, true}) {
            float[] bounds = FluidTankMeshes.mesh(damaged).getExtents();
            for (int x = 0; x < 2; x++)
                for (int y = 0; y < 2; y++)
                    for (int z = 0; z < 2; z++)
                        output.accept(
                                new Vector3f(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2]));
        }
    }

    @Override
    public void submit(
            @Nullable FluidTankContents contents,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        visitParts(
                contents,
                (mesh, part, texture) ->
                        part(
                                mesh,
                                part,
                                RenderTypes.entityCutout(texture),
                                pose,
                                collector,
                                light));
    }

    public void visitParts(@Nullable FluidTankContents contents, PartVisitor visitor) {
        boolean damaged = contents != null && contents.damaged();
        HFRWavefrontObject mesh = FluidTankMeshes.mesh(damaged);
        visitor.accept(mesh, FluidTankMeshes.frame(damaged), FluidTankMeshes.FRAME);
        if (damaged) visitor.accept(mesh, FluidTankMeshes.TANK_INNER, FluidTankMeshes.INNER);
        visitor.accept(
                mesh,
                FluidTankMeshes.tank(damaged),
                contents == null
                        ? FluidTankMeshes.NONE
                        : textures.computeIfAbsent(
                                contents.tank().type(), FluidTankMeshes::texture));
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable FluidTankContents argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new FluidTankItemVisual(ctx, this, argument);
    }

    @FunctionalInterface
    public interface PartVisitor {
        void accept(HFRWavefrontObject mesh, int part, Identifier texture);
    }

    private static void part(
            HFRWavefrontObject mesh,
            int part,
            RenderType type,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light) {
        collector.submitCustomGeometry(
                pose, type, (posed, buffer) -> mesh.renderPart(posed, buffer, light, -1, part));
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
            FluidTankItemRenderer renderer = new FluidTankItemRenderer();
            return new DynamicSpecialWrapper<>(
                    renderer,
                    (stack, display, owner) -> renderer.extractArgument(stack),
                    false,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
