// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.data.ItemData;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidCell;
import com.hbm.platform.Services;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class ItemAntiSchrabidiumCell extends ItemFluidCell implements IItemEntityUpdate {

    public ItemAntiSchrabidiumCell(Properties properties) {
        super(properties, () -> NTMFluids.ASCHRAB, ModItems.CELL_EMPTY);
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!entity.onGround()) return false;

        if (ItemData.DROP_ANTIMATTER_CELL.get()) {
            Level level = entity.level();
            EntityNukeExplosionMK3 blast =
                    EntityNukeExplosionMK3.statFacFleija(
                            level,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            ExplosionData.A_SCHRAB_RADIUS.get());

            if (!blast.isRemoved()) {
                level.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.BLOCKS,
                        100.0F,
                        level.getRandom().nextFloat() * 0.1F + 0.9F);
                level.addFreshEntity(blast);
                level.addFreshEntity(
                        EntityCloudFleija.statFac(
                                level,
                                ExplosionData.A_SCHRAB_RADIUS.get(),
                                entity.getX(),
                                entity.getY(),
                                entity.getZ()));
            }
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
        adder.accept(Component.translatable("desc.shared.warningExposureToMatter"));
        adder.accept(Component.translatable("desc.item.antiSchrabidiumCell.createAFLkvangr"));
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
