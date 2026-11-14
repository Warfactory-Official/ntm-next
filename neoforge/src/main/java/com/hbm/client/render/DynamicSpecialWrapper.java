/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render;

import com.google.common.base.Suppliers;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class DynamicSpecialWrapper<T> implements ItemModel {

    private final SpecialModelRenderer<T> renderer;
    private final ArgumentExtractor<T> argument;
    private final @Nullable ArgumentPose<T> argumentPose;
    private final boolean animated;
    private final PropertiesFor properties;
    private final Supplier<Vector3fc[]> extents;
    private final Map<ItemDisplayContext, Matrix4f> poses;
    private final Matrix4fc transformation;
    private final boolean identityTransformation;

    public DynamicSpecialWrapper(
            SpecialModelRenderer<T> renderer,
            ArgumentExtractor<T> argument,
            boolean animated,
            ModelRenderProperties properties,
            Map<ItemDisplayContext, Matrix4f> poses,
            Matrix4fc transformation) {
        this(renderer, argument, null, animated, properties, poses, transformation);
    }

    public DynamicSpecialWrapper(
            SpecialModelRenderer<T> renderer,
            ArgumentExtractor<T> argument,
            boolean animated,
            ModelRenderProperties properties,
            Matrix4fc transformation) {
        this(renderer, argument, null, animated, properties, Map.of(), transformation);
    }

    public DynamicSpecialWrapper(
            SpecialModelRenderer<T> renderer,
            ArgumentExtractor<T> argument,
            @Nullable ArgumentPose<T> argumentPose,
            boolean animated,
            ModelRenderProperties properties,
            Map<ItemDisplayContext, Matrix4f> poses,
            Matrix4fc transformation) {
        this(
                renderer,
                argument,
                argumentPose,
                animated,
                stack -> properties,
                poses,
                transformation);
    }

    public DynamicSpecialWrapper(
            SpecialModelRenderer<T> renderer,
            ArgumentExtractor<T> argument,
            @Nullable ArgumentPose<T> argumentPose,
            boolean animated,
            PropertiesFor properties,
            Map<ItemDisplayContext, Matrix4f> poses,
            Matrix4fc transformation) {
        this.renderer = renderer;
        this.argument = argument;
        this.argumentPose = argumentPose;
        this.animated = animated;
        this.properties = properties;
        this.transformation = new Matrix4f(transformation);
        this.identityTransformation =
                (this.transformation.properties() & Matrix4fc.PROPERTY_IDENTITY) != 0;
        this.poses = new EnumMap<>(ItemDisplayContext.class);
        poses.forEach(
                (display, pose) ->
                        this.poses.put(
                                display,
                                argumentPose == null
                                        ? new Matrix4f(this.transformation).mul(pose)
                                        : new Matrix4f(pose)));
        this.extents =
                Suppliers.memoize(
                        () -> {
                            Set<Vector3fc> results = new HashSet<>();
                            renderer.getExtents(results::add);
                            return results.toArray(new Vector3fc[0]);
                        });
    }

    public static ModelRenderProperties properties(
            ItemModel.BakingContext context, Identifier base) {
        ModelBaker baker = context.blockModelBaker();
        ResolvedModel model = baker.getModel(base);
        TextureSlots textureSlots = model.getTopTextureSlots();
        return ModelRenderProperties.fromResolvedModel(baker, model, textureSlots);
    }

    @Override
    public void update(
            ItemStackRenderState output,
            ItemStack item,
            ItemModelResolver resolver,
            ItemDisplayContext displayContext,
            @Nullable ClientLevel level,
            @Nullable ItemOwner owner,
            int seed) {
        output.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        if (item.hasFoil()) {
            ItemStackRenderState.FoilType foilType = ItemStackRenderState.FoilType.STANDARD;
            layer.setFoilType(foilType);
            output.setAnimated();
            output.appendModelIdentityElement(foilType);
        }

        layer.setExtents(extents);
        T arg = argument.extract(item, displayContext, owner);
        Matrix4f pose = poses.get(displayContext);
        Matrix4fc local =
                arg != null && argumentPose != null
                        ? argumentPose.apply(arg, displayContext, pose)
                        : pose;
        if (argumentPose != null && !identityTransformation && local != null) {
            local = new Matrix4f(transformation).mul(local);
        }
        layer.setLocalTransform(local != null ? local : transformation);
        layer.setupSpecialModel(renderer, arg);
        if (animated) output.setAnimated();
        if (arg != null) output.appendModelIdentityElement(arg);
        properties.of(item).applyToLayer(layer, displayContext);
    }

    public SpecialModelRenderer<T> renderer() {
        return renderer;
    }

    @FunctionalInterface
    public interface ArgumentExtractor<T> {
        @Nullable T extract(ItemStack stack, ItemDisplayContext context, @Nullable ItemOwner owner);
    }

    @FunctionalInterface
    public interface ArgumentPose<T> {
        @Nullable Matrix4fc apply(
                T argument, ItemDisplayContext context, @Nullable Matrix4fc fallback);
    }

    @FunctionalInterface
    public interface PropertiesFor {
        ModelRenderProperties of(ItemStack stack);
    }
}
