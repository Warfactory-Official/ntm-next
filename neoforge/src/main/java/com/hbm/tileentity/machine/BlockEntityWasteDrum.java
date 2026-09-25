// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuWasteDrum;
import com.hbm.inventory.recipes.FuelPoolRecipe;
import com.hbm.inventory.recipes.FuelPoolRecipes;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.BlockEntityMachineBase;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockEntityWasteDrum extends BlockEntityMachineBase implements MenuProvider {

    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    public BlockEntityWasteDrum(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASTE_DRUM.get(), pos, state, 12);
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return FuelPoolRecipes.getRecipe(stack) != null || stack.getItem() instanceof ItemRBMKRod;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (stack.getItem() instanceof ItemRBMKRod) {
            return ItemRBMKRod.getCoreHeat(stack) < 50 && ItemRBMKRod.getHullHeat(stack) < 50;
        }
        return FuelPoolRecipes.getRecipe(stack) == null;
    }

    @Override
    public void tickServer() {
        if (!waterKnown) refreshWaterFaces();
        int water = waterFaces;
        if (water == 0) return;

        int r = 60 * SharedConstants.TICKS_PER_MINUTE / water;
        for (int i = 0; i < 12; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof ItemRBMKRod rod) {
                rod.updateHeat(level, stack, 0.025D);
                rod.provideHeat(level, stack, 20D, 0.025D);
            } else if (level.getRandom().nextInt(r) == 0) {
                FuelPoolRecipe recipe = FuelPoolRecipes.getRecipe(stack);
                if (recipe != null) {
                    inventory.set(i, recipe.output().copy());
                    markChanged();
                }
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.wasteDrum");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuWasteDrum(id, inv, this);
    }

    private int waterFaces;
    private boolean waterKnown;

    public void refreshWaterFaces() {
        int water = 0;
        boolean complete = true;
        for (Direction dir : Direction.VALUES) {
            BlockPos pos = worldPosition.relative(dir);

            if (!level.isLoaded(pos)) {
                complete = false;
                continue;
            }
            if (level.getBlockState(pos).is(Blocks.WATER)) water++;
        }
        waterFaces = water;
        waterKnown = complete;
    }
}
