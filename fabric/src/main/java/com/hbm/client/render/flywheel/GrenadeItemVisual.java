// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.GrenadeItemRenderer;
import com.hbm.items.weapon.grenade.GrenadeData;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.main.ResourceManager;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class GrenadeItemVisual extends HbmItemVisual {
    private final GrenadeData data;
    private final List<Part> parts = new ArrayList<>();
    private final Matrix4f world = new Matrix4f();

    public GrenadeItemVisual(
            VisualizationContext ctx, GrenadeData data, ItemDisplayContext context) {
        super(ctx);
        this.data = data;
        GrenadeItemRenderer.visit(
                new PoseStack(),
                data,
                context,
                false,
                (pose, texture, part, color, fullBright) -> {
                    Model model = model(texture, part, color);
                    parts.add(
                            new Part(
                                    transformed(model),
                                    new Matrix4f(pose.last().pose()),
                                    color,
                                    fullBright));
                });
    }

    private static Model model(Identifier texture, int part, int color) {
        return ItemMaterials.part(
                ResourceManager.grenades,
                part,
                color == -1
                        ? ItemMaterials.cutout(texture, false)
                        : ItemMaterials.translucent(texture));
    }

    @Override
    public boolean update(ItemStack stack) {
        return ItemGrenadeUniversal.getData(stack).equals(data);
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        for (Part part : parts) {
            write(
                    part.instance,
                    world.set(pose).mul(part.local),
                    part.color,
                    part.fullBright ? LightCoordsUtil.FULL_BRIGHT : light);
        }
    }

    private record Part(
            TransformedInstance instance, Matrix4f local, int color, boolean fullBright) {}
}
