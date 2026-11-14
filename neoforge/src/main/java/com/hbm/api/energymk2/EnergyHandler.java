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

package com.hbm.api.energymk2;

import com.hbm.uninos.BufferedEndpoint;
import com.hbm.uninos.ConnectionPriority;
import com.hbm.util.ChunkUtil;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface EnergyHandler extends BufferedEndpoint {

    long getPower();

    void setPower(long stored);

    long getMaxPower();

    default void usePower(long amount) {
        setPower(getPower() - amount);
        if (amount != 0L) onEnergyChanged();
    }

    default long transferPower(long amount) {
        long stored = getPower();
        long capacity = getMaxPower();
        long room = capacity - stored;
        boolean roomOverflowed = ((capacity ^ stored) & (capacity ^ room)) < 0L;
        if (roomOverflowed ? capacity > stored : amount <= room) {
            setPower(stored + amount);
            if (amount != 0L) onEnergyChanged();
            return 0L;
        }
        setPower(capacity);
        if (capacity != stored) onEnergyChanged();
        long refused = amount - room;
        return roomOverflowed || ((amount ^ room) & (amount ^ refused)) < 0L
                ? Long.MAX_VALUE
                : refused;
    }

    default long getSendRate() {
        return getMaxPower();
    }

    default long getReceiveRate() {
        return getMaxPower();
    }

    default ConnectionPriority getEnergyTier() {
        return ConnectionPriority.NORMAL;
    }

    default void onEnergyChanged() {
        if (this instanceof BlockEntity be) ChunkUtil.markUnsaved(be);
    }

    default boolean canReceiveDirectly() {
        return true;
    }
}
