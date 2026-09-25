// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemDesignatorManual;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.DesignatorCoordPayload;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import static com.hbm.items.tool.ItemDesignatorManual.ADD;
import static com.hbm.items.tool.ItemDesignatorManual.REFERENCE_X;
import static com.hbm.items.tool.ItemDesignatorManual.REFERENCE_Z;
import static com.hbm.items.tool.ItemDesignatorManual.SET_HERE;
import static com.hbm.items.tool.ItemDesignatorManual.SUBTRACT;

public class ScreenManualDesignator extends Screen {

    private static final Identifier TEXTURE = Library.id("textures/gui/gui_designator.png");
    private static final int X_SIZE = 176, Y_SIZE = 178;
    private static final int BUTTON = 18;
    private static final int[] COLUMNS = {25, 52, 79, 106, 133};
    private static final int[] STEPS = {1, 5, 10, 50, 100};
    private static final List<FolderButton> BUTTONS = buttons();

    private static List<FolderButton> buttons() {
        List<FolderButton> buttons = new ArrayList<>();
        for (int reference : new int[] {REFERENCE_X, REFERENCE_Z}) {
            int dy = reference * 72;
            for (int i = 0; i < COLUMNS.length; i++) {
                buttons.add(
                        new FolderButton(COLUMNS[i], 26 + dy, i, ADD, reference, STEPS[i], null));
            }
            for (int i = 0; i < COLUMNS.length; i++) {
                buttons.add(
                        new FolderButton(
                                COLUMNS[i], 62 + dy, 5 + i, SUBTRACT, reference, STEPS[i], null));
            }
            buttons.add(
                    new FolderButton(
                            133,
                            44 + dy,
                            10,
                            SET_HERE,
                            reference,
                            0,
                            reference == REFERENCE_X
                                    ? "desc.gui.designator.setX"
                                    : "desc.gui.designator.setZ"));
        }
        return List.copyOf(buttons);
    }

    private final InteractionHand hand;
    private int guiLeft;
    private int guiTop;
    private long shown;

    public ScreenManualDesignator(InteractionHand hand) {
        super(Component.empty());
        this.hand = hand;
    }

    private record FolderButton(
            int x,
            int y,
            int type,
            int operator,
            int reference,
            int value,
            @Nullable String info) {}

    private boolean hovered(FolderButton button, double mouseX, double mouseY) {
        int x = guiLeft + button.x;
        int y = guiTop + button.y;
        return x <= mouseX && x + BUTTON > mouseX && y < mouseY && y + BUTTON >= mouseY;
    }

    private ItemStack designator() {
        ItemStack stack = minecraft.player.getItemInHand(hand);
        return stack.getItem() instanceof ItemDesignatorManual ? stack : ItemStack.EMPTY;
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - X_SIZE) / 2;
        guiTop = (height - Y_SIZE) / 2;
        shown = designator().getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L);
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                guiLeft,
                guiTop,
                0,
                0,
                X_SIZE,
                Y_SIZE,
                256,
                256);
        for (FolderButton button : BUTTONS) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    guiLeft + button.x,
                    guiTop + button.y,
                    hovered(button, mouseX, mouseY) ? 176 + BUTTON : 176,
                    button.type * BUTTON,
                    BUTTON,
                    BUTTON,
                    256,
                    256);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (FolderButton button : BUTTONS) {
            if (button.info != null && hovered(button, mouseX, mouseY)) {
                graphics.setComponentTooltipForNextFrame(
                        font, List.of(Component.translatable(button.info)), mouseX, mouseY);
            }
        }
        label(graphics, "X: " + IDesignatorItem.unpackX(shown), guiTop + 50);
        label(graphics, "Z: " + IDesignatorItem.unpackZ(shown), guiTop + 50 + 18 * 4);
    }

    private void label(GuiGraphicsExtractor graphics, String text, int y) {
        graphics.text(
                font, text, guiLeft + X_SIZE / 2 - font.width(text) / 2, y, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean clicked = false;
        for (FolderButton button : BUTTONS) {
            if (!hovered(button, event.x(), event.y())) continue;
            minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Services.NETWORK.sendToServer(
                    new DesignatorCoordPayload(
                            hand, button.operator, button.value, button.reference));
            shown =
                    ItemDesignatorManual.apply(
                            shown,
                            button.operator,
                            button.value,
                            button.reference,
                            minecraft.player.getX(),
                            minecraft.player.getZ());
            clicked = true;
        }
        return clicked;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void tick() {
        if (designator().isEmpty()) onClose();
    }
}
