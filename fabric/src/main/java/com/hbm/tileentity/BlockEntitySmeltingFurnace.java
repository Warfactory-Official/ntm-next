// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BlockEntitySmeltingFurnace extends BlockEntityMachineBase {

    protected final FurnaceRecipesUsed recipesUsed;

    protected BlockEntitySmeltingFurnace(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int slots,
            int... outputSlots) {
        super(type, pos, state, slots);
        this.recipesUsed = new FurnaceRecipesUsed(outputSlots);
    }

    public final void awardUsedRecipesAndPopExperience(ServerPlayer player, int slot) {

        if (recipesUsed.award(player, slot, inventory)) markChanged();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server) recipesUsed.popExperience(server, pos);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        recipesUsed.load(input);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        recipesUsed.save(output);
    }
}
