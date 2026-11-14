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

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.GhostFilters;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.BlockMenus;
import com.hbm.inventory.container.AutocrafterMenu;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class TemplateCrafterBlockEntity extends MachineBlockEntity
        implements EnergyHandler, GhostFilters, MenuProvider, SyncUnitSchema {

    public static final int TEMPLATE_START = 0;
    public static final int TEMPLATE_COUNT = 9;
    public static final int PREVIEW_SLOT = 9;
    public static final int INGREDIENT_START = 10;
    public static final int OUTPUT_SLOT = 19;
    public static final int BATTERY_SLOT = 20;
    public static final int SLOT_COUNT = 21;
    public static final int GRID_WIDTH = 3;
    public static final int GRID_HEIGHT = 3;
    public static final int CRAFT_COST = 100;
    public static final long HE_CAPACITY = 10_000;
    public static final int INGREDIENT_LIMIT = 4;

    private static final Window FILTER_WINDOW = new Window(TEMPLATE_START, TEMPLATE_COUNT);
    private static final int[] AUTOMATED_SLOTS = {10, 11, 12, 13, 14, 15, 16, 17, 18, 19};

    private long energy;
    private final ModulePatternMatcher matcher = new ModulePatternMatcher(TEMPLATE_COUNT);
    private int candidateCount;
    private int selectedRecipeIndex;

    private final List<ItemStack> templateItems;
    private final List<ItemStack> ingredientItems;
    private final ArrayList<CraftingRecipe> candidates = new ArrayList<>();
    private boolean restoreCandidates;

    public TemplateCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTOCRAFTER.get(), pos, state, SLOT_COUNT);
        templateItems = inventory.subList(TEMPLATE_START, PREVIEW_SLOT);
        ingredientItems = inventory.subList(INGREDIENT_START, OUTPUT_SLOT);
    }

    @Override
    public long getPower() {
        return energy;
    }

    @Override
    public void setPower(long stored) {
        if (energy != stored) {
            hbm$syncSet$TemplateCrafterBlockEntity$energy(stored);
            setChanged();
        }
    }

    @Override
    public long getMaxPower() {
        return HE_CAPACITY;
    }

    public ModulePatternMatcher patternMatcher() {
        return matcher;
    }

    public int candidateCount() {
        return candidateCount;
    }

    public int selectedRecipeIndex() {
        return selectedRecipeIndex;
    }

    @Override
    public Window filterWindow() {
        return FILTER_WINDOW;
    }

    @Override
    public void putFilter(int slot, ItemStack stack) {
        if (!FILTER_WINDOW.contains(slot)) return;
        ItemStack template = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        inventory.set(slot, template);
        matcher.initPatternSmart(level, template, slot);
        rebuildTemplate();
        setChanged();
    }

    @Override
    public void advanceFilterMode(int slot) {
        matcher.nextMode(level, inventory.get(slot), slot);
        setChanged();
    }

    public void rebuildTemplate() {
        if (!(level instanceof ServerLevel server)) return;
        discoverCandidates(server, false);
        setChanged();
    }

    private void discoverCandidates(ServerLevel server, boolean keepSelection) {
        int previousSelection = selectedRecipeIndex;
        int requestedSelection = keepSelection ? previousSelection : 0;
        CraftingInput input = CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, templateItems);
        candidates.clear();
        if (!input.isEmpty()) {
            for (var holder : server.getServer().getRecipeManager().getRecipes()) {
                if (holder.value() instanceof CraftingRecipe recipe
                        && recipe.matches(input, server)) {
                    candidates.add(recipe);
                }
            }
        }
        hbm$syncSet$TemplateCrafterBlockEntity$candidateCount(candidates.size());
        hbm$syncSet$TemplateCrafterBlockEntity$selectedRecipeIndex(0);
        if (requestedSelection < candidateCount)
            hbm$syncSet$TemplateCrafterBlockEntity$selectedRecipeIndex(requestedSelection);
        updatePreview(input);
        restoreCandidates = false;
        if (selectedRecipeIndex != previousSelection) setChanged();
    }

    private void updatePreview(CraftingInput input) {
        ItemStack preview = ItemStack.EMPTY;
        if (!candidates.isEmpty())
            preview = candidates.get(selectedRecipeIndex).assemble(input).copy();
        inventory.set(PREVIEW_SLOT, preview);
    }

    public void cycleRecipe() {
        if (!(level instanceof ServerLevel server)) return;
        if (restoreCandidates) discoverCandidates(server, true);
        if (candidates.isEmpty()) {
            hbm$syncSet$TemplateCrafterBlockEntity$selectedRecipeIndex(0);
            inventory.set(PREVIEW_SLOT, ItemStack.EMPTY);
            return;
        }
        int nextSelection = selectedRecipeIndex + 1;
        if (nextSelection == candidateCount) nextSelection = 0;
        if (selectedRecipeIndex != nextSelection) {
            hbm$syncSet$TemplateCrafterBlockEntity$selectedRecipeIndex(nextSelection);
            setChanged();
        }
        updatePreview(CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, templateItems));
    }

    @Override
    public void tickServer() {
        if (restoreCandidates) discoverCandidates((ServerLevel) level, true);
        long supplied = ItemEnergyTransfer.extract(this, BATTERY_SLOT, HE_CAPACITY - energy, false);
        if (supplied != 0) setPower(energy + supplied);
        if (!candidates.isEmpty() && energy >= CRAFT_COST) craftSelectedRecipe();
        networkPackNT(15);
    }

    private void craftSelectedRecipe() {
        CraftingRecipe recipe = candidates.get(selectedRecipeIndex);
        CraftingInput input = CraftingInput.of(GRID_WIDTH, GRID_HEIGHT, ingredientItems);
        if (!recipe.matches(input, level)) return;
        ItemStack result = recipe.assemble(input);
        if (result.isEmpty() || !RecipeInputState.crafting(recipe, input, result, true)) return;

        ItemStack output = inventory.get(OUTPUT_SLOT);
        int resultCount = result.getCount();
        if (output.isEmpty()) {
            if (resultCount > getMaxStackSize(result)) return;
        } else {
            if (!ItemStack.isSameItemSameComponents(output, result)) return;
            if (resultCount > getMaxStackSize(output) - output.getCount()) return;
        }

        if (output.isEmpty()) {
            inventory.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(resultCount);
        }
        for (int slot = INGREDIENT_START; slot < OUTPUT_SLOT; slot++) {
            ItemStack ingredient = inventory.get(slot);
            if (ingredient.isEmpty()) continue;
            var remainder = ingredient.getCraftingRemainder();
            ingredient.shrink(1);
            if (ingredient.isEmpty()) {
                ItemStack replacement = ItemStack.EMPTY;
                if (remainder != null) replacement = remainder.create();
                inventory.set(slot, replacement);
            }
        }
        hbm$syncSet$TemplateCrafterBlockEntity$energy((long) (energy - (CRAFT_COST)));
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATED_SLOTS;
    }

    private boolean eligibleTemplate(int index, ItemStack stack) {
        String mode = matcher.mode(index);
        return mode != null
                && !mode.isEmpty()
                && matcher.isValidForFilter(inventory.get(TEMPLATE_START + index), index, stack);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < INGREDIENT_START || slot >= OUTPUT_SLOT || stack.getCount() > INGREDIENT_LIMIT)
            return false;
        int index = slot - INGREDIENT_START;
        ItemStack current = inventory.get(slot);
        if (current.getCount() + stack.getCount() > INGREDIENT_LIMIT) return false;
        if (inventory.get(TEMPLATE_START + index).isEmpty()) return false;
        boolean hasRemainder = stack.getCraftingRemainder() != null;
        if (hasRemainder && stack.getCount() > 1) return false;
        if (!eligibleTemplate(index, stack)) return false;
        if (current.isEmpty()) return true;
        if (hasRemainder) return false;

        for (int otherIndex = 0; otherIndex < TEMPLATE_COUNT; otherIndex++) {
            if (otherIndex == index || !eligibleTemplate(otherIndex, stack)) continue;
            ItemStack other = inventory.get(INGREDIENT_START + otherIndex);
            if (other.isEmpty()) return false;
            if (ModulePatternMatcher.sameItemAndState(other, stack)
                    && other.getCount() < current.getCount()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == OUTPUT_SLOT) return true;
        if (slot < INGREDIENT_START || slot >= OUTPUT_SLOT) return false;
        int index = slot - INGREDIENT_START;
        ItemStack template = inventory.get(TEMPLATE_START + index);
        String mode = matcher.mode(index);
        return template.isEmpty()
                || mode == null
                || mode.isEmpty()
                || !matcher.isValidForFilter(template, index, stack);
    }

    @Override
    public boolean dropsSlot(int slot) {
        return slot >= INGREDIENT_START && slot < SLOT_COUNT;
    }

    @Override
    public boolean stillValid(Player player) {
        return BlockMenus.withinTwelveBlocks(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.autocrafter");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new AutocrafterMenu(containerId, playerInventory, this);
    }

    private static int decodeRecipeIndex(int index) {
        if (index < 0) throw new IllegalArgumentException("Negative crafting recipe index");
        return index;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$TemplateCrafterBlockEntity$energy(input.getLongOr("power", 0L));
        hbm$syncSet$TemplateCrafterBlockEntity$selectedRecipeIndex(
                decodeRecipeIndex(input.getIntOr("rec", 0)));
        matcher.load(input);
        restoreCandidates = true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", energy);
        output.putInt("rec", selectedRecipeIndex);
        matcher.save(output);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xf;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(energy);
            case 1 -> matcher.serialize(output);
            case 2 -> output.writeInt(candidateCount);
            case 3 -> output.writeInt(selectedRecipeIndex);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$TemplateCrafterBlockEntity$energy(input.readLong());
            case 1 -> matcher.deserialize(input);
            case 2 -> hbm$syncSet$TemplateCrafterBlockEntity$candidateCount(input.readInt());
            case 3 ->
                    hbm$syncSet$TemplateCrafterBlockEntity$selectedRecipeIndex(
                            decodeRecipeIndex(input.readInt()));
            default -> super.readSyncUnit(unit, input);
        }
    }

    public final void hbm$syncSet$TemplateCrafterBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$TemplateCrafterBlockEntity$candidateCount(int next) {
        if (this.candidateCount != next) {
            this.candidateCount = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$TemplateCrafterBlockEntity$selectedRecipeIndex(int next) {
        if (this.selectedRecipeIndex != next) {
            this.selectedRecipeIndex = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(TemplateCrafterBlockEntity.class, "matcher", 3, 2);
    }
}
