// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.turret;

import com.hbm.tileentity.turret.BlockEntityTurretHIMARS;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.phys.Vec3;

public final class HimarsPeripheral extends ArtilleryPeripheral<BlockEntityTurretHIMARS> {

    public HimarsPeripheral(BlockEntityTurretHIMARS machine) {
        super(machine);
    }

    @LuaFunction(mainThread = true)
    public final Object[] addCoords(double x, double y, double z) {
        BlockEntityTurretHIMARS machine = machine();
        machine.mode = BlockEntityTurretHIMARS.MODE_MANUAL;
        machine.addQueuedTarget(new Vec3(x, y, z));
        return new Object[] {};
    }
}
