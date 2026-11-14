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

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.SatLinkerMenu;
import com.hbm.items.SatelliteFrequencyProvider;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteDeployment;
import com.hbm.saveddata.satellites.SatelliteType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class SatLinkerBlockEntity extends MachineBlockEntity implements MenuProvider {

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_TARGET = 1;
    public static final int SLOT_RANDOMIZE = 2;
    public static final int SLOT_COUNT = 3;

    public static final int FREQ_BOUND = 100000;

    private static final int[] ACCESS_TOP = {SLOT_SOURCE};
    private static final int[] ACCESS_BOTTOM = {SLOT_TARGET};
    private static final int[] ACCESS_SIDE = {SLOT_RANDOMIZE};

    private @Nullable ItemStack rolled;

    public SatLinkerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SATLINKER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.satLinker");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel server) link(server);
    }

    private void link(ServerLevel server) {
        boolean written = false;

        ItemStack source = getItem(SLOT_SOURCE);
        ItemStack target = getItem(SLOT_TARGET);
        if (source.getItem() instanceof SatelliteFrequencyProvider
                && target.getItem() instanceof SatelliteFrequencyProvider) {
            int freq = SatelliteFrequencyProvider.frequency(source);
            if (SatelliteFrequencyProvider.frequency(target) != freq) {
                SatelliteFrequencyProvider.setFrequency(target, freq);
                written = true;
            }
            if (SatelliteType.fromStack(source) != null
                    && SatelliteType.fromStack(target) != null) {
                written |= SatelliteDeployment.copyCarried(source, target);
            }
        }

        ItemStack randomize = getItem(SLOT_RANDOMIZE);
        if (randomize.isEmpty()) {
            rolled = null;
        } else if (randomize != rolled
                && randomize.getItem() instanceof SatelliteFrequencyProvider) {
            rolled = randomize;
            SatelliteSavedData satellites =
                    SatelliteSavedData.at(server, getBlockPos().getX(), getBlockPos().getZ());
            for (int attempt = 0; attempt < FREQ_BOUND; attempt++) {
                int freq = server.getRandom().nextInt(FREQ_BOUND);
                if (satellites.isFreqTaken(freq)) continue;
                SatelliteFrequencyProvider.setFrequency(randomize, freq);
                written = true;
                break;
            }
        }

        if (written) super.setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return switch (side) {
            case DOWN -> ACCESS_BOTTOM;
            case UP -> ACCESS_TOP;
            default -> ACCESS_SIDE;
        };
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SatLinkerMenu(containerId, inventory, this);
    }
}
