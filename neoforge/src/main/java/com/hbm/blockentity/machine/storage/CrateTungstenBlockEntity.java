/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine.storage;

import com.hbm.api.block.LaserHandler;
import com.hbm.inventory.material.Materials;
import com.hbm.inventory.recipes.ingredient.RecipeInputState;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class CrateTungstenBlockEntity extends CrateBlockEntity implements LaserHandler {

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private int heatTimer;

    public CrateTungstenBlockEntity(BlockPos pos, BlockState state) {
        super(CrateType.TUNGSTEN, pos, state);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (!super.canTakeItemThroughFace(slot, stack, side)) return false;
        if (stack.is(ModItems.billetOf(Materials.POLONIUM_210))
                && stack.getComponentsPatch().isEmpty()) return false;
        if (stack.is(ModItems.CRUCIBLE.get())
                && stack.getDamageValue() > 0
                && RecipeInputState.only(stack, DataComponents.DAMAGE)) return false;
        if (!(level instanceof ServerLevel server)) return false;
        return quickCheck
                .getRecipeFor(new SingleRecipeInput(stack), server)
                .filter(recipe -> RecipeInputState.accepts(recipe.value().input(), stack))
                .isEmpty();
    }

    @Override
    public void receiveLaserEnergy(Level level, BlockPos pos, long energy, Direction dir) {
        ServerLevel server = (ServerLevel) level;
        heatTimer = 5;
        BlockState state = getBlockState();
        if (!state.getValue(BlockStateProperties.LIT)) {
            level.setBlock(
                    worldPosition,
                    state.setValue(BlockStateProperties.LIT, true),
                    Block.UPDATE_CLIENTS);
        }
        unpackLootTable(null);
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) continue;
            SingleRecipeInput input = new SingleRecipeInput(stack);
            ItemStack result =
                    quickCheck
                            .getRecipeFor(input, server)
                            .filter(
                                    recipe ->
                                            RecipeInputState.accepts(recipe.value().input(), stack))
                            .map(recipe -> recipe.value().assemble(input))
                            .orElse(ItemStack.EMPTY);
            if (energy > 10_000_000L) {
                if (stack.is(ModItems.billetOf(Materials.POLONIUM_210))
                        && stack.getComponentsPatch().isEmpty()) {
                    result = new ItemStack(ModItems.BILLET_YHARONITE.get());
                } else if (stack.is(ModItems.CRUCIBLE.get())
                        && stack.getDamageValue() > 0
                        && RecipeInputState.only(stack, DataComponents.DAMAGE)) {
                    result = new ItemStack(ModItems.CRUCIBLE.get());
                }
            }
            if (!result.isEmpty()
                    && result.getCount() * stack.getCount() <= result.getMaxStackSize()) {
                setItem(slot, result.copyWithCount(result.getCount() * stack.getCount()));
            }
        }
    }

    @Override
    public void tickServer() {
        if (heatTimer > 0) heatTimer--;
        boolean heated = heatTimer > 0;
        BlockState state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != heated) {
            level.setBlock(
                    worldPosition,
                    state.setValue(BlockStateProperties.LIT, heated),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void tickClient() {
        double x = worldPosition.getX();
        double y = worldPosition.getY();
        double z = worldPosition.getZ();
        var random = level.getRandom();
        level.addParticle(
                ParticleTypes.FLAME,
                x + random.nextDouble(),
                y + 1.1,
                z + random.nextDouble(),
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.SMOKE,
                x + random.nextDouble(),
                y + 1.1,
                z + random.nextDouble(),
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.FLAME,
                x - 0.1,
                y + random.nextDouble(),
                z + random.nextDouble(),
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.SMOKE,
                x - 0.1,
                y + random.nextDouble(),
                z + random.nextDouble(),
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.FLAME,
                x + 1.1,
                y + random.nextDouble(),
                z + random.nextDouble(),
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.SMOKE,
                x + 1.1,
                y + random.nextDouble(),
                z + random.nextDouble(),
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.FLAME,
                x + random.nextDouble(),
                y + random.nextDouble(),
                z - 0.1,
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.SMOKE,
                x + random.nextDouble(),
                y + random.nextDouble(),
                z - 0.1,
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.FLAME,
                x + random.nextDouble(),
                y + random.nextDouble(),
                z + 1.1,
                0,
                0,
                0);
        level.addParticle(
                ParticleTypes.SMOKE,
                x + random.nextDouble(),
                y + random.nextDouble(),
                z + 1.1,
                0,
                0,
                0);
    }
}
