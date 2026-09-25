// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.util.ArrayList;
import java.util.List;

public final class FlushLanes {

    private final IFluidHandlerMK2 self;
    private final List<FlushLane> lanes = new ArrayList<>(2);

    public FlushLanes(IFluidHandlerMK2 self) {
        this.self = self;
    }

    public void add(FluidTankNTM tank, FlushFaces faces) {
        lanes.add(new FlushLane(self, tank, faces));
    }

    public void add(FluidTankNTM tank, FlushFaces faces, int period) {
        lanes.add(new FlushLane(self, tank, faces).every(period));
    }

    public void add(IFluidHandlerMK2 provider, FluidTankNTM tank, FlushFaces faces) {
        lanes.add(new FlushLane(provider, tank, faces));
    }

    public void add(FlushLane lane) {
        lanes.add(lane);
    }

    public List<FlushLane> lanes() {
        return lanes;
    }
}
