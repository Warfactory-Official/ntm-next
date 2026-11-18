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

package com.hbm.blockentity.door;

import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.blocks.generic.DoorBaseBlock;
import com.hbm.items.tool.KeyPinItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class DoorBaseBlockEntity extends LockableBlockEntity {
    private boolean wasPowered;
    private boolean redstoneOnly;

    protected DoorBaseBlockEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state, 0);
    }

    private DoorBaseBlock door() {
        return (DoorBaseBlock) getBlockState().getBlock();
    }

    public final boolean isRedstoneOnly() {
        var door = door();
        return door.redstoneOnlyListed() || door.toolableListed() && redstoneOnly;
    }

    public final boolean toggleRedstoneOnly(Player player) {
        if (player.isSpectator()) return false;
        var key = player.getOffhandItem();
        if (isLocked()
                && (!(key.getItem() instanceof KeyPinItem)
                        || KeyPinItem.getPins(key) != getPinCode())) return false;
        redstoneOnly = !redstoneOnly;
        markChanged();
        return true;
    }

    protected boolean pulses() {
        return door().pulseListed();
    }

    protected final void tickRedstone(boolean powered) {
        if (powered && !wasPowered && pulses()) toggleOnSignal();
        if (wasPowered != powered) {
            wasPowered = powered;
            markChanged();
        }
    }

    protected abstract void toggleOnSignal();

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("wasPowered", wasPowered);
        output.putBoolean("redstoneOnly", redstoneOnly);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        wasPowered = input.getBooleanOr("wasPowered", false);
        redstoneOnly = input.getBooleanOr("redstoneOnly", false);
    }
}
