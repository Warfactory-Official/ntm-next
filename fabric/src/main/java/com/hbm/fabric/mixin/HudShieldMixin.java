// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.fabric.mixin;

import com.hbm.client.ClientRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Hud.class)
abstract class HudShieldMixin {

    @ModifyVariable(method = "extractArmor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int hbm$insertShield(
            int yLineBase,
            GuiGraphicsExtractor graphics,
            Player player,
            int originalY,
            int rows,
            int rowHeight,
            int left) {
        int top = yLineBase - (rows - 1) * rowHeight - 10;
        return ClientRegistry.renderShieldBar(graphics, top) ? yLineBase - 10 : yLineBase;
    }
}
