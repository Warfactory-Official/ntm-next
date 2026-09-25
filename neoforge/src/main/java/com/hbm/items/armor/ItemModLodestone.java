// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModHandler;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ItemModLodestone extends ItemArmorMod {

    private static final double PULL = 0.75D;
    private static final double LIFT_BELOW = 0.04D;
    private static final double LIFT = 0.2D;

    public final int range;

    public ItemModLodestone(Properties properties, int range) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
        this.range = range;
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.lodestone")
                        .withStyle(ChatFormatting.DARK_GRAY));
        adder.accept(
                Component.translatable("desc.item.armorMod.lodestone.range", range)
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable(
                                "desc.item.armorMod.lodestone.installed",
                                stack.getHoverName(),
                                range)
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (entity.level() instanceof ServerLevel level) pull(entity, level);
    }

    private void pull(LivingEntity entity, ServerLevel level) {
        if (entity instanceof Player player && !HbmPlayerProps.getData(player).isMagnetActive())
            return;

        for (ItemEntity item :
                level.getEntitiesOfClass(
                        ItemEntity.class, entity.getBoundingBox().inflate(range, range, range))) {
            Vec3 towards =
                    new Vec3(
                                    entity.getX() - item.getX(),
                                    entity.getY() - item.getY(),
                                    entity.getZ() - item.getZ())
                            .normalize();
            Vec3 motion = item.getDeltaMovement().add(towards.scale(PULL));
            if (towards.y > 0 && motion.y < LIFT_BELOW) motion = motion.add(0D, LIFT, 0D);
            item.setDeltaMovement(motion);

            level.getChunkSource()
                    .sendToTrackingPlayers(item, new ClientboundSetEntityMotionPacket(item));
        }
    }
}
