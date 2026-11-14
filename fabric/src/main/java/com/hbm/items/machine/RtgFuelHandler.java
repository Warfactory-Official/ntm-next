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

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.config.BalanceConfig;
import com.hbm.data.MachineData;

public record RtgFuelHandler(boolean decays, boolean scalesPower) {

    private static final RtgFuelHandler[] ALL = {
        new RtgFuelHandler(false, false), new RtgFuelHandler(false, true),
        new RtgFuelHandler(true, false), new RtgFuelHandler(true, true),
    };

    public static RtgFuelHandler current() {
        boolean preset = BalanceConfig.enable528;
        boolean decays = preset || MachineData.RTG_DECAY.get();
        boolean scales = preset || MachineData.RTG_SCALE_POWER.get();
        return ALL[(decays ? 2 : 0) | (scales ? 1 : 0)];
    }

    public int tickPellets(MachineBlockEntity machine, int[] slots) {
        var items = machine.inventory;
        int heat = 0;
        boolean changed = false;
        for (int slot : slots) {
            var stack = items.get(slot);
            if (!(stack.getItem() instanceof RTGPelletItem pellet)) continue;
            heat += pellet.power(stack, this);
            if (!decays || !pellet.getDoesDecay()) continue;
            if (pellet.getLifespan(stack) > 0) {
                pellet.decay(stack);
                changed = true;
            } else {
                var residual = pellet.getDecayItem();
                if (residual == null) continue;
                items.set(slot, residual);
                changed = true;
            }
        }
        if (changed) machine.setChanged();
        return heat;
    }
}
