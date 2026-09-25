// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.special.ItemCustomLore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ItemFluidCell extends ItemCustomLore implements IFluidContainerItem {

    public static final int CAPACITY = 1000;

    private static final List<ItemFluidCell> FILLED = new ArrayList<>();

    private final @Nullable Supplier<Fluid> fluid;
    private final @Nullable Supplier<? extends Item> emptyCell;

    public ItemFluidCell(Properties properties) {
        super(properties);
        this.fluid = null;
        this.emptyCell = null;
    }

    public ItemFluidCell(
            Properties properties, Supplier<Fluid> fluid, Supplier<? extends Item> emptyCell) {
        super(properties);
        this.fluid = fluid;
        this.emptyCell = emptyCell;
        FILLED.add(this);
    }

    private @Nullable ItemFluidCell filledFor(Fluid type) {
        for (ItemFluidCell cell : FILLED) {
            if (cell.emptyCell.get() == this && cell.fluid.get() == type) return cell;
        }
        return null;
    }

    @Override
    public int capacity(ItemStack stack) {
        return CAPACITY;
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        return fluid == null ? EMPTY_CONTENT : new FluidStackNTM(fluid.get(), CAPACITY);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        throw new UnsupportedOperationException(
                "Fixed cell item ids must transform through filledContainer/emptyContainer");
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        return fluid == null && pressure == 0 && amount >= CAPACITY && filledFor(type) != null
                ? CAPACITY
                : 0;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        return fluid != null && fluid.get() == type && pressure == 0 && amount >= CAPACITY
                ? CAPACITY
                : 0;
    }

    @Override
    public boolean canStore(Fluid type) {
        return fluid == null ? filledFor(type) != null : fluid.get() == type;
    }

    @Override
    public ItemStack emptyContainer(ItemStack stack) {

        return emptyCell == null ? stack.copyWithCount(1) : new ItemStack(emptyCell.get());
    }

    @Override
    public ItemStack filledContainer(ItemStack stack, FluidStackNTM content) {
        if (content.amount() != CAPACITY || content.pressure() != 0) {
            throw new IllegalArgumentException(
                    "cells only convert to 1000mB at zero pressure, got " + content);
        }
        if (fluid != null) {

            if (content.type() != fluid.get()) {
                throw new IllegalArgumentException(
                        this + " holds " + fluid.get() + ", not " + content.type());
            }
            return stack.copyWithCount(1);
        }
        ItemFluidCell target = filledFor(content.type());
        if (target == null) {
            throw new IllegalArgumentException("no cell exists for " + content.type());
        }
        return new ItemStack(target);
    }
}
