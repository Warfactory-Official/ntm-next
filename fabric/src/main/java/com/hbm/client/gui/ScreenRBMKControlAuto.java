// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRBMKControlAuto;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.util.BobMathUtil;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenRBMKControlAuto extends ScreenInfoContainer<MenuRBMKControlAuto> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/rbmk/gui_rbmk_control_auto.png");

    private final EditBox[] fields = new EditBox[4];

    public ScreenRBMKControlAuto(
            MenuRBMKControlAuto menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    protected void init() {
        super.init();
        BlockEntityRBMKControlAuto be = be();
        for (int i = 0; i < 4; i++) {
            EditBox field =
                    new EditBox(
                            this.font,
                            leftPos + 30,
                            topPos + 27 + 11 * i,
                            26,
                            10,
                            Component.empty());
            field.setBordered(false);
            field.setMaxLength(i < 2 ? 3 : 4);
            field.setTextColor(CommonColors.WHITE);
            addRenderableWidget(field);
            fields[i] = field;
        }
        if (be != null) {
            fields[0].setValue(String.valueOf((int) be.levelUpper));
            fields[1].setValue(String.valueOf((int) be.levelLower));
            fields[2].setValue(String.valueOf((int) be.heatUpper));
            fields[3].setValue(String.valueOf((int) be.heatLower));
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityRBMKControlAuto be = be();
        double level = be != null ? be.level : 0;
        int height = (int) (56 * (1D - level));
        if (height > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    124,
                    29,
                    176,
                    56 - height,
                    8,
                    height,
                    256,
                    256);

        int f = be != null ? be.function.ordinal() : 0;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, 59, 27, 184, f * 19, 26, 19, 256, 256);

        if (be != null && be.isPowered()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    136,
                    21,
                    210,
                    be.hasPower ? 16 : 0,
                    16,
                    16,
                    256,
                    256);
            if (checkClick(mouseX, mouseY, 136, 21, 16, 16)) {
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        136,
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
                124,
                29,
                16,
                56,
                List.of(Component.literal((int) (level * 100) + "%")));
        if (be != null) {
            String function =
                    switch (be.function) {
                        case LINEAR -> "linear";
                        case QUAD_UP -> "quadratic";
                        case QUAD_DOWN -> "inverseQuadratic";
                    };
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    58,
                    26,
                    28,
                    19,
                    List.of(
                            Component.translatable(
                                    "desc.gui.rbmkControlAuto.function",
                                    Component.translatable(
                                            "desc.gui.rbmkControlAuto." + function))));
        }
        String[] select = {"selectLinear", "selectQuadratic", "selectInverseQuadratic"};
        for (int k = 0; k < select.length; k++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    61,
                    48 + k * 11,
                    22,
                    10,
                    List.of(Component.translatable("desc.gui.rbmkControlAuto." + select[k])));
        }
        String[] field = {"levelUpper", "levelLower", "heatUpper", "heatLower"};
        for (int k = 0; k < field.length; k++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    28,
                    26 + k * 11,
                    30,
                    10,
                    lineArray("desc.gui.rbmkControlAuto." + field[k]));
        }
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                28,
                70,
                30,
                10,
                List.of(Component.translatable("desc.gui.rbmkControlAuto.save")));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int x = (int) event.x(), y = (int) event.y();
            if (checkClick(x, y, 28, 70, 30, 10)) {
                saveParameters();
                playClick();
                return true;
            }
            for (int k = 0; k < 3; k++) {
                if (checkClick(x, y, 61, 48 + k * 11, 22, 10)) {
                    BlockPos core = corePos();
                    if (core != null) {
                        CompoundTag data = new CompoundTag();
                        data.putInt("function", k);
                        Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
                    }
                    playClick();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void saveParameters() {
        BlockPos core = corePos();
        if (core == null) return;
        double[] vals = new double[4];
        for (int k = 0; k < 4; k++) {
            double clamp = k < 2 ? 100 : 9999;
            String text = fields[k].getValue();
            double v = 0;
            try {
                v = Math.clamp(Double.parseDouble(text), 0, clamp);
            } catch (NumberFormatException ignored) {
            }
            fields[k].setValue(String.valueOf((int) v));
            vals[k] = v;
        }
        CompoundTag data = new CompoundTag();
        data.putDouble("levelUpper", vals[0]);
        data.putDouble("levelLower", vals[1]);
        data.putDouble("heatUpper", vals[2]);
        data.putDouble("heatLower", vals[3]);
        Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private BlockEntityRBMKControlAuto be() {
        return menu.blockEntity();
    }
}
