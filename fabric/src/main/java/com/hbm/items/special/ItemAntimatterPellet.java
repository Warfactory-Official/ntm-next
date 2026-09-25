// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.data.ItemData;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.platform.Services;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemAntimatterPellet extends Item implements IItemEntityUpdate {

    private static final float BLAST = 20F;

    public ItemAntimatterPellet(Properties properties) {
        super(properties);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;

        if (ItemData.DROP_ANTIMATTER_CELL.get()) {
            new ExplosionVNT(entity.level(), entity.getX(), entity.getY(), entity.getZ(), BLAST)
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
        adder.accept(Component.translatable("desc.item.antimatterPellet.veryHeavy"));
        adder.accept(Component.translatable("desc.item.antimatterPellet.getsRidOf"));
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
