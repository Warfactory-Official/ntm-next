// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.items.special.ItemBookLore;
import com.hbm.lib.Library;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ScreenBookLore extends Screen {

    private static final Identifier TEXTURE = Library.id("textures/gui/book/book_lore.png");

    private static final int WIDTH = 272;
    private static final int HEIGHT = 182;
    private static final int SHEET = 512;

    private static final int WRAP = 100;

    private final ItemStack book;
    private final String key;
    private final int coverColor;
    private final int pages;
    private final int maxPage;
    private int page;
    private int left;
    private int top;

    public ScreenBookLore(Player player) {
        super(Component.empty());
        this.book = player.getMainHandItem();
        this.key = ItemBookLore.keyOf(book);
        this.coverColor = ItemBookLore.coverColorOf(book);
        this.pages = ItemBookLore.pagesOf(book);

        this.maxPage = (int) Math.ceil(pages / 2D) - 1;
    }

    @Override
    protected void init() {
        if (!(book.getItem() instanceof ItemBookLore) || pages <= 0) {
            onClose();
            return;
        }
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left,
                top,
                0.0F,
                0.0F,
                WIDTH,
                HEIGHT,
                SHEET,
                SHEET,
                0xFF000000 | coverColor);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left + 7,
                top + 7,
                0.0F,
                182.0F,
                258,
                165,
                SHEET,
                SHEET);

        boolean overY = mouseY >= top + 155 && mouseY < top + 165;
        if (page > 0) {
            boolean hover = overY && mouseX >= left + 24 && mouseX <= left + 42;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    left + 24,
                    top + 155,
                    hover ? 295.0F : 272.0F,
                    13.0F,
                    18,
                    10,
                    SHEET,
                    SHEET);
        }
        if (page < maxPage) {
            boolean hover = overY && mouseX >= left + 230 && mouseX <= left + 248;
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    left + 230,
                    top + 155,
                    hover ? 295.0F : 272.0F,
                    0.0F,
                    18,
                    10,
                    SHEET,
                    SHEET);
        }

        for (int half = 0; half < 2; half++) {
            int index = page * 2 + half;
            if (index >= pages) continue;

            List<String> args = ItemBookLore.argsOf(book, index);
            String page =
                    args.isEmpty()
                            ? I18n.get("book_lore." + key + ".page." + index)
                            : I18n.get("book_lore." + key + ".page." + index, args.toArray());
            List<String> lines = wrap(page);
            for (int line = 0; line < lines.size(); line++) {
                graphics.text(
                        font,
                        Component.literal(lines.get(line)),
                        left + 20 + half * 130,
                        top + 20 + 9 * line,
                        0xFF0F0F0F,
                        false);
            }
        }
    }

    private List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        lines.add(words[0]);
        int indent = font.width(words[0]);

        for (int w = 1; w < words.length; w++) {
            if (words[w].equals("$")) {
                if (w + 1 < words.length && !words[w + 1].equals("$")) {
                    lines.add(words[++w]);
                    indent = font.width(words[w]);
                } else {
                    lines.add("");
                }
                continue;
            }
            indent += font.width(" " + words[w]);
            if (indent <= WRAP) {
                lines.set(lines.size() - 1, lines.get(lines.size() - 1) + " " + words[w]);
            } else {
                lines.add(words[w]);
                indent = font.width(words[w]);
            }
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (mouseY >= top + 155 && mouseY < top + 165) {
            if (page > 0 && mouseX >= left + 24 && mouseX <= left + 42) {
                page--;
                turn();
                return true;
            }
            if (page < maxPage && mouseX >= left + 230 && mouseX <= left + 248) {
                page++;
                turn();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void turn() {
        if (minecraft != null) {
            minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft != null && minecraft.options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
