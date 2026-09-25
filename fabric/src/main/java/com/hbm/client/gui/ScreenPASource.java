// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuPASource;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.albion.BlockEntityPASource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenPASource extends ScreenPACooled<MenuPASource> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/particleaccelerator/gui_source.png");

    public ScreenPASource(MenuPASource menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.titleLabelY = 4;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return this.imageWidth / 2 - 9;
    }

    @Override
    protected int titleColor() {
        return CommonColors.WHITE;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityPASource be = menu.blockEntity();

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
        drawPowerBar(graphics, TEXTURE, 8, be.power, be.getMaxPower());

        if (Math.ceil(be.temperature) <= COLD_ENOUGH) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 44, 16, 176, 8, 8, 8, 256, 256);
        }
        if (be.power >= BlockEntityPASource.usage) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 44, 41, 176, 8, 8, 8, 256, 256);
        }

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                45,
                73,
                176,
                52,
                68,
                14,
                256,
                256,
                ARGB.opaque(be.state.color));

        drawCoolant(graphics, mouseX, mouseY, be, 134);
        drawElectricityInfo(graphics, mouseX, mouseY, 8, 18, 16, 52, be.power, be.getMaxPower());

        List<Component> info = new ArrayList<>();
        info.add(
                Component.translatable("desc.gui.paSource.lastMomentum")
                        .withStyle(ChatFormatting.BLUE)
                        .append(
                                Component.literal(String.format(Locale.US, "%,d", be.lastSpeed))
                                        .withStyle(ChatFormatting.RESET)));
        for (Component line : lineArray("pa." + be.state.name().toLowerCase(Locale.US) + ".desc")) {
            info.add(line.copy().withStyle(ChatFormatting.YELLOW));
        }
        drawCustomInfoStat(graphics, mouseX, mouseY, 105, 16, 10, 10, info);
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                105,
                28,
                10,
                10,
                List.of(
                        Component.translatable("desc.gui.paSource.cancelOperation")
                                .withStyle(ChatFormatting.RED)));

        Component state = Component.translatable("pa." + be.state.name().toLowerCase(Locale.US));
        graphics.text(
                this.font,
                state,
                79 - this.font.width(state) / 2,
                76,
                ARGB.opaque(be.state.color),
                false);

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && checkClick((int) event.x(), (int) event.y(), 105, 28, 10, 10)) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("cancel", true);
            Services.NETWORK.sendToServer(
                    new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
