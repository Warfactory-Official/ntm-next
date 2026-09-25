// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.ItemAccessResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

public final class FluidContainerRowHandler extends ItemAccessResourceHandler<FluidResource> {

    private FluidContainerRowHandler(ItemAccess itemAccess) {
        super(itemAccess, 1);
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(
                Capabilities.Fluid.ITEM,
                (stack, access) ->
                        FluidContainerRows.answers(stack)
                                ? new FluidContainerRowHandler(access)
                                : null,
                FluidContainerRows.items().toArray(new Item[0]));
    }

    @Override
    protected FluidResource getResourceFrom(ItemResource accessResource, int index) {
        FluidContainerRows.Row row = FluidContainerRows.full(accessResource.toStack());
        return row == null ? FluidResource.EMPTY : FluidResource.of(row.fluid().get());
    }

    @Override
    protected int getAmountFrom(ItemResource accessResource, int index) {
        FluidContainerRows.Row row = FluidContainerRows.full(accessResource.toStack());
        return row == null ? 0 : row.amount().getAsInt();
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return getCapacity(index, resource) > 0;
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        ItemStack stack = itemAccess.getResource().toStack();
        FluidContainerRows.Row row = FluidContainerRows.full(stack);
        if (row != null) {
            return resource.isEmpty() || resource.getFluid() == row.fluid().get()
                    ? row.amount().getAsInt()
                    : 0;
        }
        return resource.isEmpty() ? 0 : FluidContainerRows.fillAmount(stack, resource.getFluid());
    }

    @Override
    protected @Nullable ItemResource update(
            ItemResource accessResource, int index, FluidResource newResource, int newAmount) {
        ItemStack stack = accessResource.toStack();
        FluidContainerRows.Row row = FluidContainerRows.full(stack);
        if (row != null) {
            if (newAmount != 0) return ItemResource.EMPTY;
            ItemStack empty = row.emptyStack();
            return empty.isEmpty() ? null : ItemResource.of(empty);
        }
        int amount = FluidContainerRows.fillAmount(stack, newResource.getFluid());
        return amount > 0 && newAmount == amount
                ? ItemResource.of(FluidContainerRows.filled(stack, newResource.getFluid()))
                : ItemResource.EMPTY;
    }
}
