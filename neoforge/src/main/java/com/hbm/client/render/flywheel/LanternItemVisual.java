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

import com.hbm.client.render.LanternRenderer;
import com.hbm.main.ResourceManager;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;

public final class LanternItemVisual extends HbmItemVisual {
    private final TransformedInstance body;
    private final TransformedInstance light;

    public LanternItemVisual(
            VisualizationContext ctx, Identifier bodyTexture, Identifier lightTexture) {
        super(ctx);
        body =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.lantern,
                                LanternRenderer.LANTERN_PART,
                                ItemMaterials.cutout(bodyTexture, true)));
        light =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.lantern,
                                LanternRenderer.LIGHT_PART,
                                ItemMaterials.cutout(lightTexture, true)));
    }

    @Override
    public boolean update(ItemStack stack) {
        return true;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        write(body, pose, -1, light);
        write(this.light, pose, LanternRenderer.flicker(GameTime.now()), light);
    }
}
