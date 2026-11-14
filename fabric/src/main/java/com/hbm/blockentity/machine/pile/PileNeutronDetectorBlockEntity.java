/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

package com.hbm.blockentity.machine.pile;

import com.hbm.blockentity.BlockEntityTicks;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.pile.DrilledGraphiteBlock;
import com.hbm.blocks.machine.pile.GraphiteNeutronDetectorBlock;
import com.hbm.handler.neutron.PileNeutronReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PileNeutronDetectorBlockEntity extends BlockEntity
        implements BlockEntityTicks, PileNeutronReceiver {
    public int deposited;
    public int previousDeposit;
    public int tripThreshold = 10;

    public PileNeutronDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_NEUTRONDETECTOR.get(), pos, state);
    }

    @Override
    public void absorbNeutrons(int neutrons) {
        deposited += neutrons;
    }

    @Override
    public void tickServer() {
        var state = getBlockState();
        boolean withdrawn = state.getValue(DrilledGraphiteBlock.WITHDRAWN);
        if (withdrawn
                ? deposited >= tripThreshold
                : deposited < tripThreshold && previousDeposit < tripThreshold) {
            ((GraphiteNeutronDetectorBlock) state.getBlock()).triggerRods(level, worldPosition);
        }
        previousDeposit = deposited;
        deposited = 0;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tripThreshold = input.getIntOr("maxNeutrons", 10);
        deposited = 0;
        previousDeposit = 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("maxNeutrons", tripThreshold);
    }
}
