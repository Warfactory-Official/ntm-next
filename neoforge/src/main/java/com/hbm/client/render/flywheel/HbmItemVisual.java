// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
