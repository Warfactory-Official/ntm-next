// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.MissilePronter;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissile;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class MissileCustomItemVisual extends HbmItemVisual {
    private final @Nullable MissileStruct parts;
    private final List<TransformedInstance> instances = new ArrayList<>();
    private final FloatArrayList heights = new FloatArrayList();
    private final Matrix4f world = new Matrix4f();

    public MissileCustomItemVisual(
            VisualizationContext ctx, MissilePronter pronter, @Nullable MissileStruct parts) {
        super(ctx);
        this.parts = parts;
        if (parts == null) return;
        pronter.visit(
                parts,
                (mesh, skin, y) -> {
                    Material material = ItemMaterials.cutout(skin, true);
                    for (int group = 0; group < mesh.groups.length; group++) {
                        instances.add(transformed(ItemMaterials.part(mesh, group, material)));
                        heights.add(y);
                    }
                });
    }

    @Override
    public boolean update(ItemStack stack) {
        return Objects.equals(ItemCustomMissile.getStruct(stack), parts);
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        for (int i = 0; i < instances.size(); i++) {
            TransformedInstance instance = instances.get(i);
            write(instance, world.set(pose).translate(0F, heights.getFloat(i), 0F), -1, light);
        }
    }
}
