// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.turret;

import com.hbm.tileentity.turret.BlockEntityTurretBaseArtillery;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public abstract class ArtilleryPeripheral<BE extends BlockEntityTurretBaseArtillery>
        extends TurretPeripheral<BE> {

    protected ArtilleryPeripheral(BE machine) {
        super(machine, "ntm_artillery", SLOTS, 0);
    }

    protected static double distance(BlockPos pos, double x, double y, double z) {
        return Math.sqrt(
                Math.pow(pos.getX() - x, 2)
                        + Math.pow(pos.getY() - y, 2)
                        + Math.pow(pos.getZ() - z, 2));
    }

    @LuaFunction(mainThread = true)
    public final Object[] getCurrentTarget() {
        BE machine = machine();

        if (machine.queuedTargets() == 0) return new Object[] {null, "Index: 0, Size: 0"};
        Vec3 target = machine.queuedTarget(0);
        return new Object[] {target.x, target.y, target.z};
    }

    @LuaFunction
    public final Object[] getTargetDistance(double x, double y, double z) {
        return read(
                s ->
                        new Object[] {
                            Math.sqrt(
                                    Math.pow(s.intAt(X) - x, 2)
                                            + Math.pow(s.intAt(Y) - y, 2)
                                            + Math.pow(s.intAt(Z) - z, 2))
                        });
    }
}
