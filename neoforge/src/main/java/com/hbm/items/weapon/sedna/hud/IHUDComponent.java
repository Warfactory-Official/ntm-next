// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IHUDComponent {

    int getComponentHeight(Player player, ItemStack stack);

    void renderHUDComponent(
            GuiGraphicsExtractor graphics,
            Player player,
            ItemStack stack,
            int bottomOffset,
            int configIndex);
}
