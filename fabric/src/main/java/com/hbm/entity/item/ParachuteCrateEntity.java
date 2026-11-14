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

package com.hbm.entity.item;

import com.hbm.blockentity.machine.storage.CrateSupplyBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.FarVisible;
import com.hbm.interfaces.StoredItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LinearInterpolationHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ParachuteCrateEntity extends Entity implements StoredItems, FarVisible {
    public List<ItemStack> items = new ArrayList<>();

    @Override
    public void visitStoredItems(Visitor visitor) {
        boolean changed = false;
        for (ItemStack stack : items) changed |= visitor.visit(stack);
        if (changed) items.removeIf(ItemStack::isEmpty);
    }

    public ParachuteCrateEntity(EntityType<? extends ParachuteCrateEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) return;

        var motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (motion.y > -0.2) setDeltaMovement(motion.x, motion.y - 0.02, motion.z);
        if (getY() > 600) setPos(getX(), 600, getZ());

        BlockPos pos = blockPosition();
        if (!level().getBlockState(pos).isAir()) {
            discard();

            BlockPos above = pos.above();
            level().setBlockAndUpdate(above, ModBlocks.CRATE_SUPPLY.get().defaultBlockState());
            if (level().getBlockEntity(above) instanceof CrateSupplyBlockEntity crate) {
                int slot = 0;
                for (ItemStack stack : this.items) {
                    if (stack.isEmpty() || slot >= crate.inventory.size()) continue;
                    crate.inventory.set(slot++, stack);
                }
                crate.setChanged();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.items =
                new ArrayList<>(
                        input.read("items", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("items", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(this.items));
    }
}
