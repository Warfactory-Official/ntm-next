// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.capability.ForeignFluidItemAccess;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

final class FabricFluidItemStorage extends SingleVariantItemStorage<FluidVariant> {

    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private final ContainerItemContext context;

    private FabricFluidItemStorage(ContainerItemContext context) {
        super(context);
        this.context = context;
    }

    static void register() {
        List<Item> items = ForeignFluidItemAccess.containerItems();
        if (items.isEmpty()) return;
        FluidStorage.ITEM.registerForItems(
                (stack, context) -> new FabricFluidItemStorage(context),
                items.toArray(new Item[0]));
    }

    private static long floor(long droplets) {
        return droplets < 0 ? droplets : droplets / DROPLETS_PER_MB * DROPLETS_PER_MB;
    }

    @Override
    protected FluidVariant getBlankResource() {
        return FluidVariant.blank();
    }

    @Override
    protected FluidVariant getResource(ItemVariant currentVariant) {
        Fluid held = ForeignFluidItemAccess.heldFluid(currentVariant.toStack());
        return held == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(held);
    }

    @Override
    protected long getAmount(ItemVariant currentVariant) {
        return ForeignFluidItemAccess.heldMb(currentVariant.toStack()) * DROPLETS_PER_MB;
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        ItemStack stack = context.getItemVariant().toStack();

        Fluid fluid =
                variant.isBlank() ? ForeignFluidItemAccess.heldFluid(stack) : variant.getFluid();
        return ForeignFluidItemAccess.capacityMb(stack, fluid) * DROPLETS_PER_MB;
    }

    @Override
    protected ItemVariant getUpdatedVariant(
            ItemVariant currentVariant, FluidVariant newResource, long newAmount) {
        return ItemVariant.of(
                ForeignFluidItemAccess.withContent(
                        currentVariant.toStack(),
                        newResource.isBlank() ? Fluids.EMPTY : newResource.getFluid(),
                        newAmount / DROPLETS_PER_MB));
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank()) return 0L;
        ItemStack stack = context.getItemVariant().toStack();
        long accepted =
                ForeignFluidItemAccess.insertableMb(
                        stack, resource.getFluid(), floor(maxAmount) / DROPLETS_PER_MB);
        return accepted <= 0 ? 0L : super.insert(resource, accepted * DROPLETS_PER_MB, transaction);
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank()) return 0L;
        ItemStack stack = context.getItemVariant().toStack();
        long given =
                ForeignFluidItemAccess.extractableMb(
                        stack, resource.getFluid(), floor(maxAmount) / DROPLETS_PER_MB);
        return given <= 0 ? 0L : super.extract(resource, given * DROPLETS_PER_MB, transaction);
    }
}
