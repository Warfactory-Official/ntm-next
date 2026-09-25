// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.turret.BlockEntityTurretArty;
import com.hbm.tileentity.turret.BlockEntityTurretBaseNT;
import com.hbm.tileentity.turret.BlockEntityTurretFritz;
import com.hbm.tileentity.turret.BlockEntityTurretHIMARS;
import com.hbm.util.I18nUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

public class ScreenTurretBase extends ScreenInfoContainer<MenuTurretBase> {

    private static final int ON_X = 115, ON_Y = 26;
    private static final int TOGGLE_X = 8, TOGGLE_Y = 30, TOGGLE = 10, TOGGLE_STEP = 14;
    private static final int POWER_X = 152, POWER_Y = 45, POWER_W = 16, POWER_H = 52;
    private static final int POWER_STEPS = 53, POWER_BOTTOM = 97, POWER_V = 52, POWER_U = 194;
    private static final int TANK_X = 134, TANK_Y = 63, TANK_W = 7, TANK_H = 52;
    private static final int LEFT_X = 7, RIGHT_X = 43, LIST_Y = 80, NAME_Y = 98, BUTTON = 18;
    private static final int MODE_X = 151, MODE_Y = 16;
    private static final int TALLY_X = 77, TALLY_Y = 50, TALLY_FULL = 36;
    private static final int FIELD_X = 10, FIELD_Y = 65, FIELD_W = 50, FIELD_H = 14, FIELD_MAX = 25;
    private static final int TEXT_GREEN = 0xFF00FF00;

    private static final Map<BlockEntityType<?>, String> SHEETS =
            Map.ofEntries(
                    Map.entry(ModBlockEntities.TURRET_CHEKHOV.get(), "base"),
                    Map.entry(ModBlockEntities.TURRET_FRIENDLY.get(), "friendly"),
                    Map.entry(ModBlockEntities.TURRET_FRITZ.get(), "fritz"),
                    Map.entry(ModBlockEntities.TURRET_HOWARD.get(), "howard"),
                    Map.entry(ModBlockEntities.TURRET_JEREMY.get(), "cannon"),
                    Map.entry(ModBlockEntities.TURRET_MAXWELL.get(), "maxwell"),
                    Map.entry(ModBlockEntities.TURRET_RICHARD.get(), "richard"),
                    Map.entry(ModBlockEntities.TURRET_SENTRY.get(), "sentry"),
                    Map.entry(ModBlockEntities.TURRET_TAUON.get(), "tau"),
                    Map.entry(ModBlockEntities.TURRET_ARTY.get(), "arty"),
                    Map.entry(ModBlockEntities.TURRET_HIMARS.get(), "himars"));

    private final Identifier texture;
    private EditBox field;
    private int index;

    public ScreenTurretBase(MenuTurretBase menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.texture =
                Library.id(
                        "textures/gui/weapon/gui_turret_"
                                + SHEETS.getOrDefault(menu.blockEntity().getType(), "base")
                                + ".png");
    }

    @Override
    protected void init() {
        super.init();
        field =
                new EditBox(
                        this.font,
                        leftPos + FIELD_X,
                        topPos + FIELD_Y,
                        FIELD_W,
                        FIELD_H,
                        Component.empty());
        field.setBordered(false);
        field.setMaxLength(FIELD_MAX);
        field.setTextColor(-1);
        field.setTextColorUneditable(-1);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                leftPos,
                topPos,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                256,
                256);

        BlockEntityTurretBaseNT turret = menu.blockEntity();

        if (checkClick(mouseX, mouseY, LEFT_X, LIST_Y, BUTTON, BUTTON))
            blit(graphics, LEFT_X, LIST_Y, 176, 58);
        if (checkClick(mouseX, mouseY, RIGHT_X, LIST_Y, BUTTON, BUTTON))
            blit(graphics, RIGHT_X, LIST_Y, 194, 58);
        if (checkClick(mouseX, mouseY, LEFT_X, NAME_Y, BUTTON, BUTTON))
            blit(graphics, LEFT_X, NAME_Y, 176, 76);
        if (checkClick(mouseX, mouseY, RIGHT_X, NAME_Y, BUTTON, BUTTON))
            blit(graphics, RIGHT_X, NAME_Y, 194, 76);

        int fill = turret.getPowerScaled(POWER_STEPS);
        if (fill > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    texture,
                    leftPos + POWER_X,
                    topPos + POWER_BOTTOM - fill,
                    POWER_U,
                    POWER_V - fill,
                    POWER_W,
                    fill,
                    256,
                    256);
        }

        if (turret.isOn) blit(graphics, ON_X, ON_Y, 176, 40);

        if (turret.targetPlayers) blit(graphics, TOGGLE_X, TOGGLE_Y, 176, 0, TOGGLE, TOGGLE);
        if (turret.targetAnimals)
            blit(graphics, TOGGLE_X + TOGGLE_STEP, TOGGLE_Y, 176, 10, TOGGLE, TOGGLE);
        if (turret.targetMobs)
            blit(graphics, TOGGLE_X + TOGGLE_STEP * 2, TOGGLE_Y, 176, 20, TOGGLE, TOGGLE);
        if (turret.targetMachines)
            blit(graphics, TOGGLE_X + TOGGLE_STEP * 3, TOGGLE_Y, 176, 30, TOGGLE, TOGGLE);

        drawTallies(graphics, turret.stattrak);

        if (turret instanceof BlockEntityTurretArty arty) {
            if (arty.mode == BlockEntityTurretArty.MODE_CANNON)
                blit(graphics, MODE_X, MODE_Y, 210, 0);
            if (arty.mode == BlockEntityTurretArty.MODE_MANUAL)
                blit(graphics, MODE_X, MODE_Y, 210, 18);
        } else if (turret instanceof BlockEntityTurretHIMARS himars) {
            if (himars.mode == BlockEntityTurretHIMARS.MODE_MANUAL)
                blit(graphics, MODE_X, MODE_Y, 210, 0);
        }

        if (turret instanceof BlockEntityTurretFritz fritz) {
            drawFluidBar(graphics, leftPos + TANK_X, topPos + TANK_Y, TANK_W, TANK_H, fritz.tank);
        }
    }

    private void drawTallies(GuiGraphicsExtractor graphics, int tallies) {
        if (tallies >= TALLY_FULL) {
            blit(graphics, TALLY_X, TALLY_Y, 176, 120, 63, 6);
            return;
        }

        int steps = (int) Math.ceil(tallies / 5D);
        for (int s = 0; s < steps; s++) {
            int m = tallies % 5;
            if (s < steps - 1 || m == 0) blit(graphics, TALLY_X + 9 * s, TALLY_Y, 194, 94, 9, 6);
            else blit(graphics, TALLY_X + 9 * s, TALLY_Y, 176, 94, m * 2, 6);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);

        BlockEntityTurretBaseNT turret = menu.blockEntity();
        drawElectricityInfo(
                graphics,
                mouseX,
                mouseY,
                POWER_X,
                POWER_Y,
                POWER_W,
                POWER_H,
                turret.power,
                turret.getMaxPower());

        String on = ChatFormatting.GREEN + I18nUtil.resolveKey("turret.on");
        String off = ChatFormatting.RED + I18nUtil.resolveKey("turret.off");
        targetTooltip(graphics, mouseX, mouseY, 0, "turret.players", turret.targetPlayers, on, off);
        targetTooltip(graphics, mouseX, mouseY, 1, "turret.animals", turret.targetAnimals, on, off);
        targetTooltip(graphics, mouseX, mouseY, 2, "turret.mobs", turret.targetMobs, on, off);
        targetTooltip(
                graphics, mouseX, mouseY, 3, "turret.machines", turret.targetMachines, on, off);

        String mode = modeKey(turret);
        if (mode != null) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    MODE_X,
                    MODE_Y,
                    BUTTON,
                    BUTTON,
                    lines(I18nUtil.resolveKeyArray("turret.arty." + mode)));
        }

        if (turret instanceof BlockEntityTurretFritz fritz) {
            drawFluidGaugeInfo(
                    graphics, mouseX, mouseY, TANK_X, TANK_Y, TANK_W, TANK_H, fritz.tank);
        }

        drawListing(graphics, turret);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        if (!menu.getCarried().isEmpty()) return;
        if (!(leftPos + 79 <= mouseX
                && leftPos + 79 + 54 > mouseX
                && topPos + 62 < mouseY
                && topPos + 62 + 54 >= mouseY)) {
            return;
        }

        for (int i = BlockEntityTurretBaseNT.SLOT_AMMO_FIRST;
                i <= BlockEntityTurretBaseNT.SLOT_AMMO_LAST;
                i++) {
            Slot slot = menu.slots.get(i);

            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.hasItem()) return;
        }

        GUIElements.drawCyclingStackText(
                graphics,
                this.font,
                null,
                menu.blockEntity().getAmmoTypesForDisplay(),
                mouseX,
                mouseY,
                this.width,
                this.height);
    }

    private void drawListing(GuiGraphicsExtractor graphics, BlockEntityTurretBaseNT turret) {
        List<String> names = turret.getWhitelist();
        String name = ChatFormatting.ITALIC + I18nUtil.resolveKey("turret.none");

        while (index >= count()) index--;
        if (index < 0) index = 0;
        if (names != null) name = names.get(index);

        String typed = field.getValue();
        if (field.isFocused()) {
            String caret = System.currentTimeMillis() % 1000 < 500 ? " " : "||";
            int at = field.getCursorPosition();
            typed = typed.substring(0, at) + caret + typed.substring(at);
        }

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        graphics.text(this.font, name, 12 * 2, 51 * 2, TEXT_GREEN, false);
        graphics.text(this.font, typed, 12 * 2, 69 * 2, TEXT_GREEN, false);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        int x = (int) event.x(), y = (int) event.y();

        field.setFocused(
                x >= leftPos + FIELD_X
                        && x < leftPos + FIELD_X + FIELD_W
                        && y >= topPos + FIELD_Y
                        && y < topPos + FIELD_Y + FIELD_H);

        if (checkClick(x, y, ON_X, ON_Y, BUTTON, BUTTON)) return toggle(0);

        for (int i = 0; i < 4; i++) {
            if (checkClick(x, y, TOGGLE_X + i * TOGGLE_STEP, TOGGLE_Y, TOGGLE, TOGGLE))
                return toggle(i + 1);
        }

        if (modeKey(menu.blockEntity()) != null && checkClick(x, y, MODE_X, MODE_Y, BUTTON, BUTTON))
            return toggle(5);

        int count = count();
        if (count > 0) {
            if (checkClick(x, y, LEFT_X, LIST_Y, BUTTON, BUTTON)) {
                index--;
                if (index < 0) index = count - 1;
                playClick();
                return true;
            }
            if (checkClick(x, y, RIGHT_X, LIST_Y, BUTTON, BUTTON)) {
                index++;
                index %= count;
                playClick();
                return true;
            }
        }

        if (checkClick(x, y, LEFT_X, NAME_Y, BUTTON, BUTTON)) {
            playClick();
            if (field.getValue().isEmpty()) return true;
            CompoundTag data = new CompoundTag();
            data.putString("name", field.getValue());
            send(data);
            field.setValue("");
            return true;
        }

        if (checkClick(x, y, RIGHT_X, NAME_Y, BUTTON, BUTTON)) {
            playClick();
            CompoundTag data = new CompoundTag();
            data.putInt("del", index);
            send(data);
            return true;
        }

        return handled;
    }

    @Override
    protected @Nullable EditBox typingField() {
        return field.canConsumeInput() ? field : null;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (field.isFocused() && field.charTyped(event)) return true;
        return super.charTyped(event);
    }

    private boolean toggle(int meta) {
        playClick();
        CompoundTag data = new CompoundTag();
        data.putInt("toggle", meta);
        send(data);
        return true;
    }

    private void send(CompoundTag data) {
        Services.NETWORK.sendToServer(
                new NbtControlPayload(menu.blockEntity().getBlockPos(), data));
    }

    private void targetTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            int slot,
            String key,
            boolean state,
            String on,
            String off) {
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                TOGGLE_X + slot * TOGGLE_STEP,
                TOGGLE_Y,
                TOGGLE,
                TOGGLE,
                lines(I18nUtil.resolveKeyArray(key, state ? on : off)));
    }

    private static String modeKey(BlockEntityTurretBaseNT turret) {
        if (turret instanceof BlockEntityTurretArty arty) {
            if (arty.mode == BlockEntityTurretArty.MODE_ARTILLERY) return "artillery";
            return arty.mode == BlockEntityTurretArty.MODE_CANNON ? "cannon" : "manual";
        }
        if (turret instanceof BlockEntityTurretHIMARS himars) {
            return himars.mode == BlockEntityTurretHIMARS.MODE_AUTO
                    ? "artillery_rocket"
                    : "manual_rocket";
        }
        return null;
    }

    private static List<Component> lines(String[] text) {
        return List.of(Arrays.stream(text).map(Component::literal).toArray(Component[]::new));
    }

    private int count() {
        List<String> names = menu.blockEntity().getWhitelist();
        return names == null ? 0 : names.size();
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v) {
        blit(graphics, x, y, u, v, BUTTON, BUTTON);
    }

    private void blit(
            GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                leftPos + x,
                topPos + y,
                u,
                v,
                width,
                height,
                256,
                256);
    }
}
