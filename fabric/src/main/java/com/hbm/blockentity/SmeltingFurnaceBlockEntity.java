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

package com.hbm.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class SmeltingFurnaceBlockEntity extends MachineBlockEntity {

    protected final FurnaceRecipesUsed recipesUsed;

    protected SmeltingFurnaceBlockEntity(
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

    @Override
    public void forgetBulkDrops() {
        super.forgetBulkDrops();
        recipesUsed.clear();
    }
}
