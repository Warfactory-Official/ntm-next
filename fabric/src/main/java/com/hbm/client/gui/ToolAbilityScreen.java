/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.gui;

import com.hbm.client.InfoSystem;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.BaseAbility;
import com.hbm.handler.ability.ToolAreaAbility;
import com.hbm.handler.ability.ToolHarvestAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.items.tool.ToolAbilityItem;
import com.hbm.packet.ClientEventConstants;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.ToolPresetsLayout;
import com.hbm.sound.ModSounds;
import com.hbm.util.RegistryUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ToolAbilityScreen extends MenuScreen<HeldItemMenu> {

    public static final Identifier TEXTURE =
            RegistryUtil.id("textures/gui/tool/gui_tool_ability.png");

    private static final Row AREA =
            new Row(
                    true,
                    25,
                    List.of(
                            new Icon(ToolAreaAbility.NONE, 0, 91),
                            new Icon(ToolAreaAbility.VEIN_MINER, 32, 91),
                            new Icon(ToolAreaAbility.AOE, 64, 91),
                            new Icon(ToolAreaAbility.FLAT_AOE, 96, 91),
                            new Icon(ToolAreaAbility.EXPLOSION, 128, 91)));
    private static final Row HARVEST =
            new Row(
                    false,
                    45,
                    List.of(
                            new Icon(ToolHarvestAbility.NONE, 0, 107),
                            new Icon(ToolHarvestAbility.SILK_TOUCH, 32, 107),
                            new Icon(ToolHarvestAbility.FORTUNE, 64, 107),
                            new Icon(ToolHarvestAbility.AUTO_SMELTER, 96, 107),
                            new Icon(ToolHarvestAbility.AUTO_SHREDDER, 128, 107),
                            new Icon(ToolHarvestAbility.AUTO_CENTRIFUGE, 160, 107),
                            new Icon(ToolHarvestAbility.AUTO_CRYSTALLIZER, 192, 107),
                            new Icon(ToolHarvestAbility.MERCURY_TOUCH, 224, 107)));

    private static final int HEIGHT = 76;
    private static final int SHEET_WIDTH = 186;
    private static final int MAX_PRESETS = 99;
    private static final int INSET =
            20 * Math.max(AREA.icons().size() - 4, HARVEST.icons().size() - 8);
    private static final int WIDTH = SHEET_WIDTH + INSET;

    private final ItemStack tool;
    private final ToolAbilityItem item;
    private final AvailableAbilities abilities;
    private final List<ToolPreset> presets;

    private int current;
    private int left;
    private int top;

    public ToolAbilityScreen(HeldItemMenu menu, Inventory inventory, Component title) {
        super(menu, title);
        this.tool = menu.stack();
        this.item = (ToolAbilityItem) tool.getItem();
        this.abilities = item.abilities();
        this.presets = new ArrayList<>(item.presets(tool));
        this.current = item.currentPreset(tool);
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    private static boolean within(
            double mouseX, double mouseY, int x, int y, int width, int height) {
        return x <= mouseX && x + width > mouseX && y <= mouseY && y + height > mouseY;
    }

    private int buttonX(Command command) {
        return left + WIDTH - 86 + command.ordinal() * 11;
    }

    private @Nullable Command commandAt(double mouseX, double mouseY) {
        for (Command command : Command.VALUES) {
            if (within(mouseX, mouseY, buttonX(command), top + 11, 9, 9)) return command;
        }
        return null;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        super.extractBackground(graphics, mouseX, mouseY, partial);

        stretch(graphics, left, top, 0, 0, WIDTH, SHEET_WIDTH, HEIGHT, 74, 87);
        ToolPreset preset = presets.get(current);
        AREA.draw(this, graphics, preset, mouseX, mouseY);
        HARVEST.draw(this, graphics, preset, mouseX, mouseY);
        digits(graphics, current + 1, left + INSET + 115, top + 25);
        digits(graphics, presets.size(), left + INSET + 149, top + 25);

        Command command = commandAt(mouseX, mouseY);
        if (command != null)
            blit(graphics, buttonX(command), top + 11, 193 + command.ordinal() * 9, 0, 9, 9);

        Component tip = tip(preset, mouseX, mouseY, command);
        if (tip == null) return;
        int tipWidth = Math.max(6, font.width(tip));
        int tipX = left + WIDTH / 2 - tipWidth / 2;
        int tipY = top + HEIGHT + 5;
        stretch(graphics, tipX - 5, tipY - 4, 0, 76, tipWidth + 10, SHEET_WIDTH, 15, 3, 3);
        graphics.text(font, tip, tipX, tipY, 0xFFFFFFFF, false);
    }

    private @Nullable Component tip(
            ToolPreset preset, int mouseX, int mouseY, @Nullable Command command) {
        Component tip = AREA.tip(this, preset, mouseX, mouseY);
        if (tip == null) tip = HARVEST.tip(this, preset, mouseX, mouseY);
        if (tip == null && command != null) tip = Component.translatable(command.label);
        return tip;
    }

    private void stretch(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int u,
            int v,
            int width,
            int sheetWidth,
            int height,
            int keepLeft,
            int keepRight) {
        int tile = sheetWidth - keepLeft - keepRight;
        int rightEnd = x + width - keepRight;
        blit(graphics, x, y, u, v, keepLeft, height);
        for (int at = x + keepLeft; at < rightEnd; at += tile) {
            blit(graphics, at, y, u + keepLeft, v, Math.min(tile, rightEnd - at), height);
        }
        blit(graphics, rightEnd, y, u + sheetWidth - keepRight, v, keepRight, height);
    }

    private void digits(GuiGraphicsExtractor graphics, int number, int x, int y) {
        int shown = number + 100;
        blit(graphics, x, y, shown / 10 % 10 * 10, 123, 10, 15);
        blit(graphics, x + 12, y, shown % 10 * 10, 123, 10, 15);
    }

    private void blit(
            GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, u, v, width, height, 256, 256);
    }

    private boolean available(BaseAbility ability) {
        if (!item.canSelectAbility(tool, ability)) return false;
        if (!(ability instanceof ToolHarvestAbility) || ability == ToolHarvestAbility.NONE)
            return true;
        ToolPreset preset = presets.get(current);
        return preset.area().permitsHarvest();
    }

    private void play(SoundEvent sound, float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        ToolPreset preset = presets.get(current);
        preset = AREA.click(this, preset, event.x(), event.y());
        preset = HARVEST.click(this, preset, event.x(), event.y());
        if (!preset.area().permitsHarvest()) {
            preset = new ToolPreset(preset.area(), preset.areaLevel(), ToolHarvestAbility.NONE, 0);
        }
        presets.set(current, preset);

        Command command = commandAt(event.x(), event.y());
        if (command != null) {
            command.run(this);
            play(SoundEvents.UI_BUTTON_CLICK.value(), 0.5F);
        }

        if (!within(event.x(), event.y(), left, top, WIDTH, HEIGHT)) onClose();
        return true;
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
        PacketWire.sendToServer(ToolPresetsLayout.encode(menu.containerId, presets, current));

        ToolPreset active = presets.get(Math.clamp(current, 0, presets.size() - 1));
        InfoSystem.push(
                new InfoSystem.InfoEntry(
                        active.message(), ClientEventConstants.PLAYER_INFORM_DEFAULT_MILLIS),
                ClientEventConstants.PLAYER_INFORM_ID_TOOL_ABILITY);
        play(SoundEvents.EXPERIENCE_ORB_PICKUP, active.isNone() ? 0.75F : 1.25F);
        super.onClose();
    }

    private record Icon(BaseAbility ability, int u, int v) {}

    private record Row(boolean area, int y, List<Icon> icons) {

        BaseAbility selected(ToolPreset preset) {
            return area ? preset.area() : preset.harvest();
        }

        int level(ToolPreset preset) {
            return area ? preset.areaLevel() : preset.harvestLevel();
        }

        int slotX(ToolAbilityScreen screen, int slot) {
            return screen.left + 15 + 20 * slot;
        }

        @Nullable Icon iconAt(ToolAbilityScreen screen, double mouseX, double mouseY) {
            for (int slot = 0; slot < icons.size(); slot++) {
                if (within(mouseX, mouseY, slotX(screen, slot), screen.top + y, 16, 16))
                    return icons.get(slot);
            }
            return null;
        }

        @Nullable Component tip(
                ToolAbilityScreen screen, ToolPreset preset, double mouseX, double mouseY) {
            Icon icon = iconAt(screen, mouseX, mouseY);
            if (icon == null) return null;
            int level = icon.ability() == selected(preset) ? level(preset) : 0;
            return ToolPreset.name(icon.ability(), level)
                    .withStyle(
                            screen.available(icon.ability())
                                    ? ChatFormatting.WHITE
                                    : ChatFormatting.GRAY);
        }

        void draw(
                ToolAbilityScreen screen,
                GuiGraphicsExtractor graphics,
                ToolPreset preset,
                int mouseX,
                int mouseY) {
            int rowY = screen.top + y;
            for (int slot = 0; slot < icons.size(); slot++) {
                Icon icon = icons.get(slot);
                int x = slotX(screen, slot);
                boolean available = screen.available(icon.ability());
                boolean chosen = icon.ability() == selected(preset);

                screen.blit(graphics, x, rowY, icon.u() + (available ? 16 : 0), icon.v(), 16, 16);
                if (icon.ability().levels() > 1) {
                    int lit = chosen ? level(preset) + 1 : 0;
                    if (lit < 0 || lit > 10) lit = -1;
                    screen.blit(graphics, x + 17, rowY + 1, 188 + lit * 2, 70, 2, 14);
                }
                if (chosen) screen.blit(graphics, x - 1, rowY - 1, 220, 9, 18, 18);
                else if (available && within(mouseX, mouseY, x, rowY, 16, 16)) {
                    screen.blit(graphics, x - 1, rowY - 1, 238, 9, 18, 18);
                }
            }
        }

        ToolPreset click(
                ToolAbilityScreen screen, ToolPreset preset, double mouseX, double mouseY) {
            Icon icon = iconAt(screen, mouseX, mouseY);
            if (icon == null || !screen.available(icon.ability())) return preset;

            BaseAbility picked = icon.ability();
            boolean same = picked == selected(preset);
            int levels = Math.max(1, screen.abilities.maxLevel(picked) + 1);
            if (!same || levels > 1) screen.play(ModSounds.TECH_BOOP.get(), 2.0F);

            int next = same ? (level(preset) + 1) % levels : 0;
            return area
                    ? new ToolPreset(
                            (ToolAreaAbility) picked, next, preset.harvest(), preset.harvestLevel())
                    : new ToolPreset(
                            preset.area(), preset.areaLevel(), (ToolHarvestAbility) picked, next);
        }
    }

    private enum Command {
        RESET("desc.gui.toolAbility.resetAll") {
            @Override
            void run(ToolAbilityScreen screen) {
                screen.presets.clear();
                for (ToolPreset preset : screen.abilities.defaultPresets()) {
                    if (screen.item.canSelectAbility(screen.tool, preset.area())
                            && screen.item.canSelectAbility(screen.tool, preset.harvest()))
                        screen.presets.add(preset);
                }
                screen.current = 0;
            }
        },
        DELETE("desc.gui.toolAbility.deleteCurrent") {
            @Override
            void run(ToolAbilityScreen screen) {
                if (screen.presets.size() <= 1) return;
                screen.presets.remove(screen.current);
                screen.current = Math.min(screen.current, screen.presets.size() - 1);
            }
        },
        ADD("desc.gui.toolAbility.addNew") {
            @Override
            void run(ToolAbilityScreen screen) {
                if (screen.presets.size() >= MAX_PRESETS) return;
                screen.presets.add(++screen.current, ToolPreset.NONE);
            }
        },
        FIRST("desc.gui.toolAbility.selectFirst") {
            @Override
            void run(ToolAbilityScreen screen) {
                screen.current = 0;
            }
        },
        NEXT("desc.gui.toolAbility.nextPreset") {
            @Override
            void run(ToolAbilityScreen screen) {
                screen.current = Math.floorMod(screen.current + 1, screen.presets.size());
            }
        },
        PREVIOUS("desc.gui.toolAbility.previousPreset") {
            @Override
            void run(ToolAbilityScreen screen) {
                screen.current = Math.floorMod(screen.current - 1, screen.presets.size());
            }
        },
        CLOSE("desc.gui.toolAbility.closeWindow") {
            @Override
            void run(ToolAbilityScreen screen) {
                screen.onClose();
            }
        };

        static final Command[] VALUES = values();

        final String label;

        Command(String label) {
            this.label = label;
        }

        abstract void run(ToolAbilityScreen screen);
    }
}
