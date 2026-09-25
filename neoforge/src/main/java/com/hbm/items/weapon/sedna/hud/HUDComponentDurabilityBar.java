// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.hud;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.render.util.RenderScreenOverlay;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HUDComponentDurabilityBar implements IHUDComponent {

    protected boolean mirrored = false;

    public HUDComponentDurabilityBar() {
        this(false);
    }

    public HUDComponentDurabilityBar(boolean mirror) {
        this.mirrored = mirror;
    }

    @Override
    public int getComponentHeight(Player player, ItemStack stack) {
        return 5;
    }

    @Override
    public void renderHUDComponent(
            GuiGraphicsExtractor graphics,
            Player player,
            ItemStack stack,
            int bottomOffset,
            int gunIndex) {

        int pX = graphics.guiWidth() / 2 + (mirrored ? -(62 + 36 + 52) : (62 + 36));
        int pZ = graphics.guiHeight() - 21;

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        int dura =
                (int)
                        (50
                                * ItemGunBaseNT.getWear(stack, gunIndex)
                                / gun.getConfig(stack, gunIndex).getDurability(stack));

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                RenderScreenOverlay.misc,
                pX,
                pZ + 16,
                94F,
                0F,
                52,
                3,
                256,
                256);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                RenderScreenOverlay.misc,
                pX + 1,
                pZ + 16,
                95F,
                3F,
                50 - dura,
                3,
                256,
                256);
    }
}
