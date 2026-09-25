// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ItemSprites {
    private final Map<Item, Identifier> views = new IdentityHashMap<>();
    private final ItemModelResolver resolver;
    private final ModelManager models;
    private final ItemStackRenderState state = new ItemStackRenderState();
    private final Collector collector = new Collector();
    private final PoseStack pose = new PoseStack();

    public ItemSprites(ItemModelResolver resolver) {
        this.resolver = resolver;
        Minecraft client = Minecraft.getInstance();
        models = client.getModelManager();
        client.getResourceManager()
                .listResources(
                        "items/sprite",
                        id ->
                                id.getNamespace().equals("hbm")
                                        && id.getPath().startsWith("items/sprite/")
                                        && id.getPath().endsWith(".json"))
                .keySet()
                .forEach(
                        id -> {
                            String path =
                                    id.getPath()
                                            .substring(
                                                    "items/sprite/".length(),
                                                    id.getPath().length() - 5);
                            Identifier owner = id.withPath(path);
                            Item item =
                                    BuiltInRegistries.ITEM
                                            .getOptional(owner)
                                            .orElseThrow(
                                                    () ->
                                                            new IllegalStateException(
                                                                    "Sprite view has no item: "
                                                                            + owner));
                            views.put(item, id.withPath("sprite/" + path));
                        });
    }

    public @Nullable List<Pass> resolve(ItemStack stack, ClientLevel level) {
        Identifier view = views.get(stack.getItem());
        state.clear();
        if (view == null)
            resolver.updateForTopItem(state, stack, ItemDisplayContext.FIXED, level, null, 0);
        else
            models.getItemModel(view)
                    .update(state, stack, resolver, ItemDisplayContext.FIXED, level, null, 0);
        for (int layer = 0; layer < state.activeLayerCount; layer++) {
            if (state.layers[layer].specialRenderer != null) return null;
        }
        collector.flat = true;
        collector.passes = new ArrayList<>();
        state.submit(pose, collector, 0, OverlayTexture.NO_OVERLAY, 0);
        return collector.flat && !collector.passes.isEmpty() ? collector.passes : null;
    }

    private static final class Collector extends SubmitNodeStorage {
        private final Map<BakedQuad, BakedQuad> opaque = new IdentityHashMap<>();
        private boolean flat;
        private List<Pass> passes;

        @Override
        public SubmitNodeCollection order(int order) {
            flat = false;
            return new SubmitNodeCollection();
        }

        @Override
        public void submitItem(
                PoseStack pose,
                ItemDisplayContext context,
                int light,
                int overlay,
                int outline,
                int[] tints,
                List<BakedQuad> quads,
                ItemStackRenderState.FoilType foil) {
            for (BakedQuad quad : quads) {
                for (int v = 0; v < 4; v++) {
                    float z = quad.position(v).z();
                    if (z < 7.5F / 16F - 1.0e-6F || z > 8.5F / 16F + 1.0e-6F) {
                        flat = false;
                        return;
                    }
                }
            }
            int[] colors = tints.length == 0 ? tints : tints.clone();
            for (int i = 0; i < colors.length; i++) colors[i] |= 0xFF000000;
            int start = passes.size();
            for (BakedQuad quad : quads) {
                var material = quad.materialInfo();
                Pass pass = null;
                for (int i = start; i < passes.size(); i++) {
                    Pass candidate = passes.get(i);
                    var previous = candidate.quads.getFirst().materialInfo();
                    if (previous.sprite() == material.sprite()
                            && previous.tintIndex() == material.tintIndex()) {
                        pass = candidate;
                        break;
                    }
                }
                if (pass == null) {
                    pass = new Pass(new ArrayList<>(), colors);
                    passes.add(pass);
                }
                pass.quads.add(opaque.computeIfAbsent(quad, Collector::opaque));
            }
        }

        private static BakedQuad opaque(BakedQuad quad) {
            var original = quad.materialInfo();

            var material =
                    new BakedQuad.MaterialInfo(
                            original.sprite(),
                            original.layer(),
                            RenderTypes.entityCutoutCull(original.sprite().atlasLocation()),
                            original.tintIndex(),
                            original.shade(),
                            0);

            return new BakedQuad(
                    quad.position0(),
                    quad.position1(),
                    quad.position2(),
                    quad.position3(),
                    quad.packedUV0(),
                    quad.packedUV1(),
                    quad.packedUV2(),
                    quad.packedUV3(),
                    quad.direction(),
                    material);
        }

        @Override
        public void submitCustomGeometry(
                PoseStack pose,
                RenderType type,
                SubmitNodeCollector.CustomGeometryRenderer renderer) {
            flat = false;
        }
    }

    public static void passPose(PoseStack pose) {
        pose.translate(1, 0, 7.5F / 16F);
        pose.mulPose(Axis.YP.rotationDegrees(180));
    }

    public record Pass(List<BakedQuad> quads, int[] tints) {
        void submit(PoseStack pose, OrderedSubmitNodeCollector collector, int light) {
            pose.pushPose();
            passPose(pose);
            collector.submitItem(
                    pose,
                    ItemDisplayContext.NONE,
                    light,
                    OverlayTexture.NO_OVERLAY,
                    0,
                    tints,
                    quads,
                    ItemStackRenderState.FoilType.NONE);
            pose.popPose();
        }
    }
}
