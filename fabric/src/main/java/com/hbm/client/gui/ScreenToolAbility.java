// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.client.InfoSystem;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.BaseAbility;
import com.hbm.handler.ability.ToolAreaAbility;
import com.hbm.handler.ability.ToolHarvestAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.lib.Library;
import com.hbm.packet.toclient.PlayerInformPayload;
import com.hbm.packet.toserver.ToolPresetPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public final class ScreenToolAbility extends Screen {

    public static final Identifier TEXTURE = Library.id("textures/gui/tool/gui_tool_ability.png");

    private static final List<Switch> AREA =
            List.of(
                    new Switch(ToolAreaAbility.NONE, 0, 91),
                    new Switch(ToolAreaAbility.RECURSION, 32, 91),
                    new Switch(ToolAreaAbility.HAMMER, 64, 91),
                    new Switch(ToolAreaAbility.HAMMER_FLAT, 96, 91),
                    new Switch(ToolAreaAbility.EXPLOSION, 128, 91));
    private static final List<Switch> HARVEST =
            List.of(
                    new Switch(ToolHarvestAbility.NONE, 0, 107),
                    new Switch(ToolHarvestAbility.SILK, 32, 107),
                    new Switch(ToolHarvestAbility.LUCK, 64, 107),
                    new Switch(ToolHarvestAbility.SMELTER, 96, 107),
                    new Switch(ToolHarvestAbility.SHREDDER, 128, 107),
                    new Switch(ToolHarvestAbility.CENTRIFUGE, 160, 107),
                    new Switch(ToolHarvestAbility.CRYSTALLIZER, 192, 107),
                    new Switch(ToolHarvestAbility.MERCURY, 224, 107));

    private static final int Y_SIZE = 76;
    private static final int BASE_X_SIZE = 186;
    private static final int MAX_PRESETS = 99;
    private static final int EXTRA_BUTTONS = 7;

    private final ItemStack tool;
    private final ItemToolAbility item;
    private final AvailableAbilities abilities;
    private final List<ToolPreset> presets;
    private final int insetWidth;
    private final int xSize;

    private int current;
    private int guiLeft;
    private int guiTop;
    private int hoverArea = -1;
    private int hoverHarvest = -1;
    private int hoverButton = -1;

    public ScreenToolAbility(ItemStack tool, ItemToolAbility item) {
        super(CommonComponents.EMPTY);
        this.tool = tool;
        this.item = item;
        this.abilities = item.abilities();
        this.presets = new ArrayList<>(item.presets(tool));
        this.current = item.currentPreset(tool);
        this.insetWidth = 20 * Math.max(AREA.size() - 4, HARVEST.size() - 8);
        this.xSize = BASE_X_SIZE + insetWidth;
    }

    private static boolean isInside(
            double mouseX, double mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }

    @Override
    protected void init() {
        guiLeft = (width - xSize) / 2;
        guiTop = (height - Y_SIZE) / 2;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        super.extractBackground(graphics, mouseX, mouseY, partial);

        drawStretched(graphics, guiLeft, guiTop, 0, 0, xSize, BASE_X_SIZE, Y_SIZE, 74, 87);

        ToolPreset preset = presets.get(current);
        hoverArea =
                drawSwitches(
                        graphics,
                        AREA,
                        preset.area(),
                        preset.areaLevel(),
                        guiLeft + 15,
                        guiTop + 25,
                        mouseX,
                        mouseY);
        hoverHarvest =
                drawSwitches(
                        graphics,
                        HARVEST,
                        preset.harvest(),
                        preset.harvestLevel(),
                        guiLeft + 15,
                        guiTop + 45,
                        mouseX,
                        mouseY);

        drawNumber(graphics, current + 1, guiLeft + insetWidth + 115, guiTop + 25);
        drawNumber(graphics, presets.size(), guiLeft + insetWidth + 149, guiTop + 25);

        int buttonsX = guiLeft + xSize - 86;
        hoverButton = -1;
        for (int i = 0; i < EXTRA_BUTTONS; i++) {
            if (!isInside(mouseX, mouseY, buttonsX + i * 11, guiTop + 11, 9, 9)) continue;
            hoverButton = i;
            blit(graphics, buttonsX + i * 11, guiTop + 11, 193 + i * 9, 0, 9, 9);
        }

        Component tooltip = tooltip(preset);
        if (tooltip == null) return;

        int tooltipWidth = Math.max(6, font.width(tooltip));
        int tooltipX = guiLeft + xSize / 2 - tooltipWidth / 2;
        int tooltipY = guiTop + Y_SIZE + 5;
        drawStretched(
                graphics, tooltipX - 5, tooltipY - 4, 0, 76, tooltipWidth + 10, 186, 15, 3, 3);
        graphics.text(font, tooltip, tooltipX, tooltipY, 0xFFFFFFFF, false);
    }

    private Component tooltip(ToolPreset preset) {
        if (hoverArea != -1) {
            BaseAbility ability = AREA.get(hoverArea).ability();
            return ToolPreset.name(ability, ability == preset.area() ? preset.areaLevel() : 0);
        }
        if (hoverHarvest != -1) {
            BaseAbility ability = HARVEST.get(hoverHarvest).ability();
            return ToolPreset.name(
                    ability, ability == preset.harvest() ? preset.harvestLevel() : 0);
        }
        return hoverButton == -1 ? null : Button.VALUES[hoverButton].label();
    }

    private void drawStretched(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int u,
            int v,
            int realWidth,
            int width,
            int height,
            int keepLeft,
            int keepRight) {
        int midWidth = width - keepLeft - keepRight;
        int realMidWidth = realWidth - keepLeft - keepRight;

        blit(graphics, x, y, u, v, keepLeft, height);
        for (int i = 0; i < realMidWidth; i += midWidth) {
            blit(
                    graphics,
                    x + keepLeft + i,
                    y,
                    u + keepLeft,
                    v,
                    Math.min(midWidth, realMidWidth - i),
                    height);
        }
        blit(
                graphics,
                x + keepLeft + realMidWidth,
                y,
                u + keepLeft + midWidth,
                v,
                keepRight,
                height);
    }

    private int drawSwitches(
            GuiGraphicsExtractor graphics,
            List<Switch> switches,
            BaseAbility selected,
            int level,
            int x,
            int y,
            int mouseX,
            int mouseY) {
        int hover = -1;

        for (int i = 0; i < switches.size(); i++) {
            Switch entry = switches.get(i);
            boolean available = available(entry.ability());
            boolean isSelected = entry.ability() == selected;

            blit(graphics, x + 20 * i, y, entry.u() + (available ? 16 : 0), entry.v(), 16, 16);

            if (entry.ability().levels() > 1) {
                int shown = isSelected ? level + 1 : 0;
                if (shown > 10 || shown < 0) shown = -1;
                blit(graphics, x + 20 * i + 17, y + 1, 188 + shown * 2, 5 * 14, 2, 14);
            }

            boolean hovered = isInside(mouseX, mouseY, x + 20 * i, y, 16, 16);
            if (hovered) hover = i;

            if (isSelected) {
                blit(graphics, x + 20 * i - 1, y - 1, 220, 9, 18, 18);
            } else if (available && hovered) {
                blit(graphics, x + 20 * i - 1, y - 1, 238, 9, 18, 18);
            }
        }

        return hover;
    }

    private void drawNumber(GuiGraphicsExtractor graphics, int number, int x, int y) {

        number += 100;
        blit(graphics, x, y, (number / 10) % 10 * 10, 123, 10, 15);
        blit(graphics, x + 12, y, number % 10 * 10, 123, 10, 15);
    }

    private void blit(
            GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, width, height, 256, 256);
    }

    private boolean available(BaseAbility ability) {
        if (!abilities.supports(ability)) return false;

        ToolPreset preset = presets.get(current);
        return !(ability instanceof ToolHarvestAbility)
                || ability == ToolHarvestAbility.NONE
                || preset.area().allowsHarvest(preset.areaLevel());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ToolPreset preset = presets.get(current);

        if (hoverArea != -1) {
            preset =
                    clicked(
                            preset,
                            AREA.get(hoverArea).ability(),
                            preset.area(),
                            preset.areaLevel(),
                            true);
        }
        if (hoverHarvest != -1) {
            preset =
                    clicked(
                            preset,
                            HARVEST.get(hoverHarvest).ability(),
                            preset.harvest(),
                            preset.harvestLevel(),
                            false);
        }
        if (!preset.area().allowsHarvest(preset.areaLevel())) {
            preset = new ToolPreset(preset.area(), preset.areaLevel(), ToolHarvestAbility.NONE, 0);
        }
        presets.set(current, preset);

        if (hoverButton != -1) {
            Button.VALUES[hoverButton].apply(this);
            minecraft
                    .getSoundManager()
                    .play(
                            SimpleSoundInstance.forUI(
                                    SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.5F));
        }

        if (!isInside(event.x(), event.y(), guiLeft, guiTop, xSize, Y_SIZE)) onClose();
        return true;
    }

    private ToolPreset clicked(
            ToolPreset preset, BaseAbility hovered, BaseAbility selected, int level, boolean area) {
        if (!available(hovered)) return preset;

        int levels = abilities.maxLevel(hovered) + 1;
        if (hovered != selected || levels > 1) {
            minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(ModSounds.TECH_BOOP.get(), 1.0F, 2.0F));
        }

        int next = hovered == selected ? (level + 1) % levels : 0;
        return area
                ? new ToolPreset(
                        (ToolAreaAbility) hovered, next, preset.harvest(), preset.harvestLevel())
                : new ToolPreset(
                        preset.area(), preset.areaLevel(), (ToolHarvestAbility) hovered, next);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY < 0) current = Math.max(0, current - 1);
        if (scrollY > 0) current = Math.min(presets.size() - 1, current + 1);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        item.setPresets(tool, presets, current);
        Services.NETWORK.sendToServer(new ToolPresetPayload(presets, current));

        ToolPreset active = presets.get(Math.clamp(current, 0, presets.size() - 1));
        InfoSystem.push(
                new InfoSystem.InfoEntry(active.message(), PlayerInformPayload.DEFAULT_MILLIS),
                PlayerInformPayload.ID_TOOL_ABILITY);
        minecraft
                .getSoundManager()
                .play(
                        SimpleSoundInstance.forUI(
                                SoundEvents.EXPERIENCE_ORB_PICKUP,
                                active.isNone() ? 0.75F : 1.25F,
                                0.25F));
        super.onClose();
    }

    private record Switch(BaseAbility ability, int u, int v) {}

    private enum Button {
        RESET("desc.gui.toolAbility.resetAll") {
            @Override
            void apply(ScreenToolAbility screen) {
                screen.presets.clear();
                screen.presets.addAll(screen.abilities.defaultPresets());
                screen.current = 0;
            }
        },
        DELETE("desc.gui.toolAbility.deleteCurrent") {
            @Override
            void apply(ScreenToolAbility screen) {
                if (screen.presets.size() <= 1) return;
                screen.presets.remove(screen.current);
                screen.current = Math.min(screen.current, screen.presets.size() - 1);
            }
        },
        ADD("desc.gui.toolAbility.addNew") {
            @Override
            void apply(ScreenToolAbility screen) {
                if (screen.presets.size() >= MAX_PRESETS) return;
                screen.presets.add(screen.current + 1, ToolPreset.NONE);
                screen.current += 1;
            }
        },
        FIRST("desc.gui.toolAbility.selectFirst") {
            @Override
            void apply(ScreenToolAbility screen) {
                screen.current = 0;
            }
        },
        NEXT("desc.gui.toolAbility.nextPreset") {
            @Override
            void apply(ScreenToolAbility screen) {
                screen.current = (screen.current + 1) % screen.presets.size();
            }
        },
        PREVIOUS("desc.gui.toolAbility.previousPreset") {
            @Override
            void apply(ScreenToolAbility screen) {
                screen.current =
                        (screen.current + screen.presets.size() - 1) % screen.presets.size();
            }
        },
        CLOSE("desc.gui.toolAbility.closeWindow") {
            @Override
            void apply(ScreenToolAbility screen) {
                screen.onClose();
            }
        };

        static final Button[] VALUES = values();

        private final String translationKey;

        Button(String translationKey) {
            this.translationKey = translationKey;
        }

        Component label() {
            return Component.translatable(translationKey);
        }

        abstract void apply(ScreenToolAbility screen);
    }
}
