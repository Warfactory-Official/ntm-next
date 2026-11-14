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

import com.hbm.api.conveyor.ConveyorInputHandler;
import com.hbm.api.conveyor.ConveyorPackage;
import com.hbm.entity.ModEntities;
import com.hbm.handler.UnstableFuses;
import com.hbm.interfaces.StoredItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MovingPackageEntity extends MovingConveyorObjectEntity
        implements ConveyorPackage, StoredItems {

    protected List<ItemStack> contents = new ArrayList<>();

    @Override
    public void visitStoredItems(Visitor visitor) {
        boolean changed = false;
        for (ItemStack stack : contents) changed |= visitor.visit(stack);
        if (changed) contents.removeIf(ItemStack::isEmpty);
    }

    public MovingPackageEntity(EntityType<? extends MovingPackageEntity> type, Level level) {
        super(type, level);
    }

    public MovingPackageEntity(Level level) {
        super(ModEntities.MOVING_PACKAGE.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    public void setItemStacks(List<ItemStack> stacks) {
        this.contents = new ArrayList<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) this.contents.add(stack.copy());
        UnstableFuses.changed(this);
    }

    @Override
    public List<ItemStack> getItemStacks() {
        return this.contents;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (level().isClientSide() || isRemoved()) return InteractionResult.PASS;

        for (ItemStack stack : this.contents) {
            player.getInventory().placeItemBackInInventory(stack);
        }

        discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            discard();
            for (ItemStack stack : this.contents) {
                server.addFreshEntity(
                        new ItemEntity(server, getX(), getY() + 0.125, getZ(), stack));
            }
        }
        return false;
    }

    @Override
    public void enterBlock(ConveyorInputHandler enterable, BlockPos pos, @Nullable Direction dir) {
        if (isRemoved()) return;

        if (enterable.canPackageEnter(level(), pos, dir, this)) {
            enterable.onPackageEnter(level(), pos, dir, this);
            discard();
        }
    }

    @Override
    public boolean onLeaveConveyor() {
        discard();

        Vec3 motion = getDeltaMovement();
        for (ItemStack stack : this.contents) {
            ItemEntity item =
                    new ItemEntity(
                            level(),
                            getX() + motion.x * 2,
                            getY() + motion.y * 2,
                            getZ() + motion.z * 2,
                            stack);
            item.setDeltaMovement(motion.x * 2, 0.1, motion.z * 2);
            level().addFreshEntity(item);
        }

        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.contents =
                new ArrayList<>(input.read("contents", ItemStack.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("contents", ItemStack.CODEC.listOf(), List.copyOf(this.contents));
    }
}
