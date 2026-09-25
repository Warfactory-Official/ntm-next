// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.BoltgunItemVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.HbmAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
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

public final class BoltgunItemRenderer
        implements SpecialModelRenderer<Boolean>, ItemVisuals.Factory<Boolean> {
    private static final String PART_GUN = "Gun";
    private static final String PART_BARREL = "Barrel";
    public static final int GUN = ResourceManager.boltgun.partId(PART_GUN);
    public static final int BARREL = ResourceManager.boltgun.partId(PART_BARREL);

    private final List<Vector3fc> extents = extentsOf();

    private static List<Vector3fc> extentsOf() {
        float[] bounds = ResourceManager.boltgun.boundsOfParts(PART_GUN, PART_BARREL);
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
                (p, buffer) -> ResourceManager.boltgun.renderPart(p, buffer, light, -1, name));
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        extents.forEach(output);
    }

    @Override
    public @Nullable Boolean extractArgument(ItemStack stack) {
        return false;
    }

    @Override
    public void submit(
            @Nullable Boolean recoils,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean foil,
            int outline) {
        if (recoils == null) return;

        RenderType type = RenderTypes.entityCutoutCull(ResourceManager.boltgun_tex);

        if (recoils) {

            pose.pushPose();
            pose.translate(0F, 0F, recoil());
            part(collector, pose, type, BARREL, light);
            pose.popPose();
        }

        part(collector, pose, type, GUN, light);

        if (!recoils) part(collector, pose, type, BARREL, light);
    }

    public static float recoil() {
        return (float) -HbmAnimations.getRelevantTransformation("RECOIL")[0];
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Boolean recoils,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new BoltgunItemVisual(ctx, HandPass.local(context, owner));
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
            BoltgunItemRenderer renderer = new BoltgunItemRenderer();
            return new DynamicSpecialWrapper<>(
                    renderer,
                    (stack, display, owner) -> HandPass.local(display, owner),
                    true,
                    DynamicSpecialWrapper.properties(context, base));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
