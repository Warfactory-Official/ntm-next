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

package com.hbm.blockentity.network;

import com.hbm.api.conveyor.ConveyorBelt;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.MovingItemEntity;
import com.hbm.inventory.container.CraneUnboxerMenu;
import com.hbm.items.machine.upgrade.MachineUpgradeItem;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CraneUnboxerBlockEntity extends CraneBaseBlockEntity implements MenuProvider {

    public static final int BUFFER_SLOTS = 21;
    public static final int SLOT_UPGRADE_STACK = 21;
    public static final int SLOT_UPGRADE_EJECTOR = 22;
    public static final int SLOT_COUNT = 23;

    private static final int[] ACCESS = accessBuffer();

    public CraneUnboxerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_UNBOXER.get(), pos, state, SLOT_COUNT);
    }

    private static int[] accessBuffer() {
        int[] access = new int[BUFFER_SLOTS];
        for (int i = 0; i < BUFFER_SLOTS; i++) access[i] = i;
        return access;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.craneUnboxer");
    }

    private static int upgradeTier(ItemStack stack, UpgradeType type) {
        return MachineUpgradeItem.getLevel(stack, type);
    }

    @Override
    public void tickServer() {
        int delay =
                CraneUpgrades.delayFor(
                        upgradeTier(getItem(SLOT_UPGRADE_EJECTOR), UpgradeType.EJECTOR));

        if (!TickPhase.every(this, delay) || level.hasNeighborSignal(worldPosition)) return;

        int amount =
                CraneUpgrades.amountFor(
                        upgradeTier(getItem(SLOT_UPGRADE_STACK), UpgradeType.STACK));

        Direction output = getInputSide();
        BlockPos outputPos = worldPosition.relative(output);
        ConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, outputPos);
        if (belt == null) return;

        for (int i = 0; i < BUFFER_SLOTS; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            int toSend = Math.min(amount, stack.getCount());
            ItemStack sent = stack.copyWithCount(toSend);
            removeItem(i, toSend);

            Vec3 pos =
                    new Vec3(
                            worldPosition.getX() + 0.5 + output.getStepX() * 0.55,
                            worldPosition.getY() + 0.5 + output.getStepY() * 0.55,
                            worldPosition.getZ() + 0.5 + output.getStepZ() * 0.55);
            Vec3 snap = belt.getSnappingPosition(level, outputPos, pos);

            MovingItemEntity moving = new MovingItemEntity(level);
            moving.setItemStack(sent);
            moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
            level.addFreshEntity(moving);
            return;
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public int getComparatorPower() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CraneUnboxerMenu(containerId, inventory, this);
    }
}
