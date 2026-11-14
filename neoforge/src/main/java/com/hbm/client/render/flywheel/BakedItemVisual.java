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

import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBufferer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

final class BakedItemVisual extends HbmItemVisual {
    private final @Nullable TransformedInstance instance;

    BakedItemVisual(
            VisualizationContext ctx,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        super(ctx);
        var meshes = BakedModelBufferer.INSTANCE.bufferItemInVisualFrame(stack, context, owner, 0);
        if (meshes == null) {
            instance = null;
            return;
        }
        List<Model.ConfiguredMesh> configured = new ArrayList<>();
        meshes.meshes()
                .forEach(
                        (key, mesh) -> {
                            Material material =
                                    ModelUtil.getItemMaterial(key.layer(), key.blocksAtlas());
                            if (material == null) return;
                            configured.add(new Model.ConfiguredMesh(material, mesh));
                            if (meshes.foil())
                                configured.add(new Model.ConfiguredMesh(Materials.GLINT, mesh));
                        });
        instance = configured.isEmpty() ? null : transformed(new SimpleModel(configured));
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        if (instance == null) return;
        write(instance, pose, -1, light, overlay);
    }
}
