// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import com.hbm.data.MachineData;
import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.items.ItemPistons;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

public class ItemBlockDiesel extends BlockItem {

    public ItemBlockDiesel(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, display, adder, flag);
        adder.accept(ItemPistons.efficiencyHeader());
        List<Double> efficiency = MachineData.DIESEL_EFFICIENCY.get();

        for (FuelGrade grade : FuelGrade.VALUES) {
            double eff = efficiency.get(grade.ordinal());
            if (eff > 0) adder.accept(ItemPistons.efficiencyLine(grade, eff));
        }
    }
}
