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

import com.hbm.blockentity.PlushieBlockEntity;
import com.hbm.blocks.generic.PlushieBlock.Plushie;
import com.hbm.blocks.generic.PlushieBlock.PlushieType;
import com.hbm.client.render.PlushieRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class PlushieItemVisual extends HbmItemVisual {
    private final Plushie type;
    private final List<TransformedInstance> parts = new ArrayList<>();
    private final List<Matrix4f> locals = new ArrayList<>();
    private final Matrix4f world = new Matrix4f();

    public PlushieItemVisual(VisualizationContext ctx, Plushie type) {
        super(ctx);
        this.type = type;
        PoseStack poses = new PoseStack();
        PlushieRenderer.itemPose(type, poses);
        PlushieRenderer.visitParts(
                type,
                poses,
                (part, mesh, group, face, ps) -> {
                    if (!face.shows(0L)) return;
                    parts.add(
                            transformed(
                                    ItemMaterials.item(
                                            PlushieVisual.meshPart(part, mesh, group).model())));
                    locals.add(new Matrix4f(ps.last().pose()));
                });
        if (type == PlushieType.NUMBERNINE) {
            parts.add(
                    transformed(
                            PropItem.model(PlushieRenderer.cigarette(), ItemDisplayContext.NONE)));
            PlushieRenderer.cigarettePose(poses);
            locals.add(new Matrix4f(poses.last().pose()));
        }
    }

    @Override
    public boolean update(ItemStack stack) {
        return PlushieBlockEntity.typeOf(stack) == type;
    }

    @Override
    protected void frame(Matrix4fc pose, int light, int overlay, float partialTick) {
        for (int i = 0; i < parts.size(); i++) {
            TransformedInstance part = parts.get(i);
            write(part, world.set(pose).mul(locals.get(i)), -1, light);
        }
    }
}
