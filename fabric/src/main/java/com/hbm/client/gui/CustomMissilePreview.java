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

package com.hbm.client.gui;

import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.CustomMissile;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.CustomMissilePartItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

final class CustomMissilePreview {

    private static final int CX = 88, CY = 115;
    private static final float SIZE = 5 * 18F, MIN_HEIGHT = 6F;
    private static final int HALF_W = 33, HALF_H = 94;

    private @Nullable CustomMissile entity;

    void draw(InfoContainerScreen<?> screen, GuiGraphicsExtractor graphics, MissileStruct parts) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        if (entity == null) {
            entity = new CustomMissile(ModEntities.MISSILE_CUSTOM.get(), minecraft.level);
            entity.setId(-1);
        }
        entity.setParts(
                stack(parts.warhead()),
                stack(parts.fuselage()),
                stack(parts.fins()),
                stack(parts.thruster()));

        float height = parts.height();
        float scale = SIZE / Math.max(height, MIN_HEIGHT);

        Vector3f translation = new Vector3f(0F, 0F, height / 2F);
        Quaternionf rotation = new Quaternionf().rotateZ(Mth.PI).rotateY(Mth.HALF_PI);

        EntityRenderState rs =
                minecraft
                        .getEntityRenderDispatcher()
                        .getRenderer(entity)
                        .createRenderState(entity, 0F);
        rs.shadowPieces.clear();

        screen.submitEntity(
                graphics,
                rs,
                scale,
                translation,
                rotation,
                CX - HALF_W,
                CY - HALF_H,
                HALF_W * 2,
                HALF_H * 2);
    }

    private static ItemStack stack(@Nullable CustomMissilePartItem part) {
        return part == null ? ItemStack.EMPTY : new ItemStack(part);
    }
}
