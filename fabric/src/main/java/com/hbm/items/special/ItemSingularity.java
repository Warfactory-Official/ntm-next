// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.data.ItemData;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.platform.Services;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemSingularity extends ItemCustomLore implements IItemEntityUpdate {

    private final BiFunction<Level, Float, Entity> factory;
    private final float size;

    public ItemSingularity(
            BiFunction<Level, Float, Entity> factory, float size, Properties properties) {
        super(properties);
        this.factory = factory;
        this.size = size;
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;

        if (ItemData.DROP_SINGULARITY.get()) {
            Entity well = factory.apply(entity.level(), size);
            well.setPos(entity.getX(), entity.getY(), entity.getZ());
            entity.level().addFreshEntity(well);
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
        super.appendHoverText(stack, context, display, adder, flag);
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
