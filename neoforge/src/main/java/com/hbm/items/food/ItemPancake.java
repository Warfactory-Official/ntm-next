// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorSuitEffects;
import com.hbm.items.armor.ModArmorItem;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemPancake extends Item {

    public static final FoodProperties FOOD =
            new FoodProperties.Builder()
                    .nutrition(20)
                    .saturationModifier(20F)
                    .alwaysEdible()
                    .build();

    public ItemPancake(Properties props) {
        super(props);
    }

    private static boolean canChewIt(Player player) {
        return ArmorSuitEffects.hasFullSet(player, ModArmorItem.Suit.BJ, false)
                && player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.BJ_HELMET.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (canChewIt(player)) return super.use(level, player, hand);
        if (!level.isClientSide()) {
            player.sendSystemMessage(
                    Component.translatable("desc.item.pancake.yourTeethAreToo")
                            .withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack worn = entity.getItemBySlot(slot);
            if (worn.getItem() instanceof IBatteryItem battery) {
                battery.setCharge(worn, battery.getMaxCharge(worn));
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag tooltipFlag) {
        adder.accept(Component.translatable("desc.item.pancake.canBeEatenTo"));
        adder.accept(Component.translatable("desc.item.pancake.notForPeopleWith"));
        adder.accept(Component.empty());
        adder.accept(Component.translatable("desc.item.pancake.halfBurntAndSmells"));
    }
}
