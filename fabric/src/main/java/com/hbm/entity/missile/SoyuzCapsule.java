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

package com.hbm.entity.missile;

import com.hbm.blockentity.machine.storage.CargoCapsuleBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.ModItems;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SoyuzCapsule extends Entity implements StoredItems {

    public static final int CARGO_SLOTS = 18;
    private static final double CEILING = 600D;

    public @Nullable ItemStack[] cargo = new ItemStack[CARGO_SLOTS];
    public int skin;

    public SoyuzCapsule(EntityType<? extends SoyuzCapsule> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        var motion = getDeltaMovement();
        double my = motion.y;
        if (my > -0.2D) {
            my -= 0.02D;
            setDeltaMovement(motion.x, my, motion.z);
        }
        if (getY() > CEILING) setPos(getX(), CEILING, getZ());
        setOldPos();
        setPos(getX() + motion.x, getY() + my, getZ() + motion.z);
        if (level().getBlockState(blockPosition()).isAir()) return;
        discard();
        if (level() instanceof ServerLevel level) land(level);
    }

    private void land(ServerLevel level) {
        var site = blockPosition().above();
        level.setBlock(site, ModBlocks.SOYUZ_CAPSULE.get().defaultBlockState(), Block.UPDATE_ALL);
        if (!(level.getBlockEntity(site) instanceof CargoCapsuleBlockEntity capsule)) return;
        for (int i = 0; i < CARGO_SLOTS; i++) {
            var stack = cargo[i];
            capsule.setItem(i, stack == null ? ItemStack.EMPTY : stack);
        }
        capsule.setItem(
                CargoCapsuleBlockEntity.SLOT_ROCKET,
                new ItemStack(ModItems.soyuzOfSkin(skin).get()));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000D;
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        for (int i = 0; i < CARGO_SLOTS; i++) {
            var stack = cargo[i];
            if (stack != null && visitor.visit(stack) && stack.isEmpty())
                cargo[i] = ItemStack.EMPTY;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("soyuz", skin);
        var items = output.childrenList("items");
        for (int i = 0; i < CARGO_SLOTS; i++) {
            var stack = cargo[i];
            if (stack == null || stack.isEmpty()) continue;
            var entry = items.addChild();
            entry.putByte("slot", (byte) i);
            entry.store("item", ItemStack.OPTIONAL_CODEC, stack);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        skin = input.getIntOr("soyuz", skin);
        for (var entry : input.childrenListOrEmpty("items")) {
            int slot = entry.getByteOr("slot", (byte) -1);
            if (slot >= 0 && slot < CARGO_SLOTS) {
                cargo[slot] = entry.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            }
        }
    }
}
