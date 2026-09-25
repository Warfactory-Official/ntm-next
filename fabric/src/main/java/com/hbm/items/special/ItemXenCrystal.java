// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.data.ItemData;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.platform.Services;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemXenCrystal extends Item implements IItemEntityUpdate {

    private static final int RADIUS = 25, LIFT = 75;

    public ItemXenCrystal(Properties properties) {
        super(properties);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;

        if (ItemData.DROP_XEN_CRYSTAL.get()) {

            BlockPos at =
                    new BlockPos((int) entity.getX(), (int) entity.getY(), (int) entity.getZ());
            ExplosionChaos.floater(entity.level(), at, RADIUS, LIFT);
            ExplosionChaos.move(entity.level(), at, RADIUS, 0, LIFT, 0);
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
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
