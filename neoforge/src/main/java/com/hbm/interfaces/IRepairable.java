// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.util.InventoryUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IRepairable {

    static boolean tryRepairMultiblock(Level level, BlockPos core, Player player) {
        return repairAt(level, level.getBlockEntity(core), player);
    }

    private static boolean repairAt(Level level, BlockEntity core, Player player) {
        if (!(core instanceof IRepairable repairable) || !repairable.isDamaged()) return false;

        List<CountIngredient> list = repairable.getRepairMaterials();
        if (list == null || list.isEmpty()) {
            if (!level.isClientSide()) repairable.repair(player);
            return true;
        }
        if (level.isClientSide()) return InventoryUtil.hasIngredients(player, list);
        if (!InventoryUtil.consumeIngredients(player, list)) return false;
        repairable.repair(player);
        return true;
    }

    boolean isDamaged();

    List<CountIngredient> getRepairMaterials();

    void repair(Player player);

    void tryExtinguish(Level level, BlockPos pos, EnumExtinguishType type);

    enum EnumExtinguishType {
        WATER,
        FOAM,
        SAND,
        CO2
    }
}
