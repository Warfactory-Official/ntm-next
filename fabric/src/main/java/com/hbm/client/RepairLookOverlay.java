// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.interfaces.ILookOverlay.LookInfo;
import com.hbm.interfaces.IRepairable;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.tool.ItemBlowtorch;
import com.hbm.util.GameTime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class RepairLookOverlay {
    private RepairLookOverlay() {}

    public static void build(Level level, BlockPos core, LookInfo info) {
        if (!(Minecraft.getInstance().player.getMainHandItem().getItem() instanceof ItemBlowtorch))
            return;
        if (!(level.getBlockEntity(core) instanceof IRepairable repairable)
                || !repairable.isDamaged()) return;
        info.title(level.getBlockState(core).getBlock().getName().getString(), 0xFFFF00, 0x404000);
        info.line(Component.translatable("desc.repair.with").getString(), 0xFFAA00);
        for (CountIngredient ingredient : repairable.getRepairMaterials()) {
            ItemStack display = ingredient.extractForCyclingDisplay(GameTime.millis(level), 20);
            if (display.isEmpty()) continue;
            info.line(
                    Component.translatable(
                                    "desc.repair.material",
                                    display.getHoverName(),
                                    display.getCount())
                            .getString());
        }
    }
}
