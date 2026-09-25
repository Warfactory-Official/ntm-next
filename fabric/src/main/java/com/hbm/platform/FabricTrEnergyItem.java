// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.api.energymk2.EnergyConversion;
import com.hbm.capability.ForeignEnergyItemAccess;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

final class FabricTrEnergyItem implements EnergyStorage {

    private final ContainerItemContext context;

    private FabricTrEnergyItem(ContainerItemContext context) {
        this.context = context;
    }

    static void register() {
        List<Item> batteries = ForeignEnergyItemAccess.batteryItems();
        if (batteries.isEmpty()) return;
        EnergyStorage.ITEM.registerForItems(
                (stack, context) -> new FabricTrEnergyItem(context),
                batteries.toArray(new Item[0]));
    }

    private ItemStack stack() {
        return context.getItemVariant().toStack();
    }

    private boolean exchange(ItemStack updated, TransactionContext transaction) {
        return context.exchange(ItemVariant.of(updated), 1, transaction) == 1;
    }

    @Override
    public boolean supportsInsertion() {
        return !ForeignEnergyItemAccess.isCreative(stack());
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) return 0;
        ItemStack held = stack();
        long quantum = EnergyConversion.feQuantum();
        long offered = EnergyConversion.heFromFe(maxAmount);
        long accepted = ForeignEnergyItemAccess.insertableHe(held, offered);

        long fe = EnergyConversion.feFromHe(accepted) / quantum * quantum;
        if (fe <= 0) return 0;
        long he = EnergyConversion.heFromFe(fe);
        return exchange(ForeignEnergyItemAccess.withCharge(held, he), transaction) ? fe : 0;
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0) return 0;
        ItemStack held = stack();
        long quantum = EnergyConversion.feQuantum();
        long offered = EnergyConversion.heFromFe(maxAmount);
        long given = ForeignEnergyItemAccess.extractableHe(held, offered);
        long fe = EnergyConversion.feFromHe(given) / quantum * quantum;
        if (fe <= 0) return 0;

        if (ForeignEnergyItemAccess.isCreative(held)) return fe;
        long he = EnergyConversion.heFromFe(fe);
        return exchange(ForeignEnergyItemAccess.withCharge(held, -he), transaction) ? fe : 0;
    }

    @Override
    public long getAmount() {
        return EnergyConversion.feFromHe(ForeignEnergyItemAccess.chargeHe(stack()));
    }

    @Override
    public long getCapacity() {
        return EnergyConversion.feFromHe(ForeignEnergyItemAccess.capacityHe(stack()));
    }
}
