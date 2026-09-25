// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.tileentity.BlockEntityMachineBase;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityCranePartitioner extends BlockEntityMachineBase {

    public static final int SLOT_COUNT = 45;

    private static final Comparator<ItemStack> BY_STACK_SIZE =
            (a, b) -> (int) Math.signum(a.getCount() - b.getCount());

    private static final int[] ACCESS = accessAll();

    public BlockEntityCranePartitioner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_PARTITIONER.get(), pos, state, SLOT_COUNT * 2);
    }

    private static int[] accessAll() {
        int[] access = new int[SLOT_COUNT * 2];
        for (int i = 0; i < access.length; i++) access[i] = i;
        return access;
    }

    @Override
    public void tickServer() {
        List<ItemStack> queued = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) if (!getItem(i).isEmpty()) queued.add(getItem(i));
        queued.sort(BY_STACK_SIZE);

        boolean changed = false;

        for (ItemStack stack : queued) {
            int amount = CrystallizerRecipes.INSTANCE.getAmount(stack);

            if (amount == 0) amount = stack.getCount();

            while (stack.getCount() >= amount) {
                EntityMovingItem item = new EntityMovingItem(level);
                item.setItemStack(stack.copyWithCount(amount));
                item.snapTo(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.25,
                        worldPosition.getZ() + 0.5,
                        0F,
                        0F);
                level.addFreshEntity(item);

                stack.shrink(amount);
                changed = true;
            }
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            if (getItem(i).isEmpty()) inventory.set(i, ItemStack.EMPTY);
        }

        if (changed) setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < SLOT_COUNT && CrystallizerRecipes.INSTANCE.getAmount(stack) >= 1;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_COUNT;
    }
}
