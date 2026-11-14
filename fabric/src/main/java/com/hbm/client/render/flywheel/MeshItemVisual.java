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

import com.hbm.client.render.MeshItemRenderer;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

public final class MeshItemVisual extends HbmItemVisual {
    private final MeshItemRenderer renderer;
    private final int skin;
    private final List<TransformedInstance> parts = new ArrayList<>();
    private final List<Vector3fc> offsets = new ArrayList<>();
    private final Matrix4f world = new Matrix4f();

    public MeshItemVisual(VisualizationContext ctx, MeshItemRenderer renderer, int skin) {
        super(ctx);
        this.renderer = renderer;
        this.skin = skin;
        Material material = ItemMaterials.cutout(renderer.skinTexture(skin), renderer.cull());
        renderer.visitParts(
                skin,
                (part, offset) -> {
                    parts.add(transformed(ItemMaterials.part(renderer.mesh(), part, material)));
                    offsets.add(offset);
                });
    }

    @Override
    public boolean update(ItemStack stack) {
        return renderer.skin(stack) == skin;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        for (int i = 0; i < parts.size(); i++) {
            Vector3fc offset = offsets.get(i);
            TransformedInstance part = parts.get(i);
            write(part, world.set(pose).translate(offset.x(), offset.y(), offset.z()), -1, light);
        }
    }
}
