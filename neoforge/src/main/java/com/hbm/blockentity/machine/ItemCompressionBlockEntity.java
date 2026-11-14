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
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.FunnelMenu;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.IdentityHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ItemCompressionBlockEntity extends MachineBlockEntity
        implements MenuProvider, SyncUnitSchema {
    public static final int SLOT_COUNT = 18;
    private static final int PAIR_COUNT = SLOT_COUNT / 2;
    private static final int[] INPUT_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] OUTPUT_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final IdentityHashMap<Item, ItemStack> FOUR_RESULTS = new IdentityHashMap<>();
    private static final IdentityHashMap<Item, ItemStack> NINE_RESULTS = new IdentityHashMap<>();
    private static RecipeSource.Token pristineSource = RecipeSource.NEVER_FILLED;

    public int mode;

    private final StatefulResults[] statefulResults = new StatefulResults[PAIR_COUNT];

    public ItemCompressionBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUNNEL.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        var source = RecipeSource.token();
        if (source != null && source.source() instanceof RecipeManager recipes) {
            selectRecipeSource(source);
            unpackLootTable(null);
            boolean changed = false;
            for (int pair = 0; pair < PAIR_COUNT; pair++) {
                var input = inventory.get(pair);
                if (mode != 2 && input.getCount() >= 9) {
                    var result = resultFor(pair, input, 3, source, recipes);
                    if (!result.isEmpty()) {
                        changed |= compress(pair, input, result, 9);
                        continue;
                    }
                }
                if (mode != 1 && input.getCount() >= 4) {
                    var result = resultFor(pair, input, 2, source, recipes);
                    if (!result.isEmpty()) changed |= compress(pair, input, result, 4);
                }
            }
            if (changed) setChanged();
        }
        networkPackNT(15);
    }

    private boolean compress(int pair, ItemStack input, ItemStack result, int consumed) {
        var output = inventory.get(pair + PAIR_COUNT);
        int room = result.getMaxStackSize();
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, result)) return false;
            room -= output.getCount();
        }
        if (result.getCount() > room) return false;
        if (output.isEmpty()) inventory.set(pair + PAIR_COUNT, result.copy());
        else output.grow(result.getCount());
        input.shrink(consumed);
        return true;
    }

    private static void selectRecipeSource(RecipeSource.Token source) {
        if (source.equals(pristineSource)) return;
        FOUR_RESULTS.clear();
        NINE_RESULTS.clear();
        pristineSource = source;
    }

    private ItemStack resultFor(
            int pair,
            ItemStack input,
            int width,
            RecipeSource.Token source,
            RecipeManager recipes) {
        if (input.getComponentsPatch().isEmpty()) {
            var results = width == 2 ? FOUR_RESULTS : NINE_RESULTS;
            var result = results.get(input.getItem());
            if (result == null) {
                result = query(input.copyWithCount(1), width, recipes);
                results.put(input.getItem(), result);
            }
            return result;
        }
        var held = statefulResults[pair];
        if (held == null
                || !source.equals(held.source)
                || !ItemStack.isSameItemSameComponents(input, held.input)) {
            held = new StatefulResults(source, input.copyWithCount(1));
            statefulResults[pair] = held;
        }
        var result = width == 2 ? held.four : held.nine;
        if (result == null) {
            result = query(held.input, width, recipes);
            if (width == 2) held.four = result;
            else held.nine = result;
        }
        return result;
    }

    private ItemStack query(ItemStack ingredient, int width, RecipeManager recipes) {
        var grid = CraftingInput.of(width, width, Collections.nCopies(width * width, ingredient));
        var match = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level);
        if (match.isEmpty()) return ItemStack.EMPTY;
        var recipe = match.get().value();
        var result = recipe.assemble(grid);
        return RecipeInputState.crafting(recipe, grid, result, false) ? result : ItemStack.EMPTY;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= PAIR_COUNT) return false;
        if (!getItem(slot).isEmpty()) return true;
        var source = RecipeSource.token();
        if (source == null || !(source.source() instanceof RecipeManager recipes)) return false;
        selectRecipeSource(source);
        return !resultFor(slot, stack, 2, source, recipes).isEmpty()
                || !resultFor(slot, stack, 3, source, recipes).isEmpty();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN
                ? slot >= PAIR_COUNT
                : side != Direction.UP && slot < PAIR_COUNT;
    }

    public void advanceMode() {
        hbm$syncSet$ItemCompressionBlockEntity$mode((mode + 1) % 3);
        setChanged();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineFunnel");
    }

    @Override
    public FunnelMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new FunnelMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ItemCompressionBlockEntity$mode(decodeMode(input.getIntOr("mode", 0)));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode);
    }

    @Override
    public long syncUnitMask() {
        return 1;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        output.writeInt(mode);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        hbm$syncSet$ItemCompressionBlockEntity$mode(decodeMode(input.readInt()));
    }

    private static int decodeMode(int mode) {
        if (mode < 0 || mode > 2) throw new IllegalArgumentException();
        return mode;
    }

    private static final class StatefulResults {
        private final RecipeSource.Token source;
        private final ItemStack input;
        private @Nullable ItemStack four;
        private @Nullable ItemStack nine;

        private StatefulResults(RecipeSource.Token source, ItemStack input) {
            this.source = source;
            this.input = input;
        }
    }

    public final void hbm$syncSet$ItemCompressionBlockEntity$mode(int next) {
        if (this.mode != next) {
            this.mode = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }
}
