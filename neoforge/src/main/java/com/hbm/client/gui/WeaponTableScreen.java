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

import com.hbm.client.render.DynamicSpecialWrapper;
import com.hbm.client.render.GunItemRenderer;
import com.hbm.inventory.container.WeaponTableMenu;
import com.hbm.items.weapon.sedna.GunBaseNTItem;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.Math;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.*;

public class WeaponTableScreen extends InfoContainerScreen<WeaponTableMenu> {
    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/machine/gui_weapon_modifier.png");
    private static final int PREVIEW_X = 8, PREVIEW_Y = 17, PREVIEW_W = 160, PREVIEW_H = 80;
    private static final int CONFIG_X = 26, CONFIG_Y = 111, CONFIG_W = 7, CONFIG_H = 10;
    private static final int PREVIEW_ID = 1;

    private double yaw = 20D;
    private double pitch = -10D;
    private Display.ItemDisplay previewEntity;

    public WeaponTableScreen(WeaponTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 240);
        inventoryLabelY = imageHeight - 96 + 2;
        titleLabelY = 5;
    }

    private static Matrix4fc modTableBody(ItemStack gun) {
        Identifier modelId = gun.get(DataComponents.ITEM_MODEL);
        if (modelId != null
                && Minecraft.getInstance().getModelManager().getItemModel(modelId)
                        instanceof DynamicSpecialWrapper<?> wrapper
                && wrapper.renderer() instanceof GunItemRenderer renderer) {
            return renderer.modTable(gun);
        }
        return GunItemRenderer.MOD_TABLE_BASE;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0,
                0,
                imageWidth,
                imageHeight,
                256,
                256);

        if (checkClick(mouseX, mouseY, 8, 18, 160, 79)
                && Minecraft.getInstance().mouseHandler.isLeftPressed()) {
            yaw = (leftPos + PREVIEW_X + PREVIEW_W / 2D - mouseX) / (PREVIEW_W / 2D) * -180D;
            pitch = (topPos + 18 + 39.5D - mouseY) / 39.5D * 90D;
        }

        ItemStack gun = menu.previewStack();
        if (gun.getItem() instanceof GunBaseNTItem) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    35,
                    112,
                    176 + 6 * menu.configIndex(),
                    0,
                    6,
                    8,
                    256,
                    256);
            submitPreview(graphics, gun);
        }
        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void submitPreview(GuiGraphicsExtractor graphics, ItemStack gun) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        if (previewEntity == null || previewEntity.level() != minecraft.level) {
            previewEntity = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, minecraft.level);
            previewEntity.setId(PREVIEW_ID);
            previewEntity.setItemTransform(ItemDisplayContext.NONE);
        }
        previewEntity.setItemStack(gun);
        previewEntity.tick();
        EntityRenderState state =
                minecraft.getEntityRenderDispatcher().extractEntity(previewEntity, 0F);

        Matrix4fc body = modTableBody(gun);
        float scale = (float) Math.cbrt(-body.determinant3x3());
        Matrix4f frame =
                new Matrix4f()
                        .rotateY((float) Math.toRadians(yaw))
                        .rotateX((float) Math.toRadians(pitch));
        Matrix4f beforeFlip =
                new Matrix4f()
                        .rotationZ((float) Math.PI)
                        .mul(frame)
                        .mul(new Matrix4f(new Matrix3f(body).scale(-1F / scale)));
        Quaternionf outer =
                new Matrix4f(beforeFlip)
                        .rotateY((float) Math.PI)
                        .getNormalizedRotation(new Quaternionf());

        Vector3f translation = frame.transformPosition(body.getTranslation(new Vector3f()));
        translation.mul(1F / scale, 1F / scale, -1F / scale);
        translation.sub(beforeFlip.transformPosition(new Vector3f(0.5F, 0.5F, 0.5F)));

        submitEntity(
                graphics,
                state,
                scale,
                translation,
                outer,
                PREVIEW_X,
                PREVIEW_Y,
                PREVIEW_W,
                PREVIEW_H);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick(
                        (int) event.x(), (int) event.y(), CONFIG_X, CONFIG_Y, CONFIG_W, CONFIG_H)) {
            ItemStack gun = menu.previewStack();
            if (gun.getItem() instanceof GunBaseNTItem weapon && weapon.getConfigCount() > 1) {
                int target = (menu.configIndex() + 1) % weapon.getConfigCount();
                Minecraft.getInstance()
                        .gameMode
                        .handleInventoryButtonClick(menu.containerId, target);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
