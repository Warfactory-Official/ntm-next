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

import com.hbm.blockentity.HatchSwing;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.DataGroups;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.AshpitMenu;
import com.hbm.items.ModItems;
import com.hbm.items.machine.AshType;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AshpitBlockEntity extends MachineBlockEntity implements MenuProvider, SyncUnitSchema {

    private static final int SLOTS = 5;
    private static final AshType[] KINDS = {
        AshType.WOOD, AshType.COAL, AshType.MISC, AshType.FLY, AshType.SOOT
    };
    private static final List<DataGroups.Param<Integer>> THRESHOLDS =
            List.of(
                    MachineData.ASHPIT_THRESHOLD_WOOD,
                    MachineData.ASHPIT_THRESHOLD_COAL,
                    MachineData.ASHPIT_THRESHOLD_MISC,
                    MachineData.ASHPIT_THRESHOLD_FLY,
                    MachineData.ASHPIT_THRESHOLD_SOOT);
    private static final String[] KEYS = {
        "ashLevelWood", "ashLevelCoal", "ashLevelMisc", "ashLevelFly", "ashLevelSoot"
    };

    private final int[] ash = new int[KINDS.length];

    public boolean isFull;
    public float doorAngle;
    public float prevDoorAngle;
    private int playersUsing;

    public AshpitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASHPIT.get(), pos, state, SLOTS);
    }

    public void addAsh(AshType kind, int amount) {
        ash[kind.ordinal()] += amount;
    }

    public int ash(AshType kind) {
        return ash[kind.ordinal()];
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!level.isClientSide())
            this.hbm$syncSet$AshpitBlockEntity$playersUsing((int) (this.playersUsing + 1));
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!level.isClientSide())
            this.hbm$syncSet$AshpitBlockEntity$playersUsing((int) (this.playersUsing - 1));
    }

    @Override
    public void tickServer() {
        for (int k = 0; k < KINDS.length; k++) {
            int threshold = THRESHOLDS.get(k).get();
            if (ash[k] >= threshold && collect(KINDS[k], threshold)) ash[k] -= threshold;
        }
        hbm$syncSet$AshpitBlockEntity$isFull(holdsAsh());
        networkPackNT(50);
    }

    private boolean holdsAsh() {
        for (ItemStack stack : inventory) if (!stack.isEmpty()) return true;
        return false;
    }

    private boolean collect(AshType kind, int threshold) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) {
                inventory.set(i, ModItems.POWDER_ASH.stack(kind));
                ash[AshType.WOOD.ordinal()] -= threshold;
            } else if (ModItems.POWDER_ASH.is(stack, kind)
                    && stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
            } else {
                continue;
            }
            markChanged();
            return true;
        }
        return false;
    }

    @Override
    public void tickClient() {
        prevDoorAngle = doorAngle;
        doorAngle = HatchSwing.next(doorAngle, playersUsing > 0);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return new int[] {0, 1, 2, 3, 4};
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.ashpit");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new AshpitMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int k = 0; k < KEYS.length; k++) ash[k] = input.getIntOr(KEYS[k], 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int k = 0; k < KEYS.length; k++) output.putInt(KEYS[k], ash[k]);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.playersUsing);
            case 1 -> output.writeBoolean(this.isFull);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.hbm$syncSet$AshpitBlockEntity$playersUsing(input.readInt());
            case 1 -> this.hbm$syncSet$AshpitBlockEntity$isFull(input.readBoolean());
            default -> throw new IllegalArgumentException();
        }
    }

    public final void hbm$syncSet$AshpitBlockEntity$isFull(boolean next) {
        if (this.isFull != next) {
            this.isFull = next;
            if ((1L << 1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 1);
        }
    }

    public final void hbm$syncSet$AshpitBlockEntity$playersUsing(int next) {
        if (this.playersUsing != next) {
            this.playersUsing = next;
            if ((1L << 0) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L << 0);
        }
    }
}
