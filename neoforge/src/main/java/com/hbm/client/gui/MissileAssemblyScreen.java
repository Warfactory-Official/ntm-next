/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.client.gui;

import com.hbm.blockentity.machine.MissileWorkbenchBlockEntity;
import com.hbm.client.render.MeshItemRenderer;
import com.hbm.entity.ModEntities;
import com.hbm.entity.missile.CustomMissile;
import com.hbm.handler.MissileStruct;
import com.hbm.inventory.container.MissileAssemblyMenu;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.MissileAssemblyBuildLayout;
import com.hbm.util.RegistryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class MissileAssemblyScreen extends InfoContainerScreen<MissileAssemblyMenu> {

    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/gui_missile_assembly.png");

    private static final int LAMP_W = 6, LAMP_H = 8, LAMP_Y = 23;
    private static final int LAMP_LIT_U = 194, LAMP_BAD_U = 200;
    private static final int BUILD_X = 115, BUILD_Y = 35, BUILD_SIZE = 18, BUILD_U = 176;

    private static final MeshItemRenderer.Spin CLOCK =
            new MeshItemRenderer.Spin(10L, 360L, 1F, Optional.empty());

    private static final int PREVIEW_CX = 88, PREVIEW_CY = 98, PREVIEW_HALF = 72;
    private static final float PREVIEW_SIZE = 8 * 18F, PREVIEW_MIN_HEIGHT = 6F;

    private @Nullable CustomMissile previewEntity;

    public MissileAssemblyScreen(MissileAssemblyMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                0,
                0,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        MissileWorkbenchBlockEntity be = menu.blockEntity();

        if (be.bodyStatus() == 1) lamp(graphics, 49, LAMP_LIT_U);
        if (be.payloadStatus() == 1) lamp(graphics, 31, LAMP_LIT_U);
        if (be.guidanceStatus() == 1) lamp(graphics, 13, LAMP_LIT_U);
        if (be.stabilizerStatus() == 1) lamp(graphics, 67, LAMP_LIT_U);
        if (be.stabilizerStatus() == 0) lamp(graphics, 67, LAMP_BAD_U);
        if (be.propulsionStatus() == 1) lamp(graphics, 85, LAMP_LIT_U);

        if (be.canAssemble()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    BUILD_X,
                    BUILD_Y,
                    BUILD_U,
                    0,
                    BUILD_SIZE,
                    BUILD_SIZE,
                    256,
                    256);
        }

        drawMissilePreview(graphics);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private void drawMissilePreview(GuiGraphicsExtractor graphics) {
        if (this.minecraft == null || this.minecraft.level == null) return;

        ItemStack warhead = menu.getSlot(MissileWorkbenchBlockEntity.PAYLOAD_SLOT).getItem();
        ItemStack fuselage = menu.getSlot(MissileWorkbenchBlockEntity.BODY_SLOT).getItem();
        ItemStack fins = menu.getSlot(MissileWorkbenchBlockEntity.STABILIZER_SLOT).getItem();
        ItemStack thruster = menu.getSlot(MissileWorkbenchBlockEntity.PROPULSION_SLOT).getItem();

        if (previewEntity == null) {
            previewEntity =
                    new CustomMissile(ModEntities.MISSILE_CUSTOM.get(), this.minecraft.level);
            previewEntity.setId(-1);
        }
        previewEntity.setParts(warhead, fuselage, fins, thruster);

        MissileStruct parts = previewEntity.parts();
        float height = parts.height();
        if (height == 0F && parts.fins() == null) return;

        float scale = PREVIEW_SIZE / Math.max(height, PREVIEW_MIN_HEIGHT);
        float spin = (float) Math.toRadians(CLOCK.degrees());

        Vector3f translation =
                new Vector3f(height / 2F * Mth.cos(spin), 0F, -height / 2F * Mth.sin(spin));
        Quaternionf rotation =
                new Quaternionf()
                        .rotateZ(Mth.PI)
                        .rotateY(-spin)
                        .rotateX(Mth.HALF_PI)
                        .rotateZ(-Mth.HALF_PI);

        EntityRenderState rs =
                this.minecraft
                        .getEntityRenderDispatcher()
                        .getRenderer(previewEntity)
                        .createRenderState(previewEntity, 0F);
        rs.shadowPieces.clear();

        submitEntity(
                graphics,
                rs,
                scale,
                translation,
                rotation,
                PREVIEW_CX - PREVIEW_HALF,
                PREVIEW_CY - PREVIEW_HALF,
                PREVIEW_HALF * 2,
                PREVIEW_HALF * 2);
    }

    private void lamp(GuiGraphicsExtractor graphics, int x, int u) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, x, LAMP_Y, u, 0, LAMP_W, LAMP_H, 256, 256);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT
                && checkClick(
                        (int) event.x(),
                        (int) event.y(),
                        BUILD_X,
                        BUILD_Y,
                        BUILD_SIZE,
                        BUILD_SIZE)) {
            PacketWire.sendToServer(MissileAssemblyBuildLayout.encode(menu.containerId));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
