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

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AffineUvTransformedInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.PosedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;

public abstract class HbmItemVisual implements ItemStackVisual {
    protected final InstancerProvider instancers;
    private final List<Instance> instances = new ArrayList<>();
    private boolean hidden;

    protected HbmItemVisual(VisualizationContext ctx) {
        instancers = ctx.instancerProvider();
    }

    protected final TransformedInstance transformed(Model model) {
        TransformedInstance instance =
                instancers.instancer(InstanceTypes.TRANSFORMED, model).createInstance();
        instances.add(instance);
        return instance;
    }

    protected final AffineUvTransformedInstance affineUv(Model model) {
        AffineUvTransformedInstance instance =
                instancers.instancer(AffineUvTransformedInstance.TYPE, model).createInstance();
        instances.add(instance);
        return instance;
    }

    protected final PosedInstance posed(Model model) {
        PosedInstance instance = instancers.instancer(InstanceTypes.POSED, model).createInstance();
        instances.add(instance);
        return instance;
    }

    protected static void write(
            TransformedInstance instance, Matrix4fc pose, int color, int light) {
        write(instance, pose, color, light, OverlayTexture.NO_OVERLAY);
    }

    protected static void write(
            TransformedInstance instance, Matrix4fc pose, int color, int light, int overlay) {
        instance.setTransform(pose);
        instance.colorArgb(color);
        instance.light(light);
        instance.overlay(overlay);
        instance.setChanged();
    }

    protected static void write(
            PosedInstance instance, Matrix4fc pose, Matrix3fc normal, int color, int light) {
        instance.setTransform(pose, normal);
        instance.colorArgb(color);
        instance.light(light);
        instance.overlay(OverlayTexture.NO_OVERLAY);
        instance.setChanged();
    }

    protected final void release(Instance instance) {
        instances.remove(instance);
        instance.delete();
    }

    @Override
    public boolean update(ItemStack stack) {
        return false;
    }

    @Override
    public final void beginFrame(Matrix4fc pose, int light, int overlay, float partialTick) {
        if (hidden) {
            for (Instance instance : instances) instance.setVisible(true);
            hidden = false;
        }
        frame(pose, light, overlay, partialTick);
    }

    protected abstract void frame(Matrix4fc pose, int light, int overlay, float partialTick);

    @Override
    public void hide() {
        if (hidden) return;
        for (Instance instance : instances) instance.setVisible(false);
        hidden = true;
    }

    @Override
    public void delete() {
        for (Instance instance : instances) instance.delete();
        instances.clear();
    }
}
