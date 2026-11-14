/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.platform;

import com.hbm.capability.FluidContainerRows;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ExtractionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FabricFluidContainerRows {

    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private FabricFluidContainerRows() {}

    public static void register() {
        for (Item item : FluidContainerRows.fullItems()) {
            FluidStorage.combinedItemApiProvider(item)
                    .register(
                            context -> {
                                FluidContainerRows.Row row =
                                        FluidContainerRows.full(context.getItemVariant().toStack());
                                return row == null ? null : new Full(context, row);
                            });
        }
        for (Item item : FluidContainerRows.emptyItems()) {
            FluidStorage.combinedItemApiProvider(item).register(Empty::new);
        }
    }

    private static final class Full
            implements ExtractionOnlyStorage<FluidVariant>, SingleSlotStorage<FluidVariant> {

        private final ContainerItemContext context;
        private final FluidContainerRows.Row row;

        private Full(ContainerItemContext context, FluidContainerRows.Row row) {
            this.context = context;
            this.row = row;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            long amount = getAmount();
            if (amount == 0 || !resource.equals(getResource()) || maxAmount < amount) return 0;
            ItemStack empty = row.emptyStack();
            long converted =
                    empty.isEmpty()
                            ? context.extract(context.getItemVariant(), 1, transaction)
                            : context.exchange(ItemVariant.of(empty), 1, transaction);
            return converted == 1 ? amount : 0;
        }

        private boolean holds() {
            return row.isFull(context.getItemVariant().toStack());
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public FluidVariant getResource() {
            return holds() ? FluidVariant.of(row.fluid().get()) : FluidVariant.blank();
        }

        @Override
        public long getAmount() {
            return holds() ? row.amount().getAsInt() * DROPLETS_PER_MB : 0;
        }

        @Override
        public long getCapacity() {
            return getAmount();
        }
    }

    private static final class Empty implements InsertionOnlyStorage<FluidVariant> {

        private final ContainerItemContext context;
        private final Item item;

        private Empty(ContainerItemContext context) {
            this.context = context;
            this.item = context.getItemVariant().getItem();
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            StoragePreconditions.notBlankNotNegative(resource, maxAmount);
            if (!context.getItemVariant().isOf(item)
                    || !resource.equals(FluidVariant.of(resource.getFluid()))) {
                return 0;
            }
            ItemStack stack = context.getItemVariant().toStack();
            long amount =
                    FluidContainerRows.fillAmount(stack, resource.getFluid()) * DROPLETS_PER_MB;
            if (amount == 0 || maxAmount < amount) return 0;
            ItemVariant filled =
                    ItemVariant.of(FluidContainerRows.filled(stack, resource.getFluid()));
            return context.exchange(filled, 1, transaction) == 1 ? amount : 0;
        }
    }
}
