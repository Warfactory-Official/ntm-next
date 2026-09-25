// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.data.ItemData;
import com.hbm.entity.effect.EntityQuasar;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.platform.Services;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.I18nUtil;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

public class ItemDigamma extends Item implements IItemEntityUpdate {

    private final int digamma;

    public ItemDigamma(Properties properties, int digamma) {
        super(properties);
        this.digamma = digamma;
    }

    private static final float QUASAR_SIZE = 5F;

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround() || !(entity.level() instanceof ServerLevel level)) return false;

        if (ItemData.DROP_SINGULARITY.get()) {
            EntityQuasar quasar = new EntityQuasar(level, QUASAR_SIZE);
            quasar.setPos(entity.getX(), entity.getY(), entity.getZ());
            level.addFreshEntity(quasar);
        }
        entity.discard();
        return true;
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.applyDigammaData(living, 1.0 / digamma);
        }
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        adder.accept(
                Component.literal(I18nUtil.resolveKey("trait.hlParticle", "1.67*10³⁴ a"))
                        .withStyle(ChatFormatting.GOLD));
        adder.accept(
                Component.literal(I18nUtil.resolveKey("trait.hlPlayer", (digamma / 20D) + "s"))
                        .withStyle(ChatFormatting.RED));
        adder.accept(Component.empty());

        double mdrxPerSecond = ((int) ((1000D / digamma) * 200D)) / 10D;
        adder.accept(
                Component.literal("[" + I18nUtil.resolveKey("trait.digamma") + "]")
                        .withStyle(ChatFormatting.RED));
        adder.accept(
                Component.literal(mdrxPerSecond + "mDRX/s").withStyle(ChatFormatting.DARK_RED));

        adder.accept(
                Component.literal("[" + I18nUtil.resolveKey("trait.drop") + "]")
                        .withStyle(ChatFormatting.RED));
    }
}
