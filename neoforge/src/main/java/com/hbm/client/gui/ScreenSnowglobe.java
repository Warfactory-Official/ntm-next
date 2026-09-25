// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.gui;

import com.hbm.blocks.generic.BlockSnowglobe.SnowglobeType;
import com.hbm.client.I18nClient;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntitySnowglobe;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;

public class ScreenSnowglobe extends Screen {

    private static final int SIZE_X = 300;
    private static final int SIZE_Y = 150;
    private static final int SLAB_COLOR = 0xCC003300;
    private static final int HEADING = 0xFF00FF00;
    private static final int BODY = 0xFF009900;
    private static final int WRAP_WIDTH = 280;

    private final BlockEntitySnowglobe snowglobe;

    public ScreenSnowglobe(BlockEntitySnowglobe snowglobe) {
        super(Component.translatable("desc.gui.snowglobe.nuclearTechCommemorative"));
        this.snowglobe = snowglobe;
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null) {
            this.minecraft
                    .getSoundManager()
                    .play(SimpleSoundInstance.forUI(ModSounds.BOBBLE.get(), 1.0F));
        }
    }

    @Override
    public void extractBackground(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - SIZE_X) / 2;
        int top = (this.height - SIZE_Y) / 2;
        graphics.fill(left, top, left + SIZE_X, top + SIZE_Y, SLAB_COLOR);

        int centre = left + SIZE_X / 2;
        int nextLevel = top + 10;

        line(graphics, "Nuclear Tech Commemorative Snowglobe", centre, nextLevel, HEADING);
        nextLevel += 10;

        SnowglobeType type = this.snowglobe.type;
        line(graphics, type.label, centre, nextLevel, BODY);
        nextLevel += 20;

        if (type.inscription != null) {
            line(
                    graphics,
                    "On the bottom is the following inscription:",
                    centre,
                    nextLevel,
                    HEADING);
            nextLevel += 10;
            for (String text :
                    I18nClient.autoBreakWithParagraphs(this.font, type.inscription, WRAP_WIDTH)) {
                line(graphics, text, centre, nextLevel, BODY);
                nextLevel += 10;
            }
        }
    }

    private void line(GuiGraphicsExtractor graphics, String text, int centre, int y, int color) {
        graphics.text(this.font, text, centre - this.font.width(text) / 2, y, color, true);
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
