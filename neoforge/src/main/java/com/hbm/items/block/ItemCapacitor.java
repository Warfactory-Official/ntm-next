// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.block;

import com.hbm.blocks.machine.storage.MachineCapacitor;
import com.hbm.client.ModifierKeys;
import com.hbm.items.PersistentInfoBlockItem;
import com.hbm.util.BobMathUtil;
import com.hbm.util.TooltipStyle;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;

public final class ItemCapacitor extends PersistentInfoBlockItem {

    public ItemCapacitor(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        long ceiling = ((MachineCapacitor) getBlock()).maxPower;
        adder.accept(
                Component.translatable(
                                "desc.shared.storesUpTo", BobMathUtil.getShortNumber(ceiling))
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.translatable(
                                "desc.shared.chargeSpeed",
                                BobMathUtil.getShortNumber(ceiling / 200))
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.translatable(
                                "desc.shared.dischargeSpeed",
                                BobMathUtil.getShortNumber(ceiling / 600))
                        .withStyle(ChatFormatting.GOLD));
        if ((FMLLoader.getCurrent().getDist() == Dist.CLIENT) && ModifierKeys.detailsHeld()) {
            adder.accept(
                    Component.translatable("desc.hbm.capacitor").withStyle(ChatFormatting.YELLOW));
        } else {
            adder.accept(
                    TooltipStyle.holdDetails()
                            .copy()
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        super.appendHoverText(stack, context, display, adder, flag);
    }
}
