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

package com.hbm.inventory.container;

import com.hbm.blockentity.generator.IsotopeGeneratorBlockEntity;
import com.hbm.config.ModeStatus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RTGMenu extends BlockEntityMenu<IsotopeGeneratorBlockEntity> {

    private final SyncedData data;
    private final boolean clientSide;

    public RTGMenu(int containerId, Inventory playerInv, IsotopeGeneratorBlockEntity be) {
        super(ModMenus.MACHINE_RTG.get(), containerId, be);
        checkContainerSize(be, IsotopeGeneratorBlockEntity.SLOT_COUNT);
        this.clientSide = playerInv.player.level().isClientSide();
        this.data =
                playerInv.player.level().isClientSide()
                        ? SyncedData.client(IsotopeGeneratorBlockEntity.class)
                        : SyncedData.of(be);

        for (int i = 0; i < IsotopeGeneratorBlockEntity.SLOT_COUNT; i++) {
            addSlot(new Slot(be, i, 16 + (i % 5) * 18, 18 + (i / 5) * 18));
        }

        addStandardInventorySlots(playerInv, 8, 106);
        addDataSlots(data);
    }

    public int getHeat() {
        return data.getInt("heat");
    }

    public int getHeatScaled(int i) {
        int ceiling =
                clientSide
                        ? (ModeStatus.clientRtgDecay ? 600 : 200)
                        : IsotopeGeneratorBlockEntity.maxHeat();
        return getHeat() * i / ceiling;
    }

    public long getPower() {
        return blockEntity().power;
    }

    public long getPowerScaled(long i) {
        return blockEntity().power * i / IsotopeGeneratorBlockEntity.MAX_POWER;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveUnsorted(player, index, IsotopeGeneratorBlockEntity.SLOT_COUNT);
    }
}
