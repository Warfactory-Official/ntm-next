// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.client.I18nClient;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemHolotapeImage.EnumHoloImage;
import com.hbm.sound.ModSounds;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;

public class ScreenHolotape extends Screen {

    private static final double SIZE_X = 300;
    private static final double SIZE_Y = 150;
    private static final int SLAB_COLOR = 0xCC003300;
    private static final int TEXT_COLOR = 0xFF009900;
    private static final int WRAP_WIDTH = 275;

    private final InteractionHand hand;
    private @Nullable EnumHoloImage holo;

    public ScreenHolotape(InteractionHand hand) {
        super(Component.empty());
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft == null || this.minecraft.player == null) return;
        this.minecraft
                .getSoundManager()
                .play(SimpleSoundInstance.forUI(ModSounds.BOBBLE.get(), 1.0F));
        this.holo = ModItems.HOLOTAPE_IMAGE.typeOf(this.minecraft.player.getItemInHand(this.hand));
        if (this.holo == null) this.onClose();
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.holo == null) return;
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        double left = (this.width - SIZE_X) / 2;
        double top = (this.height - SIZE_Y) / 2;
        int x0 = (int) Math.floor(left);
        int y0 = (int) Math.floor(top);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) (left - x0), (float) (top - y0));
        graphics.fill(x0, y0, x0 + (int) SIZE_X, y0 + (int) SIZE_Y, SLAB_COLOR);
        graphics.pose().popMatrix();

        int nextLevel = (int) top + 30;
        for (String text :
                I18nClient.autoBreak(this.font, I18n.get(this.holo.textKey()), WRAP_WIDTH)) {
            graphics.text(
                    this.font,
                    text,
                    (int) (left + SIZE_X / 2 - this.font.width(text) / 2),
                    nextLevel,
                    TEXT_COLOR,
                    true);
            nextLevel += 10;
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.minecraft != null && this.minecraft.options.keyInventory.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
