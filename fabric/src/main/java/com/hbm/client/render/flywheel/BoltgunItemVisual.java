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
