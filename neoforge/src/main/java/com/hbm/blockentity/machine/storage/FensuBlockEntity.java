/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine.storage;

import com.hbm.blocks.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class FensuBlockEntity extends BatteryBlockEntity {

    public static final long MAX_TRANSFER = 10_000_000_000_000_000L;

    public float rotation;
    public float prevRotation;

    public FensuBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FENSU.get(), pos, state, Long.MAX_VALUE);
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        rotation += getSpeed();
        if (rotation >= 360F) {
            rotation -= 360F;
            prevRotation -= 360F;
        }
    }

    public float getSpeed() {
        return (float) Math.pow(Math.log(0.75D * getPower() + 1D) * 0.05F, 5D);
    }

    @Override
    public long transferPower(long amount) {
        long capped = Math.min(amount, MAX_TRANSFER);
        long room = Long.MAX_VALUE - getPower();
        long accepted = Math.min(capped, room);
        if (accepted > 0L) {
            setPower(getPower() + accepted);
            onEnergyChanged();
        }
        return amount - accepted;
    }

    @Override
    public long getSendRate() {
        return getActiveMode().sends() ? MAX_TRANSFER : 0L;
    }

    @Override
    public long getReceiveRate() {
        return getActiveMode().receives() ? MAX_TRANSFER : 0L;
    }

    @Override
    protected long meanStock(long began, long ended) {
        return (began >> 1) + (ended >> 1);
    }

    @Override
    public long fillScaled(long units) {
        return (long) (units * ((double) getPower() / getMaxPower()));
    }

    @Override
    protected boolean pushes(TransferMode mode) {
        return mode.sends();
    }

    @Override
    protected boolean pushesThrough(Direction side) {
        return side == Direction.DOWN;
    }

    @Override
    protected boolean conducts(TransferMode mode) {
        return false;
    }

    @Override
    protected void updateComparator() {}

    @Override
    public int getComparatorPower() {
        return 0;
    }
}
