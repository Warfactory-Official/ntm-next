// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna.hud;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class HUDComponentAmmoCounter implements IHUDComponent {

    protected int receiver;
    protected boolean mirrored;
    protected boolean noCounter;

    public HUDComponentAmmoCounter(int receiver) {
        this.receiver = receiver;
    }

    public HUDComponentAmmoCounter mirror() {
        this.mirrored = true;
        return this;
    }

    public HUDComponentAmmoCounter noCounter() {
        this.noCounter = true;
        return this;
    }

    @Override
    public int getComponentHeight(Player player, ItemStack stack) {
        return 17;
    }

    @Override
    public void renderHUDComponent(
            GuiGraphicsExtractor graphics,
            Player player,
            ItemStack stack,
            int bottomOffset,
            int gunIndex) {

        int pX =
                graphics.guiWidth() / 2
                        + (mirrored ? -(62 + 36 + 52) : (62 + 36))
                        + (noCounter ? 14 : 0);
        int pZ = graphics.guiHeight() - bottomOffset - 18;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> mag =
                gun.getConfig(stack, gunIndex)
                        .getReceivers(stack)[this.receiver]
                        .getMagazine(stack);

        if (!noCounter)
            graphics.text(
                    Minecraft.getInstance().font,
                    mag.reportAmmoStateForHUD(stack, player),
                    pX + 17,
                    pZ + 6,
                    0xFFFFFFFF,
                    false);

        ItemStack icon = mag.getIconForHUD(stack, player);
        if (icon != null && !icon.isEmpty()) graphics.item(icon, pX, pZ);
    }
}
