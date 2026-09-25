// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.turret;

import com.hbm.tileentity.turret.BlockEntityTurretArty;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.phys.Vec3;

public final class ArtyPeripheral extends ArtilleryPeripheral<BlockEntityTurretArty> {

    public ArtyPeripheral(BlockEntityTurretArty machine) {
        super(machine);
    }

    @LuaFunction(mainThread = true)
    public final Object[] addCoords(double x, double y, double z) {
        BlockEntityTurretArty machine = machine();
        machine.mode = BlockEntityTurretArty.MODE_MANUAL;
        if (distance(machine.getBlockPos(), x, y, z) >= machine.getDetectorRange())
            return new Object[] {false};
        machine.addQueuedTarget(new Vec3(x, y, z));
        return new Object[] {true};
    }
}
