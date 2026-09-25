// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.items.machine.IFluidContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public final class ItemFluidHandler extends ItemAccessResourceHandler<FluidResource> {

    public ItemFluidHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        return FluidResource.of(ForeignFluidItemAccess.heldFluid(accessResource.toStack()));
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        return (int) ForeignFluidItemAccess.heldMb(accessResource.toStack());
    }

    @Override
    protected @Nullable ItemResource update(
            ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        ItemStack updated =
                ForeignFluidItemAccess.withContent(
                        accessResource.toStack(),
                        newAmount <= 0 ? Fluids.EMPTY : newResource.getFluid(),
                        newAmount);
        return ItemResource.of(updated);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return getCapacity(index, resource) > 0;
    }

    @Override
    public int insert(
            int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty()) return 0;
        long accepted =
                ForeignFluidItemAccess.insertableMb(
                        itemAccess.getResource().toStack(), resource.getFluid(), amount);
        return accepted <= 0 ? 0 : super.insert(index, resource, (int) accepted, transaction);
    }

    @Override
    public int extract(
            int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (resource.isEmpty()) return 0;
        long given =
                ForeignFluidItemAccess.extractableMb(
                        itemAccess.getResource().toStack(), resource.getFluid(), amount);
        return given <= 0 ? 0 : super.extract(index, resource, (int) given, transaction);
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        ItemStack stack = itemAccess.getResource().toStack();

        Fluid fluid =
                resource.isEmpty() ? ForeignFluidItemAccess.heldFluid(stack) : resource.getFluid();
        return (int) ForeignFluidItemAccess.capacityMb(stack, fluid);
    }
}
