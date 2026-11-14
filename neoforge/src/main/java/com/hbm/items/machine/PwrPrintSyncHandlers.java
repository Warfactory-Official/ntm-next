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

package com.hbm.items.machine;

import com.hbm.client.gui.PwrPrinterScreen;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class PwrPrintSyncHandlers {
    private PwrPrintSyncHandlers() {}

    public static BlockState state(int stateId) {
        BlockState state = Block.BLOCK_STATE_REGISTRY.byId(stateId);
        if (state == null) throw new DecoderException("Unknown PWR print block state " + stateId);
        return state;
    }

    public static void open(
            BlockPos min,
            BlockPos max,
            Direction direction,
            long[] positions,
            BlockState[] states) {
        if (!direction.getAxis().isHorizontal())
            throw new DecoderException("Invalid PWR print direction");
        PwrPrinterScreen.open(new PwrPrintData(min, max, direction, positions, states));
    }
}
