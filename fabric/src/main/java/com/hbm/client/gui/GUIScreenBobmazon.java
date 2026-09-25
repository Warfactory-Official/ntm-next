// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.handler.BobmazonOffer;
import com.hbm.lib.Library;
import com.hbm.packet.toserver.BobmazonPurchasePayload;
import com.hbm.platform.Services;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

public class GUIScreenBobmazon extends Screen {

    private static final Identifier TEXTURE = Library.id("textures/gui/gui_bobmazon.png");

    private static final int X_SIZE = 217;
    private static final int Y_SIZE = 229;
    private static final int TEXT = 0xFF404040;
    private static final int SUBTEXT = 0xFF222222;

    private final List<BobmazonOffer> offers;
    private final List<FolderButton> buttons = new ArrayList<>();
    private int guiLeft;
    private int guiTop;
    private int currentPage;

    public GUIScreenBobmazon(List<BobmazonOffer> offers) {
        super(Component.empty());
        this.offers = offers;
    }

    private int getPageCount() {
        return (offers.size() - 1) / 3;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - X_SIZE) / 2;
        this.guiTop = (this.height - Y_SIZE) / 2;
        updateButtons();
    }

    @Override
    public void tick() {
        if (currentPage < 0) currentPage = 0;
        if (currentPage > getPageCount()) currentPage = getPageCount();
    }

    private void updateButtons() {
        buttons.clear();

        for (int i = currentPage * 3; i < Math.min(currentPage * 3 + 3, offers.size()); i++) {
            buttons.add(
                    new FolderButton(
                            guiLeft + 34,
                            guiTop + 35 + (54 * i) - currentPage * 3 * 54,
                            offers.get(i),
                            i));
        }

        if (currentPage != 0) {
            buttons.add(new FolderButton(guiLeft + 25 - 18, guiTop + 26 + (27 * 3), 1, "Previous"));
        }
        if (currentPage != getPageCount()) {
            buttons.add(
                    new FolderButton(
                            guiLeft + 25 + (27 * 4) + 18 + 41, guiTop + 26 + (27 * 3), 2, "Next"));
        }
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
                0.0F,
                0.0F,
                X_SIZE,
                Y_SIZE,
                256,
                256);

        for (FolderButton b : buttons) b.drawButton(graphics, b.isMouseOn(mouseX, mouseY));
        for (FolderButton b : buttons) b.drawIcon(graphics);

        for (int d = currentPage * 3; d < Math.min(currentPage * 3 + 3, offers.size()); d++) {
            drawRequirement(
                    graphics,
                    offers.get(d),
                    guiLeft + 34,
                    guiTop + 53 + (54 * d) - currentPage * 3 * 54);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        String page = (currentPage + 1) + "/" + (getPageCount() + 1);
        graphics.text(
                this.font,
                page,
                guiLeft + X_SIZE / 2 - this.font.width(page) / 2,
                guiTop + 205,
                TEXT,
                false);

        for (FolderButton b : buttons) {
            if (b.isMouseOn(mouseX, mouseY) && b.info != null && !b.info.isEmpty()) {
                graphics.setComponentTooltipForNextFrame(
                        this.font, List.of(Component.literal(b.info)), mouseX, mouseY);
            }
        }
    }

    private void drawRequirement(GuiGraphicsExtractor graphics, BobmazonOffer offer, int x, int y) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x + 19,
                y - 4,
                217.0F,
                62.0F,
                39,
                8,
                256,
                256);
        if (offer.barWidth() > 0) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    x + 19,
                    y - 4,
                    217.0F,
                    54.0F,
                    offer.barWidth(),
                    8,
                    256,
                    256);
        }

        String count = offer.offer().getCount() > 1 ? " x" + offer.offer().getCount() : "";

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        graphics.text(
                this.font,
                offer.offer().getHoverName().getString() + count,
                (x + 20) * 2,
                (y - 12) * 2,
                TEXT,
                false);
        graphics.pose().popMatrix();

        String price = offer.cost() + " Cap" + (offer.cost() != 1 ? "s" : "");
        graphics.text(this.font, price, x + 62, y - 3, TEXT, false);

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.5F, 0.5F);
        if (!offer.author().isEmpty()) {
            graphics.text(
                    this.font, "- " + offer.author(), (x + 20) * 2, (y + 18) * 2, SUBTEXT, false);
        }
        graphics.text(this.font, offer.comment(), (x + 20) * 2, (y + 8) * 2, SUBTEXT, false);
        graphics.pose().popMatrix();

        graphics.item(offer.requirement().icon(), x + 1, y + 1);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x(), y = (int) event.y();
        for (FolderButton b : buttons) {
            if (b.isMouseOn(x, y)) {
                b.execute();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class FolderButton {

        private final int xPos;
        private final int yPos;
        private final int type;
        private final @Nullable String info;
        private final @Nullable BobmazonOffer offer;
        private final int index;

        FolderButton(int x, int y, int type, String info) {
            this.xPos = x;
            this.yPos = y;
            this.type = type;
            this.info = info;
            this.offer = null;
            this.index = -1;
        }

        FolderButton(int x, int y, BobmazonOffer offer, int index) {
            this.xPos = x;
            this.yPos = y;
            this.type = 0;
            this.info = null;
            this.offer = offer;
            this.index = index;
        }

        boolean isMouseOn(int mouseX, int mouseY) {
            return xPos <= mouseX && xPos + 18 > mouseX && yPos < mouseY && yPos + 18 >= mouseY;
        }

        void drawButton(GuiGraphicsExtractor graphics, boolean hovered) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    xPos,
                    yPos,
                    hovered ? 235.0F : 217.0F,
                    type == 1 ? 18.0F : (type == 2 ? 36.0F : 0.0F),
                    18,
                    18,
                    256,
                    256);
        }

        void drawIcon(GuiGraphicsExtractor graphics) {
            if (offer != null) graphics.item(offer.offer(), xPos + 1, yPos + 1);
        }

        void execute() {
            Minecraft.getInstance()
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (type == 0) {
                Services.NETWORK.sendToServer(new BobmazonPurchasePayload(index));
            } else if (type == 1) {
                if (currentPage > 0) currentPage--;
                updateButtons();
            } else if (type == 2) {
                if (currentPage < getPageCount()) currentPage++;
                updateButtons();
            }
        }
    }
}
