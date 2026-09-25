// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitTooltip;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.material.Fluid;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public abstract class ScreenInfoContainer<T extends AbstractContainerMenu>
        extends AbstractContainerScreen<T> {

    private static final Identifier GUI_UTILITY = Library.id("textures/gui/gui_utility.png");
    private static final Identifier GAUGE_ROUND_SMALL =
            Library.id("textures/gui/gauges/small_round.png");
    private static final int GAUGE_ROUND_SMALL_FRAMES = 13;
    private static final int GAUGE_ROUND_SMALL_SIZE = 18;

    public ScreenInfoContainer(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    public ScreenInfoContainer(
            T menu, Inventory inventory, Component title, int imageWidth, int imageHeight) {
        super(menu, inventory, title, imageWidth, imageHeight);
    }

    protected static List<Component> resolveLines(String... keys) {
        List<Component> lines = new ArrayList<>(keys.length);
        for (String key : keys) lines.add(Component.literal(I18nUtil.resolveKey(key)));
        return lines;
    }

    protected static List<Component> lineArray(String key) {
        String[] parts = I18nUtil.resolveKeyArray(key);
        List<Component> lines = new ArrayList<>(parts.length);
        for (String part : parts) lines.add(Component.literal(part));
        return lines;
    }

    protected static List<Component> pelletLines(String key, int perHeat) {
        List<ItemRTGPellet> pellets = ItemRTGPellet.pelletList;
        List<Component> lines = new ArrayList<>(pellets.size() + 1);
        lines.add(Component.literal(I18nUtil.resolveKey("desc.gui.rtg.pellets")));
        for (ItemRTGPellet pellet : pellets) {
            lines.add(
                    Component.literal(
                            I18nUtil.resolveKey(
                                    key,
                                    I18nUtil.resolveKey(pellet.getDescriptionId()),
                                    pellet.getHeat() * perHeat)));
        }
        return lines;
    }

    private static String upgradeGuiKey(UpgradeType type) {
        return switch (type) {
            case SPEED -> "upgrade.gui.speed";
            case POWER -> "upgrade.gui.power";
            case EFFECT -> "upgrade.gui.effectiveness";
            case AFTERBURN -> "upgrade.gui.afterburner";
            case OVERDRIVE -> "upgrade.gui.overdrive";
            default -> null;
        };
    }

    protected static void playClick() {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        EditBox box = typingField();
        if (box == null || event.isEscape()) return super.keyPressed(event);
        if (box.keyPressed(event)) fieldKeyTaken(box);
        return true;
    }

    protected @Nullable EditBox typingField() {
        return getFocused() instanceof EditBox box && box.canConsumeInput() ? box : null;
    }

    protected void fieldKeyTaken(EditBox box) {}

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.drawTitle()) {
            int tx = this.titleCenterX() - this.font.width(this.title) / 2;
            graphics.text(this.font, this.title, tx, this.titleLabelY, this.titleColor(), false);
        }
        graphics.text(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                -12566464,
                false);
    }

    protected int titleCenterX() {
        return this.imageWidth / 2;
    }

    protected int titleColor() {
        return -12566464;
    }

    protected boolean drawTitle() {
        return true;
    }

    protected void submitEntity(
            GuiGraphicsExtractor graphics,
            EntityRenderState state,
            float scale,
            Vector3fc translation,
            Quaternionfc rotation,
            int left,
            int top,
            int sizeX,
            int sizeY) {
        graphics.entity(
                state,
                scale,
                translation,
                rotation,
                null,
                leftPos + left,
                topPos + top,
                leftPos + left + sizeX,
                topPos + top + sizeY);
    }

    protected boolean checkClick(int x, int y, int left, int top, int sizeX, int sizeY) {
        return leftPos + left <= x
                && leftPos + left + sizeX > x
                && topPos + top < y
                && topPos + top + sizeY >= y;
    }

    protected void drawCustomInfoStat(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            List<Component> lines) {
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                x,
                y,
                width,
                height,
                mouseX - leftPos,
                mouseY - topPos,
                lines);
    }

    protected void drawCustomInfoStat(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            int tPosX,
            int tPosY,
            List<Component> lines) {
        if (checkClick(mouseX, mouseY, x, y, width, height)) {
            graphics.setComponentTooltipForNextFrame(
                    this.font, lines, leftPos + tPosX, topPos + tPosY);
        }
    }

    protected List<Component> upgradeInfo(IUpgradeInfoProvider provider, UpgradeType... shown) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(I18nUtil.resolveKey("upgrade.gui.title")));
        int[] caps = provider.getValidUpgrades();
        for (UpgradeType type : shown) {
            String key = upgradeGuiKey(type);
            if (key == null) continue;
            int level = caps[type.ordinal()];
            if (level <= 0) continue;
            lines.add(Component.literal(I18nUtil.resolveKey(key, level)));
        }
        return lines;
    }

    protected static List<Component> priorityInfo(int ordinal) {
        String lang =
                switch (ConnectionPriority.VALUES[ordinal]) {
                    case LOW -> "low";
                    case HIGH -> "high";
                    default -> "normal";
                };
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("battery.priority." + lang));
        lines.add(Component.translatable("battery.priority.recommended"));
        lines.addAll(lineArray("battery.priority." + lang + ".desc"));
        return lines;
    }

    protected void drawInfoPanel(
            GuiGraphicsExtractor graphics, int x, int y, int w, int h, int index) {
        int u, v;
        switch (index) {
            case 0 -> {
                u = 0;
                v = 0;
            }
            case 1 -> {
                u = 0;
                v = 8;
            }
            case 2 -> {
                u = 8;
                v = 0;
            }
            case 3 -> {
                u = 24;
                v = 0;
            }
            case 4 -> {
                u = 0;
                v = 16;
            }
            case 5 -> {
                u = 0;
                v = 24;
            }
            case 6 -> {
                u = 8;
                v = 16;
            }
            case 7 -> {
                u = 24;
                v = 16;
            }
            case 8 -> {
                u = 0;
                v = 32;
            }
            case 9 -> {
                u = 0;
                v = 40;
            }
            case 10 -> {
                u = 8;
                v = 32;
            }
            case 11 -> {
                u = 24;
                v = 32;
            }
            default -> {
                return;
            }
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_UTILITY, x, y, u, v, w, h, 256, 256);
    }

    protected void drawFluidInfo(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, String... text) {
        Component[] lines = new Component[text.length];
        for (int i = 0; i < text.length; i++) lines[i] = Component.literal(text[i]);
        graphics.setComponentTooltipForNextFrame(this.font, List.of(lines), mouseX, mouseY);
    }

    protected void drawElectricityInfo(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            long power,
            long maxPower) {
        if (checkClick(mouseX, mouseY, x, y, width, height)) {
            drawFluidInfo(
                    graphics,
                    mouseX,
                    mouseY,
                    BobMathUtil.getShortNumber(power)
                            + "/"
                            + BobMathUtil.getShortNumber(maxPower)
                            + "HE");
        }
    }

    protected void drawFluidBar(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, FluidTankNTM tank) {
        Fluid type = tank.getFluid();
        int max = tank.getMaxFill();
        if (type == null || max <= 0 || tank.getFill() <= 0) return;
        int filled = (int) Math.min(height, (long) tank.getFill() * height / max);
        if (filled <= 0) return;
        FluidGauge.vertical(graphics, x, y + height - filled, width, filled, type);
    }

    protected void drawFluidBarH(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height, FluidTankNTM tank) {
        Fluid type = tank.getFluid();
        int max = tank.getMaxFill();
        if (type == null || max <= 0 || tank.getFill() <= 0) return;
        int filled = (int) Math.min(width, (long) tank.getFill() * width / max);
        if (filled <= 0) return;
        FluidGauge.horizontal(graphics, x, y, filled, height, type);
    }

    protected void drawRoundGauge(GuiGraphicsExtractor graphics, int x, int y, float progress) {
        int frame = Math.round((GAUGE_ROUND_SMALL_FRAMES - 1) * Mth.clamp(progress, 0F, 1F));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                GAUGE_ROUND_SMALL,
                x,
                y,
                0,
                frame * GAUGE_ROUND_SMALL_SIZE,
                GAUGE_ROUND_SMALL_SIZE,
                GAUGE_ROUND_SMALL_SIZE,
                GAUGE_ROUND_SMALL_SIZE,
                GAUGE_ROUND_SMALL_FRAMES * GAUGE_ROUND_SMALL_SIZE);
    }

    protected void drawFluidGaugeInfo(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int x,
            int y,
            int width,
            int height,
            FluidTankNTM tank) {
        if (!checkClick(mouseX, mouseY, x, y, width, height)) return;
        Fluid type = tank.getTankType();
        List<Component> lines = new ArrayList<>();

        lines.add(NTMFluidProperties.getDisplayName(tank.getFluid()));
        lines.add(Component.literal(tank.getFill() + "/" + tank.getMaxFill() + "mB"));

        if (tank.getPressure() != 0) {
            lines.add(
                    Component.translatable("desc.gui.infoContainer.pressure", tank.getPressure())
                            .withStyle(ChatFormatting.RED));
            lines.add(
                    Component.translatable("desc.shared.pressurizedUseCompressor")
                            .withStyle(
                                    BobMathUtil.getBlink()
                                            ? ChatFormatting.RED
                                            : ChatFormatting.DARK_RED));
        }
        FluidTraitTooltip.addInfo(type, lines::add);
        graphics.setTooltipForNextFrame(
                this.font,
                lines.stream().map(Component::getVisualOrderText).toList(),
                Optional.of(FluidTooltipFrame.of(type)),
                DefaultTooltipPositioner.INSTANCE,
                mouseX,
                mouseY,
                false,
                null);
    }
}
