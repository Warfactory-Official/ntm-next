// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRBMKBoiler;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

public class ScreenRBMKBoiler extends ScreenInfoContainer<MenuRBMKBoiler> {

    private static final Identifier TEXTURE = Library.id("textures/gui/rbmk/gui_rbmk_boiler.png");

    public ScreenRBMKBoiler(MenuRBMKBoiler menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    private static int steamTypeU(Fluid type) {
        if (type == NTMFluids.HOTSTEAM) return 208;
        if (type == NTMFluids.SUPERHOTSTEAM) return 222;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 236;
        return 194;
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

        BlockEntityRBMKBoiler be = be();
        if (be != null) {
            int i = be.feed.getMaxFill() > 0 ? be.feed.getFill() * 58 / be.feed.getMaxFill() : 0;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    126,
                    82 - i,
                    176,
                    58 - i,
                    14,
                    i,
                    256,
                    256);

            int j = be.steam.getMaxFill() > 0 ? be.steam.getFill() * 22 / be.steam.getMaxFill() : 0;
            if (j > 0) j++;
            if (j > 22) j++;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 91, 65 - j, 190, 24 - j, 4, j, 256, 256);

            int u = steamTypeU(be.steam.getTankType());
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 36, 24, u, 0, 14, 58, 256, 256);

            drawFluidGaugeInfo(graphics, mouseX, mouseY, 126, 24, 16, 56, be.feed);
            drawFluidGaugeInfo(graphics, mouseX, mouseY, 89, 39, 8, 28, be.steam);
            if (checkClick(mouseX, mouseY, 33, 21, 20, 64)) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        33,
                        21,
                        20,
                        64,
                        List.of(
                                Component.translatable(
                                        "desc.gui.rbmkBoiler.compressSteamCycleTier"),
                                Component.translatable(
                                        "desc.gui.rbmkBoiler.columnHeat", (int) be.heat + " °C")));
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 33, 21, 20, 64)) {
            BlockPos core = corePos();
            if (core != null) {
                CompoundTag data = new CompoundTag();
                data.putBoolean("compression", true);
                Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
                playClick();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private BlockEntityRBMKBoiler be() {
        return menu.blockEntity();
    }
}
