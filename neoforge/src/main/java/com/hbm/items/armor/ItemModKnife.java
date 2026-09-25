// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.advancement.HbmCriteria;
import com.hbm.handler.ArmorModHandler;
import com.hbm.lib.Library;
import com.hbm.packet.toclient.ProperJoltPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class ItemModKnife extends ItemArmorMod {

    public static final Identifier TRIGAMMA_MODIFIER_ID = Library.id("trigamma_health");

    public ItemModKnife(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, false, true, false, false);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.modKnife.pain").withStyle(ChatFormatting.RED));
        adder.accept(Component.empty());
        adder.accept(
                Component.translatable("desc.item.modKnife.hurtsDoesntIt")
                        .withStyle(ChatFormatting.RED));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.literal("  ").append(stack.getHoverName()).withStyle(ChatFormatting.RED));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level().isClientSide()
                || entity.tickCount % 50 != 0
                || entity.getMaxHealth() <= 2F) return;

        entity.level()
                .playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        ModSounds.SLICER.get(),
                        entity.getSoundSource(),
                        1.0F,
                        1.0F);

        AttributeInstance maxHealth = entity.getAttribute(Attributes.MAX_HEALTH);
        float health = entity.getMaxHealth();
        maxHealth.removeModifier(TRIGAMMA_MODIFIER_ID);
        maxHealth.addPermanentModifier(
                new AttributeModifier(
                        TRIGAMMA_MODIFIER_ID,
                        -(entity.getMaxHealth() - health + 2),
                        AttributeModifier.Operation.ADD_VALUE));

        if (entity instanceof ServerPlayer player) {

            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
                player.connection.send(
                        new ClientboundSetHealthPacket(
                                player.getHealth(),
                                player.getFoodData().getFoodLevel(),
                                player.getFoodData().getSaturationLevel()));
            }
            if (player.getMaxHealth() > 2F) {
                Services.NETWORK.sendTo(
                        new ProperJoltPayload(10000 + player.getRandom().nextInt(10000), 10000),
                        player);
            } else {
                HbmCriteria.bledOut(player);
                Services.NETWORK.sendTo(new ProperJoltPayload(0, 0), player);
            }
        }
    }
}
