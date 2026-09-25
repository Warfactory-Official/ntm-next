// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.BoltgunItemRenderer;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class BoltgunItemVisual extends HbmItemVisual {
    private final boolean recoils;
    private final TransformedInstance gun;
    private final TransformedInstance barrel;
    private final Matrix4f barrelPose = new Matrix4f();

    public BoltgunItemVisual(VisualizationContext ctx, boolean recoils) {
        super(ctx);
        this.recoils = recoils;
        Material material = ItemMaterials.cutout(ResourceManager.boltgun_tex, true);
        gun =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.boltgun, BoltgunItemRenderer.GUN, material));
        barrel =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.boltgun, BoltgunItemRenderer.BARREL, material));
    }

    @Override
    public boolean update(ItemStack stack) {
        return true;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        write(gun, pose, -1, light);
        write(
                barrel,
                recoils
                        ? barrelPose.set(pose).translate(0F, 0F, BoltgunItemRenderer.recoil())
                        : pose,
                -1,
                light);
    }
}
