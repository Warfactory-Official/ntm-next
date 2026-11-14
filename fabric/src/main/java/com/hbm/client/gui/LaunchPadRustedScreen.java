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

import com.hbm.blockentity.ordnance.launch.LaunchSiloBlockEntity;
import com.hbm.entity.missile.MissileVariants;
import com.hbm.inventory.container.LaunchPadRustedMenu;
import com.hbm.items.ModItems;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.RustLaunchPadReleaseLayout;
import com.hbm.util.RegistryUtil;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LaunchPadRustedScreen extends InfoContainerScreen<LaunchPadRustedMenu> {
    private static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/weapon/gui_launch_pad_rusted.png");
    private static final int RELEASE_X = 26, RELEASE_Y = 36, RELEASE_SIZE = 16;

    public LaunchPadRustedScreen(LaunchPadRustedMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 236);
        titleLabelY = 4;
        inventoryLabelY = imageHeight - 96 + 2;
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
        LaunchSiloBlockEntity pad = pad();
        if (pad != null) {
            boolean codes =
                    menu.getSlot(LaunchSiloBlockEntity.CODE_SLOT)
                            .getItem()
                            .is(ModItems.LAUNCH_CODE.get());
            boolean key =
                    menu.getSlot(LaunchSiloBlockEntity.KEY_SLOT)
                            .getItem()
                            .is(ModItems.LAUNCH_KEY.get());
            if (codes)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 121, 32, 192, 0, 6, 8, 256, 256);
            if (key)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED, TEXTURE, 139, 32, 192, 0, 6, 8, 256, 256);
            if (codes && key && pad.isPayloadArmed()) {
                int launchCode =
                        new Random(pad.getBlockPos().getX() * 131_071 + pad.getBlockPos().getZ())
                                .nextInt(100_000_000);
                for (int i = 0; i < 8; i++) {
                    int magnitude = (int) Math.pow(10, i);
                    int digit = launchCode % (magnitude * 10) / magnitude;
                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            TEXTURE,
                            109 + 6 * i,
                            85,
                            192 + 6 * digit,
                            8,
                            6,
                            8,
                            256,
                            256);
                }
            }
            if (pad.isPayloadArmed()) drawMissilePreview(graphics);
        }
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                RELEASE_X,
                RELEASE_Y,
                RELEASE_SIZE,
                RELEASE_SIZE,
                List.of(
                        Component.translatable("desc.gui.launchPadRusted.releaseMissile"),
                        Component.translatable("desc.gui.launchPadRusted.missileIsLockedIn"),
                        Component.translatable("desc.gui.launchPadRusted.releasingMayCauseDamage"),
                        Component.translatable("desc.gui.launchPadRusted.damagedMissileCanNot"),
                        Component.translatable("desc.gui.launchPadRusted.intoLaunchingPosition")));
        super.extractLabels(graphics, mouseX, mouseY);
    }

    private LaunchSiloBlockEntity pad() {
        return minecraft != null
                        && minecraft.level != null
                        && minecraft.level.getBlockEntity(menu.blockEntity().getBlockPos())
                                instanceof LaunchSiloBlockEntity pad
                ? pad
                : null;
    }

    private void drawMissilePreview(GuiGraphicsExtractor graphics) {
        if (minecraft == null || minecraft.level == null) return;
        var missile = MissileVariants.DOOMSDAY_RUSTED.create(minecraft.level);
        missile.setId(-1);
        EntityRenderState state =
                minecraft
                        .getEntityRenderDispatcher()
                        .getRenderer(missile)
                        .createRenderState(missile, 0F);
        state.shadowPieces.clear();

        submitEntity(
                graphics,
                state,
                7F,
                new Vector3f(),
                new Quaternionf().rotateZ(Mth.PI).rotateY((float) Math.toRadians(90)),
                34,
                32,
                72,
                176);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (checkClick(
                (int) event.x(),
                (int) event.y(),
                RELEASE_X,
                RELEASE_Y,
                RELEASE_SIZE,
                RELEASE_SIZE)) {
            PacketWire.sendToServer(RustLaunchPadReleaseLayout.encode(menu.containerId));
            if (minecraft != null)
                minecraft
                        .getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
