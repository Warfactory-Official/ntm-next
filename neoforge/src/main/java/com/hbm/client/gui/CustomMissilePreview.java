// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissilePart;
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

    private @Nullable EntityMissileCustom entity;

    void draw(ScreenInfoContainer<?> screen, GuiGraphicsExtractor graphics, MissileStruct parts) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;

        if (entity == null)
            entity = new EntityMissileCustom(ModEntities.MISSILE_CUSTOM.get(), minecraft.level);
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

    private static ItemStack stack(@Nullable ItemCustomMissilePart part) {
        return part == null ? ItemStack.EMPTY : new ItemStack(part);
    }
}
