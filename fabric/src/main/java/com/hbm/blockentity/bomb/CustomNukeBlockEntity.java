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

package com.hbm.blockentity.bomb;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.explosion.assembly.ChargeIngredients;
import com.hbm.explosion.assembly.ChargeYield;
import com.hbm.inventory.container.NukeCustomMenu;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public final class CustomNukeBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int SLOT_COUNT = 27;

    public CustomNukeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_CUSTOM.get(), pos, state, SLOT_COUNT);
    }

    public ChargeYield snapshot() {
        return ChargeIngredients.evaluate(inventory);
    }

    public boolean hasFallingAccessory() {
        var accessory = ChargeIngredients.fallingAccessory();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            var stack = inventory.get(slot);
            if (!stack.isEmpty() && stack.is(accessory)) return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukeCustom");
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        if (Objects.equals(getCustomName(), name)) return;
        super.setCustomName(name);
        setChanged();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new NukeCustomMenu(containerId, playerInventory, this);
    }
}
