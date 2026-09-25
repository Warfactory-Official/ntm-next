// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.inventory.container.MenuRBMKConsole;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.NbtControlPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;

public class ScreenRBMKConsole extends ScreenInfoContainer<MenuRBMKConsole> {

    private static final Identifier TEXTURE = Library.id("textures/gui/rbmk/gui_rbmk_console.png");

    private final boolean[] selection = new boolean[15 * 15];
    private boolean az5Lid = true;
    private long lastPress = 0;
    private EditBox field;

    public ScreenRBMKConsole(MenuRBMKConsole menu, Inventory inv, Component title) {
        super(menu, inv, title, 244, 172);
    }

    private static void line(
            GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int wx = dx >= dy ? 0 : 1, wy = dx >= dy ? 1 : 0;
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1 + wx, y0 + 1 + wy, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static CompoundTag action(String name) {
        CompoundTag tag = new CompoundTag();
        tag.putString("action", name);
        return tag;
    }

    private static void click(float pitch) {
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    private static void sound(SoundEvent event, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, pitch));
    }

    @Override
    protected void init() {
        super.init();
        field = new EditBox(this.font, leftPos + 9, topPos + 84, 35, 9, Component.empty());
        field.setBordered(false);
        field.setMaxLength(3);
        field.setTextColor(CommonColors.GREEN);
        addRenderableWidget(field);
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
        BlockEntityRBMKConsole console = console();

        if (az5Lid)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    30,
                    138,
                    228.0F,
                    172.0F,
                    28,
                    28,
                    256,
                    256);
        else if (lastPress + 3000 >= System.currentTimeMillis())
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    30,
                    136,
                    228.0F,
                    228.0F,
                    28,
                    28,
                    256,
                    256);

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 2; k++) {
                int id = j * 2 + k;
                int off = console != null ? console.screens[id].type.offset : 0;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        6 + 40 * k,
                        8 + 21 * j,
                        off,
                        238.0F,
                        18,
                        18,
                        256,
                        256);
            }
        }

        if (console != null) {
            drawGrid(graphics, console);
            drawFluxGraph(graphics, console);
        }

        drawTooltips(graphics, mouseX, mouseY, console);
    }

    private void drawGrid(GuiGraphicsExtractor graphics, BlockEntityRBMKConsole console) {
        int bX = 86, bY = 11, size = 10;
        int idSteam = RBMKColumn.fluidId(NTMFluids.STEAM);
        int idHot = RBMKColumn.fluidId(NTMFluids.HOTSTEAM);
        int idSuper = RBMKColumn.fluidId(NTMFluids.SUPERHOTSTEAM);
        int idUltra = RBMKColumn.fluidId(NTMFluids.ULTRAHOTSTEAM);

        for (int i = 0; i < console.columns.length; i++) {
            RBMKColumn col = console.columns[i];
            if (col == null) continue;

            int x = bX + size * (i % 15);
            int y = bY + size * (i / 15);

            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x,
                    y,
                    col.type.offset,
                    172.0F,
                    size,
                    size,
                    256,
                    256);

            int h = (int) Math.min(Math.ceil((col.heat - 20) * 10 / col.maxHeat), 10);
            if (h > 0)
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y + size - h,
                        0.0F,
                        192.0F - h,
                        10,
                        h,
                        256,
                        256);

            switch (col.type) {
                case CONTROL -> {
                    short color = ((RBMKColumn.ControlColumn) col).color;
                    if (color > -1)
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                TEXTURE,
                                x,
                                y,
                                color * size,
                                202.0F,
                                10,
                                10,
                                256,
                                256);
                    drawControlLevel(graphics, x, y, ((RBMKColumn.ControlColumn) col).level);
                }
                case CONTROL_AUTO ->
                        drawControlLevel(graphics, x, y, ((RBMKColumn.ControlColumn) col).level);
                case FUEL, FUEL_SIM, BREEDER -> {
                    RBMKColumn.FuelColumn fuel = (RBMKColumn.FuelColumn) col;
                    if (fuel.c_maxHeat > 0) {
                        int fh =
                                Math.min(
                                        (int) Math.ceil((fuel.c_heat - 20) * 8 / fuel.c_maxHeat),
                                        8);
                        if (fh > 0)
                            graphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    TEXTURE,
                                    x + 1,
                                    y + size - fh - 1,
                                    11.0F,
                                    191.0F - fh,
                                    2,
                                    fh,
                                    256,
                                    256);
                        int fe = Math.min((int) Math.ceil(fuel.enrichment * 8), 8);
                        if (fe > 0)
                            graphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    TEXTURE,
                                    x + 4,
                                    y + size - fe - 1,
                                    14.0F,
                                    191.0F - fe,
                                    2,
                                    fe,
                                    256,
                                    256);
                        int fx = Math.min((int) Math.ceil(fuel.xenon * 8 / 100), 8);
                        if (fx > 0)
                            graphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    TEXTURE,
                                    x + 7,
                                    y + size - fx - 1,
                                    17.0F,
                                    191.0F - fx,
                                    2,
                                    fx,
                                    256,
                                    256);
                    }
                }
                case BOILER -> {
                    RBMKColumn.BoilerColumn b = (RBMKColumn.BoilerColumn) col;
                    if (b.maxWater > 0) {
                        int fw = (int) Math.ceil((double) b.water * 8 / b.maxWater);
                        if (fw > 0)
                            graphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    TEXTURE,
                                    x + 1,
                                    y + size - fw - 1,
                                    41.0F,
                                    191.0F - fw,
                                    3,
                                    fw,
                                    256,
                                    256);
                    }
                    if (b.maxSteam > 0) {
                        int fs = (int) Math.ceil((double) b.steam * 8 / b.maxSteam);
                        if (fs > 0)
                            graphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    TEXTURE,
                                    x + 6,
                                    y + size - fs - 1,
                                    46.0F,
                                    191.0F - fs,
                                    3,
                                    fs,
                                    256,
                                    256);
                    }
                    if (b.steamType == idSteam)
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                TEXTURE,
                                x + 4,
                                y + 1,
                                44.0F,
                                183.0F,
                                2,
                                2,
                                256,
                                256);
                    if (b.steamType == idHot)
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                TEXTURE,
                                x + 4,
                                y + 3,
                                44.0F,
                                185.0F,
                                2,
                                2,
                                256,
                                256);
                    if (b.steamType == idSuper)
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                TEXTURE,
                                x + 4,
                                y + 5,
                                44.0F,
                                187.0F,
                                2,
                                2,
                                256,
                                256);
                    if (b.steamType == idUltra)
                        graphics.blit(
                                RenderPipelines.GUI_TEXTURED,
                                TEXTURE,
                                x + 4,
                                y + 7,
                                44.0F,
                                189.0F,
                                2,
                                2,
                                256,
                                256);
                }
                case HEATEX -> {
                    RBMKColumn.HeaterColumn ht = (RBMKColumn.HeaterColumn) col;
                    if (ht.maxWater > 0) {
                        int cc = (int) Math.ceil((double) ht.water * 8 / ht.maxWater);
                        if (cc > 0)
                            graphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    TEXTURE,
                                    x + 1,
                                    y + size - cc - 1,
                                    131.0F,
                                    191.0F - cc,
                                    3,
                                    cc,
                                    256,
                                    256);
                    }
                    if (ht.maxSteam > 0) {
                        int hc = (int) Math.ceil((double) ht.steam * 8 / ht.maxSteam);
                        if (hc > 0)
                            graphics.blit(
                                    RenderPipelines.GUI_TEXTURED,
                                    TEXTURE,
                                    x + 6,
                                    y + size - hc - 1,
                                    136.0F,
                                    191.0F - hc,
                                    3,
                                    hc,
                                    256,
                                    256);
                    }
                }
                default -> {}
            }

            if (selection[i])
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        TEXTURE,
                        x,
                        y,
                        0.0F,
                        192.0F,
                        10,
                        10,
                        256,
                        256);
        }
    }

    private void drawControlLevel(GuiGraphicsExtractor graphics, int x, int y, double level) {
        int fr = 8 - (int) Math.ceil(level * 8);
        if (fr > 0)
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x + 4,
                    y + 1,
                    24.0F,
                    183.0F,
                    2,
                    fr,
                    256,
                    256);
    }

    private void drawFluxGraph(GuiGraphicsExtractor graphics, BlockEntityRBMKConsole console) {
        int[] flux = console.fluxBuffer;
        int highest = Integer.MIN_VALUE, lowest = Integer.MAX_VALUE;
        for (int f : flux) {
            if (f > highest) highest = f;
            if (f < lowest) lowest = f;
        }
        int range = Math.max(highest - lowest, 1);
        int color = CommonColors.GREEN;
        for (int i = 0; i < flux.length - 1; i++) {
            int x0 = (int) (7 + i * 74.0 / flux.length);
            int y0 = (int) (127 - (flux[i] - lowest) * 24.0 / range);
            int x1 = (int) (7 + (i + 1) * 74.0 / flux.length);
            int y1 = (int) (127 - (flux[i + 1] - lowest) * 24.0 / range);
            line(graphics, x0, y0, x1, y1, color);
        }

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        String hi = String.valueOf(highest);
        String lo = String.valueOf(lowest);
        graphics.text(this.font, hi, 16, 196, color, false);
        graphics.text(
                this.font, hi, (int) ((80 - this.font.width(hi) * 0.5F) / 0.5F), 196, color, false);
        graphics.text(
                this.font,
                lo,
                16,
                (int) ((133 - this.font.lineHeight * 0.5F) / 0.5F),
                color,
                false);
        graphics.text(
                this.font,
                lo,
                (int) ((80 - this.font.width(lo) * 0.5F) / 0.5F),
                (int) ((133 - this.font.lineHeight * 0.5F) / 0.5F),
                color,
                false);
        graphics.pose().popMatrix();
    }

    private void drawTooltips(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, BlockEntityRBMKConsole console) {

        if (console != null && checkClick(mouseX, mouseY, 86, 11, 150, 150)) {
            int lx = mouseX - leftPos - 86, ly = mouseY - topPos - 11;
            int index = lx / 10 + ly / 10 * 15;
            if (index >= 0 && index < console.columns.length && console.columns[index] != null) {
                List<Component> list = new ArrayList<>();
                list.add(Component.literal(console.columns[index].type.toString()));
                list.addAll(console.columns[index].getFancyStats());
                graphics.setComponentTooltipForNextFrame(this.font, list, mouseX, mouseY);
            }
        }

        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                61,
                70,
                10,
                10,
                List.of(Component.translatable("desc.gui.rbmkConsole.selectAllControlRods")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                72,
                70,
                10,
                10,
                List.of(Component.translatable("desc.gui.rbmkConsole.deselectAll")));
        drawCustomInfoStat(
                graphics,
                mouseX,
                mouseY,
                70,
                82,
                12,
                12,
                List.of(Component.translatable("desc.gui.rbmkConsole.cycleSteamChannel")));

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 2; k++) {
                int id = j * 2 + k;
                String type =
                        console != null
                                ? console.screens[id].type.name().toLowerCase(Locale.US)
                                : "none";
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        6 + 40 * k,
                        8 + 21 * j,
                        18,
                        18,
                        List.of(
                                Component.translatable("rbmk.console." + type, id + 1)
                                        .withStyle(ChatFormatting.YELLOW)));
                drawCustomInfoStat(
                        graphics,
                        mouseX,
                        mouseY,
                        24 + 40 * k,
                        8 + 21 * j,
                        18,
                        18,
                        List.of(Component.translatable("rbmk.console.assign", id + 1)));
            }
        }

        String[] groups = {"red", "yellow", "green", "blue", "purple"};
        ChatFormatting[] groupColors = {
            ChatFormatting.RED,
            ChatFormatting.YELLOW,
            ChatFormatting.GREEN,
            ChatFormatting.BLUE,
            ChatFormatting.LIGHT_PURPLE
        };
        for (int k = 0; k < 5; k++) {
            drawCustomInfoStat(
                    graphics,
                    mouseX,
                    mouseY,
                    6 + k * 11,
                    70,
                    10,
                    10,
                    List.of(
                            Component.translatable(
                                            "desc.gui.rbmkConsole.leftClickSelect", groups[k])
                                    .withStyle(groupColors[k]),
                            Component.translatable(
                                            "desc.gui.rbmkConsole.rightClickAssign", groups[k])
                                    .withStyle(groupColors[k])));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x(), my = (int) event.y();
        int button = event.button();
        BlockEntityRBMKConsole console = console();

        if (console != null && checkClick(mx, my, 86, 11, 150, 150)) {
            int index = (mx - leftPos - 86) / 10 + (my - topPos - 11) / 10 * 15;
            if (index >= 0 && index < selection.length && console.columns[index] != null) {
                selection[index] = !selection[index];
                click(0.75F + (selection[index] ? 0.25F : 0.0F));
                return true;
            }
        }

        if (checkClick(mx, my, 72, 70, 10, 10)) {
            Arrays.fill(selection, false);
            click(0.5F);
            return true;
        }

        if (console != null && checkClick(mx, my, 61, 70, 10, 10)) {
            selectWhere(console, c -> c.type == RBMKColumnType.CONTROL);
            click(1.5F);
            return true;
        }

        if (console != null && checkClick(mx, my, 70, 82, 12, 12)) {
            CompoundTag data = action("compressor");
            data.putIntArray("cols", selectedOfType(console, RBMKColumnType.BOILER));
            send(data);
            click(1.0F);
            return true;
        }

        for (int k = 0; k < 5; k++) {
            if (console != null && checkClick(mx, my, 6 + k * 11, 70, 10, 10)) {
                if (button == 0) {
                    int color = k;
                    selectWhere(
                            console,
                            c ->
                                    c.type == RBMKColumnType.CONTROL
                                            && ((RBMKColumn.ControlColumn) c).color == color);
                } else if (button == 1) {
                    CompoundTag data = action("color");
                    data.putInt("color", k);
                    data.putIntArray("cols", selectedOfType(console, RBMKColumnType.CONTROL));
                    send(data);
                }
                click(0.8F + k * 0.1F);
                return true;
            }
        }

        if (checkClick(mx, my, 30, 138, 28, 28)) {
            if (az5Lid) {
                az5Lid = false;
                sound(ModSounds.RBMK_AZ5_COVER.get(), 0.5F);
            } else if (lastPress + 3000 < System.currentTimeMillis() && console != null) {
                lastPress = System.currentTimeMillis();
                sound(ModSounds.RBMK_SHUTDOWN.get(), 1.0F);
                CompoundTag data = action("level");
                data.putDouble("level", 0);
                data.putIntArray("cols", selectedOfType(console, RBMKColumnType.CONTROL, true));
                send(data);
                click(1.0F);
            }
            return true;
        }

        if (checkClick(mx, my, 48, 82, 12, 12)) {
            try {
                double typed = Double.parseDouble(field.getValue());

                if (!Double.isFinite(typed)) return true;
                int pct = (int) Math.clamp(typed, 0, 100);
                field.setValue(String.valueOf(pct));
                CompoundTag data = action("level");
                data.putDouble("level", pct * 0.01D);
                data.putIntArray("cols", selectedIndices());
                send(data);
                click(1.0F);
            } catch (NumberFormatException ignored) {
            }
            return true;
        }

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 2; k++) {
                int id = j * 2 + k;
                if (checkClick(mx, my, 6 + 40 * k, 8 + 21 * j, 18, 18)) {
                    CompoundTag data = action("toggle");
                    data.putInt("slot", id);
                    send(data);
                    click(0.5F);
                    return true;
                }
                if (checkClick(mx, my, 24 + 40 * k, 8 + 21 * j, 18, 18)) {
                    CompoundTag data = action("assign");
                    data.putInt("slot", id);
                    data.putIntArray("cols", selectedIndices());
                    send(data);
                    click(0.75F);
                    return true;
                }
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void selectWhere(BlockEntityRBMKConsole console, ColPredicate pred) {
        for (int i = 0; i < selection.length; i++) {
            selection[i] = console.columns[i] != null && pred.test(console.columns[i]);
        }
    }

    private int[] selectedIndices() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < selection.length; i++) if (selection[i]) list.add(i);
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] selectedOfType(BlockEntityRBMKConsole console, RBMKColumnType type) {
        return selectedOfType(console, type, false);
    }

    private int[] selectedOfType(BlockEntityRBMKConsole console, RBMKColumnType type, boolean all) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < console.columns.length; i++) {
            RBMKColumn c = console.columns[i];
            if (c != null && c.type == type && (all || selection[i])) list.add(i);
        }
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    private void send(CompoundTag data) {
        BlockPos core = corePos();
        if (core != null) Services.NETWORK.sendToServer(new NbtControlPayload(core, data));
    }

    private BlockPos corePos() {
        return menu.blockEntity().getBlockPos();
    }

    private BlockEntityRBMKConsole console() {
        return menu.blockEntity();
    }

    private interface ColPredicate {
        boolean test(RBMKColumn col);
    }
}
