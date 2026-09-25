// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRBMKControl;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class ScreenRBMKControl extends ScreenInfoContainer<MenuRBMKControl> {

    private static final Identifier TEXTURE = Library.id("textures/gui/rbmk/gui_rbmk_control.png");

    public ScreenRBMKControl(MenuRBMKControl menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
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

        BlockEntityRBMKControl be = be();
        double level = be != null ? be.level : 0;
        int height = (int) (56 * (1D - level));
        if (height > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    75,
                    29,
                    176,
                    56 - height,
                    8,
                    height,
                    256,
                    256);

        if (be != null && be.isPowered()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    87,
                    21,
                    196,
                    be.hasPower ? 16 : 0,
                    16,
                    16,
                    256,
                    256);
            if (checkClick(mouseX, mouseY, 87, 21, 16, 16)) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        87,
                        21,
                        16,
                        16,
                        List.of(
                                Component.literal(
                                        BobMathUtil.getShortNumber(be.power)
                                                + " / "
                                                + BobMathUtil.getShortNumber(
                                                        BlockEntityRBMKControl.maxPower)
                                                + "HE")));
            }
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                71,
                29,
                16,
                56,
                List.of(Component.literal((int) (level * 100) + "%")));

        if (be instanceof BlockEntityRBMKControlManual m && m.color != null) {
            int color = m.color.ordinal();
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    28,
                    26 + color * 11,
                    184,
                    color * 10,
                    12,
                    10,
                    256,
                    256);
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int mx = (int) event.x(), my = (int) event.y();
            for (int k = 0; k < 5; k++) {
                if (checkClick(mx, my, 118, 26 + k * 11, 30, 10)) {
                    sendLevel(1.0D - (k * 0.25D));
                    playClick();
                    return true;
                }

                if (checkClick(mx, my, 28, 26 + k * 11, 12, 10)) {
                    sendColor(k);
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void sendLevel(double level) {
        BlockPos core = corePos();
        if (core == null) return;
        CompoundTag data = new CompoundTag();
        data.putDouble("level", level);
        Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
    }

    private void sendColor(int ordinal) {
        BlockPos core = corePos();
        if (core == null) return;
        CompoundTag data = new CompoundTag();
        data.putInt("color", ordinal);
        Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private BlockEntityRBMKControl be() {
        return menu.blockEntity();
    }
}
