// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.items.tool.ItemGuideBook.BookType;
import com.hbm.lib.Library;
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
import org.apache.commons.lang3.math.NumberUtils;

public final class ScreenGuide extends Screen {

    private static final Identifier PAPER = Library.id("textures/gui/book/book.png");
    private static final Identifier COVER = Library.id("textures/gui/book/book_cover.png");
    private static final int WIDTH = 272;
    private static final int HEIGHT = 182;
    private final BookType type;
    private final List<GuideBookContents.GuidePage> pages;
    private final int lastPage;
    private int page = -1;
    private int left;
    private int top;

    public ScreenGuide(BookType type) {
        super(Component.translatable(type.title));
        this.type = type;
        pages = GuideBookContents.pages(type);
        lastPage = (pages.size() + 1) / 2 - 1;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                page < 0 ? COVER : PAPER,
                left,
                top,
                0.0F,
                0.0F,
                WIDTH,
                HEIGHT,
                512,
                512);
        if (page < 0) {
            float scale = type.titleScale;
            var lines = font.split(Component.translatable(type.title), Integer.MAX_VALUE);
            graphics.pose().pushMatrix();
            graphics.pose().scale(scale, scale);
            for (int i = 0; i < lines.size(); i++) {
                graphics.text(
                        font,
                        lines.get(i),
                        (int) ((left + WIDTH / 2 - font.width(lines.get(i)) / 2 * scale) / scale),
                        (int) ((top + 50 + i * 10 * scale) / scale),
                        0xFFFECE00,
                        false);
            }
            graphics.pose().popMatrix();
            return;
        }
        boolean arrowY = mouseY >= top + 155 && mouseY < top + 165;
        if (page > 0) {
            boolean hover = arrowY && mouseX >= left + 24 && mouseX < left + 42;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    PAPER,
                    left + 24,
                    top + 155,
                    hover ? 26.0F : 3.0F,
                    207.0F,
                    18,
                    10,
                    512,
                    512);
        }
        if (page < lastPage) {
            boolean hover = arrowY && mouseX >= left + 230 && mouseX < left + 248;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    PAPER,
                    left + 230,
                    top + 155,
                    hover ? 26.0F : 3.0F,
                    194.0F,
                    18,
                    10,
                    512,
                    512);
        }
        for (int side = 0; side < 2; side++) {
            int index = page * 2 + side;
            if (index >= pages.size()) continue;
            GuideBookContents.GuidePage sheet = pages.get(index);
            for (GuideBookContents.GuideText text : sheet.texts) {
                float scale = text.scale;
                float titleScale = overrideScale(sheet.titleScale, sheet.title + ".scale");
                float offset =
                        text.yOffset == -1
                                ? (sheet.title == null ? -10 : 6 / titleScale)
                                : text.yOffset;
                var lines =
                        font.split(Component.translatable(text.text), (int) (text.width * scale));
                graphics.pose().pushMatrix();
                graphics.pose().scale(1.0F / scale, 1.0F / scale);
                for (int line = 0; line < lines.size(); line++) {
                    graphics.text(
                            font,
                            lines.get(line),
                            (int) ((left + 20 + side * 130 + text.xOffset) * scale),
                            (int) ((top + 30 + offset) * scale + 12 * line),
                            0xFF404040,
                            false);
                }
                graphics.pose().popMatrix();
            }
            if (sheet.title != null) {
                float scale = sheet.titleScale;
                Component title = Component.translatable(sheet.title);
                graphics.pose().pushMatrix();
                graphics.pose().scale(1.0F / scale, 1.0F / scale);
                graphics.text(
                        font,
                        title,
                        (int)
                                ((left + 20 + side * 130 + 50 - font.width(title) / 2 / scale)
                                        * scale),
                        (int) ((top + 20) * scale),
                        0xFF000000 | sheet.titleColor,
                        false);
                graphics.pose().popMatrix();
            }
            for (GuideBookContents.GuideImage image : sheet.images) {
                int x = image.x == -1 ? 50 - image.sizeX / 2 : image.x;
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        image.image,
                        left + 20 + x + 130 * side,
                        top + image.y,
                        0.0F,
                        0.0F,
                        image.sizeX,
                        image.sizeY,
                        image.sizeX,
                        image.sizeY);
            }
            String number = (index + 1) + "/" + pages.size();
            graphics.text(
                    font,
                    number,
                    left + 44 + side * 185 - side * font.width(number),
                    top + 156,
                    0xFF404040,
                    false);
        }
    }

    private static float overrideScale(float fallback, String key) {
        String value = Component.translatable(key).getString();
        return NumberUtils.isCreatable(value) ? 1.0F / NumberUtils.toFloat(value) : fallback;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x();
        int y = (int) event.y();
        if (page < 0) {
            page = 0;
        } else if (y >= top + 155 && y < top + 165 && x >= left + 24 && x < left + 42 && page > 0) {
            page--;
        } else if (y >= top + 155
                && y < top + 165
                && x >= left + 230
                && x < left + 248
                && page < lastPage) {
            page++;
        } else {
            return super.mouseClicked(event, doubleClick);
        }
        minecraft
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
