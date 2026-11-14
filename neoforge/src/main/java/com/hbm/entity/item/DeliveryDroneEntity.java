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

import com.hbm.entity.ModEntities;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.items.ModItems;
import com.hbm.items.tool.DroneItem.EnumDroneType;
import com.hbm.util.ChunkShapeHelper;
import com.hbm.util.ChunkUtil;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class DeliveryDroneEntity extends DroneBaseEntity implements Container {

    public static final int SLOT_COUNT = 18;
    private static final EntityDataAccessor<Boolean> EXPRESS =
            SynchedEntityData.defineId(DeliveryDroneEntity.class, EntityDataSerializers.BOOLEAN);

    private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    public @Nullable FluidStackNTM fluid;

    private boolean chunkLoading;

    public DeliveryDroneEntity(EntityType<? extends DeliveryDroneEntity> type, Level level) {
        super(type, level);
    }

    public DeliveryDroneEntity(Level level) {
        super(ModEntities.DELIVERY_DRONE.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EXPRESS, false);
    }

    public void setExpress(boolean express) {
        entityData.set(EXPRESS, express);
    }

    public boolean isExpress() {
        return entityData.get(EXPRESS);
    }

    public void setChunkLoading(boolean chunkLoading) {
        this.chunkLoading = chunkLoading;
    }

    @Override
    public double getSpeed() {
        return isExpress() ? 0.375 * 3 : 0.375;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (isRemoved()
                || !(attacker instanceof Player player)
                || player.isSpectator()
                || !(level() instanceof ServerLevel server)) {
            return false;
        }

        discard();
        for (ItemStack stack : this.slots) if (!stack.isEmpty()) spawnAtLocation(server, stack);
        spawnAtLocation(server, ModItems.DRONE.stack(droneType()));
        return false;
    }

    private EnumDroneType droneType() {
        if (isExpress()) {
            return this.chunkLoading
                    ? EnumDroneType.PATROL_EXPRESS_CHUNKLOADING
                    : EnumDroneType.PATROL_EXPRESS;
        }
        return this.chunkLoading ? EnumDroneType.PATROL_CHUNKLOADING : EnumDroneType.PATROL;
    }

    @Override
    protected void loadNeighboringChunks() {
        if (!this.chunkLoading || !(level() instanceof ServerLevel server)) return;

        LongIterator chunks =
                ChunkShapeHelper.alongSegment(
                                Mth.floor(getX()),
                                Mth.floor(getZ()),
                                Mth.floor(getX() + getDeltaMovement().x()),
                                Mth.floor(getZ() + getDeltaMovement().z()),
                                8)
                        .iterator();
        while (chunks.hasNext()) {
            ChunkUtil.holdForEntity(
                    server, ChunkPos.unpack(chunks.nextLong()), ChunkUtil.HOLD_RADIUS);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.slots.clear();
        ContainerHelper.loadAllItems(input, this.slots);
        this.fluid = input.read("fluid", FluidStackNTM.CODEC).orElse(null);
        setExpress(input.getBooleanOr("express", false));
        this.chunkLoading = input.getBooleanOr("chunkLoading", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        ContainerHelper.saveAllItems(output, this.slots);
        if (this.fluid != null) output.store("fluid", FluidStackNTM.CODEC, this.fluid);
        output.putBoolean("express", isExpress());
        output.putBoolean("chunkLoading", this.chunkLoading);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.slots) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.slots.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(this.slots, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.slots, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.slots.set(slot, stack);
        stack.limitSize(getMaxStackSize());
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        this.slots.clear();
    }
}
