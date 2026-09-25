// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.data.ItemData;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidCell;
import com.hbm.platform.Services;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemAntimatterCell extends ItemFluidCell implements IItemEntityUpdate {

    public ItemAntimatterCell(Properties properties) {
        super(properties, () -> NTMFluids.AMAT, ModItems.CELL_EMPTY);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;

        if (ItemData.DROP_ANTIMATTER_CELL.get()) {
            new ExplosionVNT(entity.level(), entity.getX(), entity.getY(), entity.getZ(), 3F)
                    .makeAmat()
                    .explode();
        }
        entity.discard();
        return true;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(Component.translatable("desc.shared.warningExposureToMatter"));
        adder.accept(Component.translatable("desc.item.antimatterCell.leadToViolentAnnihilation"));
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
