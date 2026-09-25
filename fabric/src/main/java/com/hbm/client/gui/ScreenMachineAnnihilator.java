// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.container.MenuMachineAnnihilator;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineAnnihilator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class ScreenMachineAnnihilator extends ScreenInfoContainer<MenuMachineAnnihilator> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/processing/gui_annihilator.png");

    private EditBox pool;

    public ScreenMachineAnnihilator(
            MenuMachineAnnihilator menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 208);
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected int titleCenterX() {
        return 70;
    }

    @Override
    protected void init() {
        super.init();
        pool = new EditBox(this.font, leftPos + 31, topPos + 85, 80, 8, Component.empty());
        pool.setBordered(false);
        pool.setMaxLength(20);
        pool.setTextColor(CommonColors.GREEN);
        pool.setValue(annihilator().pool == null ? "" : annihilator().pool);
        pool.setResponder(this::sendPool);
        addRenderableWidget(pool);
    }

    private void sendPool(String text) {
        CompoundTag data = new CompoundTag();
        data.putString("pool", text);
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
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
        BlockEntityMachineAnnihilator be = annihilator();

        if (checkClick(mouseX, mouseY, 151, 35, 18, 18)) {
            ItemStack monitorItem = be.getItem(BlockEntityMachineAnnihilator.SLOT_MONITOR);
            if (!monitorItem.isEmpty()) {
                String name = monitorItem.getHoverName().getString();
                if (monitorItem.getItem() instanceof FluidIdentifierItem) {
                    FluidIdentifierData fid =
                            monitorItem.getOrDefault(
                                    ModDataComponents.FLUID_IDENTIFIER.get(),
                                    FluidIdentifierData.EMPTY);
                    Fluid primary = fid.primary();
                    if (primary != null && primary != Fluids.EMPTY)
                        name = NTMFluidProperties.clientName(primary);
                }
                graphics.setComponentTooltipForNextFrame(
                        this.font,
                        List.of(
                                Component.literal(name + ":"),
                                Component.literal(
                                        String.format(Locale.US, "%,d", be.monitorAmount))),
                        mouseX,
                        mouseY);
            }
        }

        super.extractLabels(graphics, mouseX, mouseY);
    }

    private BlockEntityMachineAnnihilator annihilator() {
        return menu.blockEntity();
    }
}
