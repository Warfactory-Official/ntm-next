// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin;

import com.hbm.api.environment.Environment;
import com.hbm.items.ModItems;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractFurnaceBlockEntity.class, priority = 500)
public abstract class MixinAbstractFurnaceBlockEntity {
    @WrapOperation(
            method = "consumeFuel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/Item;getCraftingRemainder()Lnet/minecraft/world/item/ItemStackTemplate;"))
    private static ItemStackTemplate hbm$eternalCoalFuelRemainder(
            Item fuel, Operation<ItemStackTemplate> original) {
        return fuel == ModItems.COAL_ETERNAL.get()
                ? new ItemStackTemplate(fuel)
                : original.call(fuel);
    }

    @WrapOperation(
            method = "serverTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration("
                                            + "Lnet/minecraft/world/level/block/entity/FuelValues;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int hbm$burnNeedsAir(
            AbstractFurnaceBlockEntity entity,
            FuelValues fuelValues,
            ItemStack fuel,
            Operation<Integer> original,
            ServerLevel level,
            BlockPos pos) {
        int burnTime = original.call(entity, fuelValues, fuel);
        return burnTime > 0 && !fuel.is(Items.LAVA_BUCKET) && !Environment.breatheAir(level, pos, 0)
                ? 0
                : burnTime;
    }
}
