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

import com.hbm.blockentity.SnowglobeBlockEntity;
import com.hbm.blocks.generic.SnowglobeBlock.SnowglobeType;
import com.hbm.client.render.SnowglobeRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class SnowglobeItemVisual extends HbmItemVisual {
    private final SnowglobeType type;
    private final List<TransformedInstance> parts = new ArrayList<>(3);
    private final WorldText label;
    private final int lineHeight = Minecraft.getInstance().font.lineHeight;
    private final Matrix4f globe = new Matrix4f();
    private final Matrix4f labelBase = new Matrix4f();
    private final WorldText.Posing labelPosing =
            (width, out) -> SnowglobeRenderer.labelWidth(out.set(labelBase), width, lineHeight);

    public SnowglobeItemVisual(VisualizationContext ctx, SnowglobeType type) {
        super(ctx);
        this.type = type;
        parts.add(transformed(ItemMaterials.item(SnowglobeVisual.SOCKET_PART.model())));
        parts.add(transformed(ItemMaterials.item(SnowglobeVisual.GLASS_PART.model())));
        @Nullable MeshPart feature = SnowglobeVisual.FEATURE_PARTS[type.ordinal()];
        if (feature != null) parts.add(transformed(ItemMaterials.item(feature.model())));
        label = new WorldText(instancers, WorldText.Style.NORMAL, true);
        label.set(Component.literal(type.label), 0F, 0F, SnowglobeRenderer.LABEL_COLOR);
    }

    @Override
    public boolean update(ItemStack stack) {
        return SnowglobeBlockEntity.typeOf(stack) == type;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        globe.set(pose).scale(SnowglobeRenderer.SCALE);
        for (TransformedInstance part : parts) {
            write(part, globe, -1, light);
        }
        label.setVisible(true);
        SnowglobeRenderer.labelBase(labelBase.set(globe));
        label.write(labelPosing);
    }

    @Override
    public void hide() {
        super.hide();
        label.setVisible(false);
    }

    @Override
    public void delete() {
        super.delete();
        label.delete();
    }
}
