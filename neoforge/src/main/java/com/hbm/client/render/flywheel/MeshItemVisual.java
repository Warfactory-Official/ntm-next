// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
