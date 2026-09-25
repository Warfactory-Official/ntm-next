// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuLaunchPadBase;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.lib.Library;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadBase;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public class ScreenLaunchPad extends ScreenInfoContainer<MenuLaunchPadBase<?>> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/weapon/gui_launch_pad_large.png");

    private static final int POWER_X = 107, POWER_Y = 36, POWER_W = 16, POWER_H = 52;
    private static final int FUEL_X = 125, OX_X = 143, TANK_Y = 36, TANK_W = 16, TANK_H = 52;
    private static final int STATUS_Y = 23, STATUS_W = 6, STATUS_H = 8;
    private static final int POWER_STATUS_X = 112, FUEL_STATUS_X = 130, OX_STATUS_X = 148;
    private static final int GREEN_U = 192, RED_U = 198;
    private static final int STATUS_TEXT_X = 34, STATUS_TEXT_Y = 107;
    private static final int PREVIEW_CX = 70, PREVIEW_CY = 120, PREVIEW_HW = 34, PREVIEW_HH = 102;
    private @Nullable Item previewItem;
    private @Nullable Entity previewEntity;

    public ScreenLaunchPad(MenuLaunchPadBase<?> menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 236);
        this.titleLabelY = 4;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static float previewScale(@Nullable Item item) {
        if (item == ModItems.MISSILE_STEALTH.get()) return 9.0F;
        if (item instanceof ItemMissile m) {
            return switch (m.formFactor) {
                case V2 -> 14.0F;
                case STRONG -> 11.0F;
                case HUGE -> 7.4F;
                case ATLAS -> 7.0F;
                case MICRO -> 20.0F;
                case ABM -> 11.6F;
                default -> 8.0F;
            };
        }
        return 8.0F;
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

        BlockEntityLaunchPadBase be = launchPad();
        if (be != null) {
            long power = be.power;
            int filled =
                    (int) Math.min(POWER_H, power * POWER_H / BlockEntityLaunchPadBase.MAX_POWER);
            if (filled > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - filled),
                        176,
                        POWER_H - filled,
                        POWER_W,
                        filled,
                        256,
                        256);
            }
            drawFluidBar(graphics, FUEL_X, TANK_Y, TANK_W, TANK_H, be.fuelTank);
            drawFluidBar(graphics, OX_X, TANK_Y, TANK_W, TANK_H, be.oxidizerTank);
            drawStatusIndicators(graphics, be);
        }

        if (be != null) {
            drawElectricityInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    POWER_X,
                    POWER_Y,
                    POWER_W,
                    POWER_H,
                    be.power,
                    BlockEntityLaunchPadBase.MAX_POWER);
            drawFluidGaugeInfo(
                    graphics, mouseX, mouseY, FUEL_X, TANK_Y, TANK_W, TANK_H, be.fuelTank);
            drawFluidGaugeInfo(
                    graphics, mouseX, mouseY, OX_X, TANK_Y, TANK_W, TANK_H, be.oxidizerTank);
        }

        drawMissilePreview(graphics);
        if (be != null) drawStatusText(graphics, be);
        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Slot designator = menu.getSlot(BlockEntityLaunchPadBase.SLOT_DESIGNATOR);
        if (!menu.getCarried().isEmpty()
                || designator.hasItem()
                || !isHovering(designator.x, designator.y, 16, 16, mouseX, mouseY)) {
            return;
        }
        GUIElements.drawCyclingStackText(
                graphics,
                this.font,
                null,
                List.of(
                        new ItemStack(ModItems.DESIGNATOR.get()),
                        new ItemStack(ModItems.DESIGNATOR_RANGE.get()),
                        new ItemStack(ModItems.DESIGNATOR_MANUAL.get())),
                mouseX,
                mouseY,
                this.width,
                this.height);
    }

    private void drawStatusText(GuiGraphicsExtractor graphics, BlockEntityLaunchPadBase be) {
        float scale;
        Component text;
        int color;
        switch (be.state) {
            case BlockEntityLaunchPadBase.STATE_LOADING -> {
                scale = 0.6F;
                text = Component.translatable("desc.gui.launchPad.loading");
                color = 0xFFFF8000;
            }

            case BlockEntityLaunchPadBase.STATE_READY -> {
                scale = 0.8F;
                text = Component.translatable("desc.gui.launchPad.ready");
                color = 0xFF0FF000;
            }
            default -> {
                scale = 0.5F;
                text = Component.translatable("desc.gui.launchPad.notReady");
                color = 0xFFFF0000;
            }
        }
        graphics.pose().pushMatrix();
        graphics.pose().translate(STATUS_TEXT_X, STATUS_TEXT_Y);
        graphics.pose().scale(scale, scale);
        graphics.text(
                this.font,
                text,
                -this.font.width(text) / 2,
                -this.font.lineHeight / 2,
                color,
                false);
        graphics.pose().popMatrix();
    }

    private void drawStatusIndicators(GuiGraphicsExtractor graphics, BlockEntityLaunchPadBase be) {
        ItemStack missile = menu.getSlot(BlockEntityLaunchPadBase.SLOT_MISSILE).getItem();
        if (!(missile.getItem() instanceof ItemMissile mi) || !mi.launchable) return;

        pip(graphics, POWER_STATUS_X, be.power >= BlockEntityLaunchPadBase.LAUNCH_POWER);
        if (mi.fuel != ItemMissile.MissileFuel.SOLID) {
            pip(graphics, FUEL_STATUS_X, be.fuelTank.getFill() >= mi.fuelCap);
            pip(graphics, OX_STATUS_X, be.oxidizerTank.getFill() >= mi.fuelCap);
        }
    }

    private void pip(GuiGraphicsExtractor graphics, int x, boolean ready) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                STATUS_Y,
                ready ? GREEN_U : RED_U,
                0,
                STATUS_W,
                STATUS_H,
                256,
                256);
    }

    private @Nullable BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private @Nullable BlockEntityLaunchPadBase launchPad() {
        BlockPos core = corePos();
        if (core != null
                && this.minecraft != null
                && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(core)
                        instanceof BlockEntityLaunchPadBase be) {
            return be;
        }
        return null;
    }

    private void drawMissilePreview(GuiGraphicsExtractor graphics) {
        if (this.minecraft == null || this.minecraft.level == null) return;
        ItemStack stack = menu.getSlot(BlockEntityLaunchPadBase.SLOT_MISSILE).getItem();
        Item item = stack.isEmpty() ? null : stack.getItem();
        if (item != previewItem) {
            previewItem = item;
            previewEntity =
                    item == null
                            ? null
                            : BlockEntityLaunchPadBase.createPreview(item, this.minecraft.level);
        }
        if (previewEntity == null) return;

        EntityRenderState rs =
                this.minecraft
                        .getEntityRenderDispatcher()
                        .getRenderer(previewEntity)
                        .createRenderState(previewEntity, 0F);
        rs.shadowPieces.clear();

        Quaternionf rotation =
                new Quaternionf().rotateZ(Mth.PI).rotateY((float) Math.toRadians(90));
        Vector3f translation = new Vector3f(0F, 0F, 0F);
        submitEntity(
                graphics,
                rs,
                previewScale(item),
                translation,
                rotation,
                PREVIEW_CX - PREVIEW_HW,
                PREVIEW_CY - PREVIEW_HH,
                PREVIEW_HW * 2,
                PREVIEW_HH * 2);
    }
}
