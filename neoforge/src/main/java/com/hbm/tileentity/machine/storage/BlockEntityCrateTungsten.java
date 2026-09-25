// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.api.block.ILaserable;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class BlockEntityCrateTungsten extends BlockEntityCrate implements ILaserable {

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private int heatTimer;

    public BlockEntityCrateTungsten(BlockPos pos, BlockState state) {
        super(CrateType.TUNGSTEN, pos, state);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (!super.canTakeItemThroughFace(slot, stack, side)) return false;
        if (stack.is(ModItems.billetOf(Mats.MAT_POLONIUM))) return false;
        if (stack.is(ModItems.CRUCIBLE.get()) && stack.getDamageValue() > 0) return false;
        if (!(level instanceof ServerLevel server)) return false;
        return quickCheck.getRecipeFor(new SingleRecipeInput(stack), server).isEmpty();
    }

    @Override
    public void addEnergy(Level level, BlockPos pos, long energy, Direction dir) {
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
                            .map(recipe -> recipe.value().assemble(input))
                            .orElse(ItemStack.EMPTY);
            if (energy > 10_000_000L) {
                if (stack.is(ModItems.billetOf(Mats.MAT_POLONIUM))) {
                    result = new ItemStack(ModItems.BILLET_YHARONITE.get());
                } else if (stack.is(ModItems.CRUCIBLE.get()) && stack.getDamageValue() > 0) {
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
