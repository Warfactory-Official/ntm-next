// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.RenderLantern;
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
                                RenderLantern.LANTERN_PART,
                                ItemMaterials.cutout(bodyTexture, true)));
        light =
                transformed(
                        ItemMaterials.part(
                                ResourceManager.lantern,
                                RenderLantern.LIGHT_PART,
                                ItemMaterials.cutout(lightTexture, true)));
    }

    @Override
    public boolean update(ItemStack stack) {
        return true;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        write(body, pose, -1, light);
        write(this.light, pose, RenderLantern.flicker(GameTime.now()), light);
    }
}
