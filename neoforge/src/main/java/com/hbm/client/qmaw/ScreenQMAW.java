// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.qmaw;

import com.hbm.lib.Library;
import com.hbm.qmaw.QMAWCatalog;
import com.hbm.qmaw.QuickManualAndWiki;
import com.hbm.sound.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class ScreenQMAW extends Screen {
    private static final Identifier TEXTURE = Library.id("textures/gui/gui_wiki.png");
    private static final Identifier FLIX = Library.id("textures/gui/gui_wiki_flix.png");
    private static final int WIDTH = 340, HEIGHT = 224;
    private QMAWCatalog catalog;
    private Identifier pageId;
    private QuickManualAndWiki page;
    private String language;
    private String heading;
    private @Nullable ItemStack headerIcon;
    private List<QMAWLayout.Line> lines = List.of();
    private final List<Identifier> back = new ArrayList<>(), forward = new ArrayList<>();
    private int left, top, scroll;
    private boolean leftDown, rightDown, dragging;

    public ScreenQMAW(QMAWCatalog catalog, Identifier page) {
        super(
                Component.literal(
                        localized(
                                catalog.page(page).title(),
                                Minecraft.getInstance().getLanguageManager().getSelected())));
        this.catalog = catalog;
        this.pageId = page;
        this.heading = super.getTitle().getString();
    }

    @Override
    public Component getTitle() {
        return Component.literal(heading);
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        rebuild();
    }

    public static String localized(Map<String, String> values, String language) {
        return values.getOrDefault(
                language,
                values.getOrDefault(
                        "en_us",
                        Component.translatable("gui.hbm.qmaw.missing_localization").getString()));
    }

    private void rebuild() {
        page = catalog.page(pageId);
        language = Minecraft.getInstance().getLanguageManager().getSelected();
        heading = localized(page.title(), language);
        headerIcon = page.icon().map(ItemStackTemplate::create).orElse(null);
        lines =
                QMAWLayout.build(
                        font,
                        WIDTH - 29,
                        catalog.compose(pageId, language, localized(page.content(), language)),
                        catalog,
                        pageId);
        scroll = Math.clamp(scroll, 0, Math.max(0, lines.size() - 1));
    }

    @Override
    public void tick() {
        QMAWCatalog next = QMAWClient.catalog();
        if (next.page(pageId) == null) {
            onClose();
            return;
        }
        if (next != catalog
                || !language.equals(Minecraft.getInstance().getLanguageManager().getSelected())) {
            catalog = next;
            back.removeIf(id -> catalog.page(id) == null);
            forward.removeIf(id -> catalog.page(id) == null);
            rebuild();
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xE0000000);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0, 0, 170, HEIGHT, 256, 256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left + 170,
                top,
                52,
                0,
                30,
                HEIGHT,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left + 200,
                top,
                52,
                0,
                140,
                HEIGHT,
                256,
                256);
        if (!back.isEmpty())
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    left + 3,
                    top + 3,
                    204,
                    0,
                    18,
                    18,
                    256,
                    256);
        if (!forward.isEmpty())
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    left + 21,
                    top + 3,
                    222,
                    0,
                    18,
                    18,
                    256,
                    256);
        int slider = 25 + (lines.size() < 2 ? 0 : (int) (180.0 * scroll / (lines.size() - 1)));
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                left + WIDTH - 15,
                top + slider,
                192,
                0,
                12,
                16,
                256,
                256);
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int headingX = 43, headingY = 5;
        if (headerIcon != null) {
            var icon = headerIcon;
            graphics.item(icon, left + 43, top + 4);
            graphics.itemDecorations(font, icon, left + 43, top + 4);
            headingX += 18;
            headingY += (16 - font.lineHeight) / 2;
        }
        graphics.text(font, heading, left + headingX, top + headingY, 0xFFFFFFFF, false);
        if (atEnd()) {
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    FLIX,
                    left + 60,
                    top + HEIGHT - 84,
                    0,
                    0,
                    80,
                    80,
                    256,
                    256);
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    FLIX,
                    left + 140,
                    top + HEIGHT - 60,
                    0,
                    80,
                    77,
                    39,
                    256,
                    256);
            return;
        }
        int y = top + 30;
        for (int i = scroll; i < lines.size(); i++) {
            var line = lines.get(i);
            if (y + line.height() > top + 219) break;
            int x = left + 7;
            for (var span : line.spans()) {
                int elementY = y + (line.height() - span.height()) / 2;
                boolean hover = contains(mouseX, mouseY, x, elementY, span.width(), span.height());
                int textX = x, textY = elementY;
                if (span.icon() != null) {
                    graphics.item(span.icon(), x, elementY - 1);
                    graphics.itemDecorations(font, span.icon(), x, elementY - 1);
                    textX += 18;
                    textY += (16 - font.lineHeight) / 2;
                }
                int color =
                        !span.link()
                                ? 0xFFFFFFFF
                                : span.target() == null
                                        ? 0xFFFF7F7F
                                        : span.target().equals(pageId)
                                                ? 0xFFA0A0A0
                                                : hover ? 0xFFFFD800 : 0xFF0094FF;
                graphics.text(font, span.text(), textX, textY, color, false);
                x += span.width();
            }
            y += line.height() + 2;
        }
    }

    private static boolean contains(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y > top && y <= top + height;
    }

    private boolean atEnd() {
        return lines.size() > 1 && scroll == lines.size() - 1;
    }

    private void navigate(Identifier id) {
        if (catalog.page(id) == null) return;
        pageId = id;
        scroll = 0;
        rebuild();
        Minecraft.getInstance()
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
    }

    private void back() {
        if (back.isEmpty()) return;
        forward.add(pageId);
        navigate(back.removeLast());
    }

    private void forward() {
        if (forward.isEmpty()) return;
        back.add(pageId);
        navigate(forward.removeLast());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) leftDown = true;
        if (event.button() == 1) rightDown = true;
        if (contains(event.x(), event.y(), left + 3, top + 3, 18, 18)) {
            back();
            return true;
        }
        if (contains(event.x(), event.y(), left + 21, top + 3, 18, 18)) {
            forward();
            return true;
        }
        if (atEnd() && contains(event.x(), event.y(), left + 60, top + HEIGHT - 84, 80, 80)) {
            Minecraft.getInstance().getSoundManager().play(new SingerSound());
            return true;
        }
        if (event.button() == 0 && onScrollBar(event.x(), event.y())) {
            dragging = true;
            drag(event.y());
            return true;
        }
        if (event.button() == 0 && !atEnd()) {
            int y = top + 30;
            for (int i = scroll; i < lines.size(); i++) {
                var line = lines.get(i);
                if (y + line.height() > top + 219) break;
                int x = left + 7;
                for (var span : line.spans()) {
                    if (span.target() != null
                            && !span.target().equals(pageId)
                            && contains(
                                    event.x(),
                                    event.y(),
                                    x,
                                    y + (line.height() - span.height()) / 2,
                                    span.width(),
                                    span.height())) {
                        back.add(pageId);
                        forward.clear();
                        navigate(span.target());
                        return true;
                    }
                    x += span.width();
                }
                y += line.height() + 2;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean onScrollBar(double x, double y) {
        return contains(x, y, left + WIDTH - 15, top + 25, 12, 191);
    }

    private void drag(double y) {
        scroll =
                Math.clamp(
                        (int)
                                Math.round(
                                        (lines.size() - 1)
                                                * Math.clamp((y - top - 33) / 175.0, 0, 1)),
                        0,
                        Math.max(0, lines.size() - 1));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (leftDown && (dragging || onScrollBar(event.x(), event.y()))) {
            dragging = true;
            drag(event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            leftDown = false;
            dragging = false;
        }
        if (event.button() == 1) rightDown = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (leftDown || rightDown || vertical == 0) return false;
        scroll = Math.clamp(scroll + (vertical > 0 ? -1 : 1), 0, Math.max(0, lines.size() - 1));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            back();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            forward();
            return true;
        }
        if (event.isEscape() || Minecraft.getInstance().options.keyInventory.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static final class SingerSound extends SimpleSoundInstance {
        private SingerSound() {

            super(
                    ModSounds.QMAW_SINGER.get().location(),
                    SoundSource.PLAYERS,
                    0.25F,
                    1F,
                    SoundInstance.createUnseededRandom(),
                    false,
                    0,
                    SoundInstance.Attenuation.NONE,
                    0,
                    0,
                    0,
                    true);
        }
    }
}
