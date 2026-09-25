// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuMachinePlasmaForge;
import com.hbm.inventory.recipes.PlasmaForgeRecipe;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionPlasmaForge;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ScreenMachinePlasmaForge extends ScreenInfoContainer<MenuMachinePlasmaForge> {

    private static final Identifier TEXTURE =
            Library.id("textures/gui/reactors/gui_fusion_plasmaforge.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 62;
    private static final int TANK_X = 80, TANK_Y = 18, TANK_W = 16, TANK_H = 52;
    private static final int BTN_X = 7, BTN_Y = 80, BTN_W = 18, BTN_H = 18;
    private static final int PLASMA_STAT_X = 25, PLASMA_STAT_Y = 115, STAT_W = 18, STAT_H = 18;

    public ScreenMachinePlasmaForge(
            MenuMachinePlasmaForge menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 244);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    private static List<Component> toComponents(List<String> strings) {
        List<Component> out = new ArrayList<>(strings.size());
        for (String s : strings) out.add(Component.literal(s));
        return out;
    }

    @Override
    protected int titleCenterX() {
        return 70;
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

        BlockEntityFusionPlasmaForge be = menu.blockEntity();
        PlasmaForgeRecipe recipe = be.module.getRecipe() instanceof PlasmaForgeRecipe f ? f : null;

        if (be.maxPower > 0) {
            int p = (int) (be.power * POWER_H / be.maxPower);
            if (p > 0) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        POWER_X,
                        POWER_Y + (POWER_H - p),
                        176,
                        POWER_H - p,
                        POWER_W,
                        p,
                        256,
                        256);
            }
        }

        if (be.module.progress > 0) {
            int j = (int) Math.ceil(70 * be.module.progress);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 62, 81, 176.0F, 62.0F, j, 16, 256, 256);
        }

        if (be.didProcess) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 51, 76, 195.0F, 0.0F, 3, 6, 256, 256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 56, 76, 195.0F, 0.0F, 3, 6, 256, 256);
        } else if (recipe != null) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED, TEXTURE, 51, 76, 192.0F, 0.0F, 3, 6, 256, 256);
            if (be.power >= recipe.power) {
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        56,
                        76,
                        192.0F,
                        0.0F,
                        3,
                        6,
                        256,
                        256);
            }
        }

        double inputGauge =
                recipe == null
                        ? 0
                        : Math.min((double) be.plasmaEnergySync / (double) recipe.ignitionTemp, 1.5)
                                / 1.5D;
        double boosterGauge = be.maxBooster <= 0 ? 0 : (double) be.booster / (double) be.maxBooster;

        SmoothGaugeElement.draw(graphics, 34, 124, inputGauge, 5, 2, 1, 0xA00000, 0x000000);
        SmoothGaugeElement.draw(graphics, 70, 124, boosterGauge, 5, 2, 1, 0xA00000, 0x000000);

        graphics.item(
                recipe != null ? recipe.getIcon() : new ItemStack(ModItems.TEMPLATE_FOLDER),
                BTN_X + 1,
                BTN_Y + 1);

        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < recipe.inputItem.length && i < 12; i++) {
                Slot slot = menu.slots.get(BlockEntityFusionPlasmaForge.SLOT_INPUT_START + i);
                if (slot.hasItem()) continue;
                CountIngredient ingredient = recipe.inputItem[i];
                graphics.item(ingredient.extractForCyclingDisplay(20), slot.x, slot.y);
            }
        }

        drawFluidBar(graphics, TANK_X, TANK_Y, TANK_W, TANK_H, be.inputTank);

        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                be.power,
                be.maxPower);
        drawFluidGaugeInfo(graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, be.inputTank);

        String plasma =
                recipe != null
                        ? ChatFormatting.GREEN
                                + "-> "
                                + ChatFormatting.RESET
                                + BobMathUtil.getShortNumber(be.plasmaEnergySync)
                                + "TU / "
                                + BobMathUtil.getShortNumber(recipe.ignitionTemp)
                                + "TU"
                        : "0TU / 0TU";
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                PLASMA_STAT_X,
                PLASMA_STAT_Y,
                STAT_W,
                STAT_H,
                List.of(Component.literal(plasma)));

        super.extractLabels(graphics, mouseX, mouseY);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        Slot booster = menu.slots.get(BlockEntityFusionPlasmaForge.SLOT_BOOSTER);
        if (booster.hasItem()
                || !menu.getCarried().isEmpty()
                || !isHovering(booster.x, booster.y, 16, 16, mouseX, mouseY)) {
            return;
        }
        List<ItemStack> isotopes = new ArrayList<>();
        for (BlockEntityFusionPlasmaForge.Booster entry : BlockEntityFusionPlasmaForge.boosters()) {
            entry.ingredient().items().forEach(item -> isotopes.add(new ItemStack(item)));
        }
        GUIElements.drawCyclingStackText(
                graphics,
                this.font,
                I18nUtil.resolveKey("desc.gui.plasmaForge.boosterIsotope"),
                isotopes,
                mouseX,
                mouseY,
                this.width,
                this.height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0
                && checkClick((int) event.x(), (int) event.y(), BTN_X, BTN_Y, BTN_W, BTN_H)) {
            BlockEntityFusionPlasmaForge be = menu.blockEntity();
            String current = be.module.getRecipeName().isEmpty() ? null : be.module.getRecipeName();
            String pool =
                    ItemBlueprints.grabPool(
                            be.getItem(BlockEntityFusionPlasmaForge.SLOT_BLUEPRINT));
            GUIScreenRecipeSelector.openSelector(
                    PlasmaForgeRecipes.INSTANCE, be.getBlockPos(), current, 0, pool, this);
            playClick();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (!checkClick(mouseX, mouseY, BTN_X, BTN_Y, BTN_W, BTN_H)) return;

        PlasmaForgeRecipe recipe =
                menu.blockEntity().module.getRecipe() instanceof PlasmaForgeRecipe f ? f : null;
        if (recipe != null) {
            GUIElements.drawHoveringTextRecipe(
                    graphics,
                    this.font,
                    toComponents(recipe.print()),
                    mouseX,
                    mouseY,
                    this.width,
                    this.height);
        } else {
            graphics.setTooltipForNextFrame(
                    this.font,
                    Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW),
                    mouseX,
                    mouseY);
        }
    }
}
