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

package com.hbm.blockentity.machine;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.machine.reactor.ResearchReactorBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.inventory.container.ReactorBreedingMenu;
import com.hbm.inventory.recipes.BreederRecipe;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ReactorBreedingBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_COUNT = 2;

    public static final float PROGRESS_PER_TICK = 0.0025F;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_INPUT, SLOT_OUTPUT};

    public int flux;
    public float progress;

    public ReactorBreedingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_REACTOR_BREEDING.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        hbm$syncSet$ReactorBreedingBlockEntity$flux(0);
        gatherFlux();

        BreederRecipe recipe = BreederRecipes.getOutput(inventory.get(SLOT_INPUT));

        if (canProcess(recipe)) {
            hbm$syncSet$ReactorBreedingBlockEntity$progress(
                    (float) (progress + (PROGRESS_PER_TICK * (flux / recipe.flux))));
            markChanged();

            if (progress >= 1.0F) {
                hbm$syncSet$ReactorBreedingBlockEntity$progress(0F);
                processItem(recipe);
                setChanged();
            }
        } else {
            if (progress != 0.0F) markChanged();
            hbm$syncSet$ReactorBreedingBlockEntity$progress(0.0F);
        }

        networkPackNT(20);
    }

    private void gatherFlux() {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = worldPosition.relative(dir);
            if (!level.isLoaded(side)) continue;

            BlockPos reactorCore = null;
            if (level.getBlockState(side).getBlock() == ModBlocks.REACTOR_RESEARCH.get()) {
                reactorCore = side;
            } else {
                MultiblockCoreBlock.FoldedOwner owner = MultiblockCoreBlock.ownerOf(level, side);
                if (owner != null && owner.block() == ModBlocks.REACTOR_RESEARCH.get())
                    reactorCore = owner.pos();
            }

            if (reactorCore == null) continue;
            if (level.getBlockEntity(reactorCore) instanceof ResearchReactorBlockEntity reactor) {
                hbm$syncSet$ReactorBreedingBlockEntity$flux((int) (flux + (reactor.totalFlux)));
            }
        }
    }

    public boolean canProcess(BreederRecipe recipe) {
        if (inventory.get(SLOT_INPUT).isEmpty()) return false;
        if (recipe == null) return false;
        if (flux < recipe.flux) return false;

        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItem(out, recipe.output())) return false;
        return out.getCount() < out.getMaxStackSize();
    }

    private void processItem(BreederRecipe recipe) {
        ItemStack result = recipe.output();
        ItemStack out = inventory.get(SLOT_OUTPUT);

        if (out.isEmpty()) {
            inventory.set(SLOT_OUTPUT, result.copy());
        } else if (ItemStack.isSameItem(out, result)) {
            out.grow(result.getCount());
        }

        ItemStack in = inventory.get(SLOT_INPUT);
        in.shrink(1);
        if (in.isEmpty()) inventory.set(SLOT_INPUT, ItemStack.EMPTY);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_INPUT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.reactorBreeding");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new ReactorBreedingMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ReactorBreedingBlockEntity$progress(input.getFloatOr("progress", 0F));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putFloat("progress", progress);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.flux);
            case 1 -> output.writeFloat(this.progress);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$ReactorBreedingBlockEntity$flux(input.readInt());
            case 1 -> this.hbm$syncSet$ReactorBreedingBlockEntity$progress(input.readFloat());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$ReactorBreedingBlockEntity$flux(int next) {
        if (this.flux != next) {
            this.flux = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }

    public final void hbm$syncSet$ReactorBreedingBlockEntity$progress(float next) {
        if (Float.floatToRawIntBits(this.progress) != Float.floatToRawIntBits(next)) {
            this.progress = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    static {
        com.hbm.inventory.container.SyncedData.fields(ReactorBreedingBlockEntity.class, "flux");
    }
}
