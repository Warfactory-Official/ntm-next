// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.FluidTankItemRenderer;
import com.hbm.tileentity.machine.storage.FluidTankContents;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class FluidTankItemVisual extends HbmItemVisual {
    private final FluidTankItemRenderer renderer;
    private final boolean damaged;
    private final @Nullable Fluid fluid;
    private final List<TransformedInstance> parts = new ArrayList<>();

    public FluidTankItemVisual(
            VisualizationContext ctx,
            FluidTankItemRenderer renderer,
            @Nullable FluidTankContents contents) {
        super(ctx);
        this.renderer = renderer;
        damaged = contents != null && contents.damaged();
        fluid = contents == null ? null : contents.tank().type();
        renderer.visitParts(
                contents,
                (mesh, part, texture) ->
                        parts.add(
                                transformed(
                                        ItemMaterials.part(
                                                mesh,
                                                part,
                                                ItemMaterials.cutout(texture, false)))));
    }

    @Override
    public boolean update(ItemStack stack) {
        FluidTankContents contents = renderer.extractArgument(stack);
        return (contents != null && contents.damaged()) == damaged
                && Objects.equals(contents == null ? null : contents.tank().type(), fluid);
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        for (TransformedInstance part : parts) {
            write(part, pose, -1, light);
        }
    }
}
