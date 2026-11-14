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

import com.hbm.blockentity.machine.storage.FluidTankContents;
import com.hbm.client.render.FluidTankItemRenderer;
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
