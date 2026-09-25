// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.food;

import com.hbm.entity.effect.EntityVortex;
import com.hbm.items.ModItems;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemConserve extends Item {

    public final EnumFoodType type;

    public ItemConserve(Properties properties, EnumFoodType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.canEat(false)) return InteractionResult.PASS;
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (!(entity instanceof ServerPlayer player) || !(level instanceof ServerLevel server))
            return rest;

        player.getFoodData().eat(type.foodLevel, type.saturation);
        server.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.PLAYER_BURP,
                SoundSource.PLAYERS,
                0.5F,
                server.getRandom().nextFloat() * 0.1F + 0.9F);
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.CAN_KEY));

        if (type == EnumFoodType.BHOLE) {
            EntityVortex vortex = new EntityVortex(server, 0.5F);
            vortex.setShrinkRate(0.01F).noBreak();
            vortex.setPos(player.getX(), player.getY(), player.getZ());
            server.addFreshEntity(vortex);
        } else if (type == EnumFoodType.RECURSION && server.getRandom().nextInt(10) > 0) {
            player.getInventory()
                    .placeItemBackInInventory(
                            ModItems.CANNED_CONSERVE.stack(EnumFoodType.RECURSION));
        } else if (type == EnumFoodType.FIST) {
            player.hurtServer(server, player.damageSources().magic(), 2F);
        }

        return rest;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line : I18nUtil.loreLines(getDescriptionId() + ".desc")) {
            adder.accept(Component.literal(line));
        }
    }

    public enum EnumFoodType {
        BEEF(8, 0.75F),
        TUNA(4, 0.75F),
        MYSTERY(6, 0.5F),
        PASHTET(4, 0.5F),
        CHEESE(3, 1F),
        SLIME(15, 5F),
        MILK(5, 0.25F),
        ASS(6, 0.75F),

        PIZZA(8, 75F),
        TUBE(2, 0.25F),
        TOMATO(4, 0.5F),
        ASBESTOS(7, 1F),
        BHOLE(10, 1F),
        HOTDOGS(5, 0.75F),
        LEFTOVERS(1, 0.1F),
        YOGURT(3, 0.5F),
        STEW(5, 0.5F),
        CHINESE(6, 0.1F),
        OIL(3, 1F),
        FIST(6, 0.75F),
        SPAM(8, 1F),
        FRIED(10, 0.75F),
        NAPALM(6, 1F),
        DIESEL(6, 1F),
        KEROSENE(6, 1F),
        RECURSION(1, 1F),
        BARK(2, 1F);

        public final int foodLevel;
        public final float saturation;

        EnumFoodType(int foodLevel, float saturation) {
            this.foodLevel = foodLevel;
            this.saturation = saturation;
        }
    }
}
