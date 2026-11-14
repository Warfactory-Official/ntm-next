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

package com.hbm.compat.computercraft.turret;

import com.hbm.blockentity.turret.TurretBaseArtilleryBlockEntity;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public abstract class ArtilleryPeripheral<BE extends TurretBaseArtilleryBlockEntity>
        extends TurretControlPeripheral<BE> {

    private static final int QUEUED = TURRET_NUMBERS;
    private static final int TARGET_X = TURRET_NUMBERS + 1;
    private static final int TARGET_Y = TURRET_NUMBERS + 2;
    private static final int TARGET_Z = TURRET_NUMBERS + 3;

    protected ArtilleryPeripheral(BE machine) {
        super(machine, "ntm_artillery", TURRET_NUMBERS + 4, 0);
    }

    @Override
    protected void capture(BE machine) {
        super.capture(machine);
        boolean queued = machine.queuedTargets() != 0;
        put(QUEUED, queued);
        if (queued) {
            Vec3 target = machine.queuedTarget(0);
            put(TARGET_X, target.x);
            put(TARGET_Y, target.y);
            put(TARGET_Z, target.z);
        }
    }

    protected static double distance(BlockPos pos, double x, double y, double z) {
        return Math.sqrt(
                Math.pow(pos.getX() - x, 2)
                        + Math.pow(pos.getY() - y, 2)
                        + Math.pow(pos.getZ() - z, 2));
    }

    @LuaFunction
    public final Object[] getCurrentTarget() {
        return read(
                s ->
                        s.booleanAt(QUEUED)
                                ? new Object[] {
                                    s.doubleAt(TARGET_X), s.doubleAt(TARGET_Y), s.doubleAt(TARGET_Z)
                                }
                                : new Object[] {null, "Index: 0, Size: 0"});
    }

    @LuaFunction
    public final Object[] getTargetDistance(double x, double y, double z) {
        return read(
                s ->
                        new Object[] {
                            Math.sqrt(
                                    Math.pow(s.intAt(CORE_X) - x, 2)
                                            + Math.pow(s.intAt(CORE_Y) - y, 2)
                                            + Math.pow(s.intAt(CORE_Z) - z, 2))
                        });
    }
}
