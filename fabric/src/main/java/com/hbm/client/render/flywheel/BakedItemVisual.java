// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
