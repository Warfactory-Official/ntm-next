// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.bomb;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.tileentity.bomb.BlockEntityLaunchPadBase;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public final class LaunchPadPeripheral extends SnapshotPeripheral<BlockEntityLaunchPadBase> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int FUEL = 2;
    private static final int FUEL_MAX = 3;
    private static final int OXIDIZER = 4;
    private static final int OXIDIZER_MAX = 5;
    private static final int CAN_LAUNCH = 6;
    private static final int TIER = 7;
    private static final int X = 8;
    private static final int Y = 9;
    private static final int Z = 10;
    private static final int FUEL_NAME = 0;
    private static final int OXIDIZER_NAME = 1;

    public LaunchPadPeripheral(BlockEntityLaunchPadBase machine) {
        super(machine, "ntm_launch_pad", 11, 2);
    }

    @Override
    protected void capture(BlockEntityLaunchPadBase machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(FUEL, machine.fuelTank.getFill());
        put(FUEL_MAX, machine.fuelTank.getMaxFill());
        put(OXIDIZER, machine.oxidizerTank.getFill());
        put(OXIDIZER_MAX, machine.oxidizerTank.getMaxFill());
        putRef(
                FUEL_NAME,
                "hbmfluid."
                        + NTMFluids.legacyName(machine.fuelTank.getTankType())
                                .toLowerCase(Locale.US));
        putRef(
                OXIDIZER_NAME,
                "hbmfluid."
                        + NTMFluids.legacyName(machine.oxidizerTank.getTankType())
                                .toLowerCase(Locale.US));
        put(CAN_LAUNCH, machine.canLaunch());
        ItemStack missile = machine.getItem(BlockEntityLaunchPadBase.SLOT_MISSILE);
        put(
                TIER,
                machine.isMissileValid(missile) && missile.getItem() instanceof ItemMissile item
                        ? item.tier.ordinal()
                        : -1);
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FUEL),
                            s.intAt(FUEL_MAX),
                            s.refAt(FUEL_NAME),
                            s.intAt(OXIDIZER),
                            s.intAt(OXIDIZER_MAX),
                            s.refAt(OXIDIZER_NAME)
                        });
    }

    @LuaFunction
    public final Object[] canLaunch() {
        return read(s -> new Object[] {s.booleanAt(CAN_LAUNCH)});
    }

    @LuaFunction
    public final Object[] getTier() {
        return read(s -> s.intAt(TIER) < 0 ? new Object[] {} : new Object[] {s.intAt(TIER)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] launch(int x, int z) {
        BlockEntityLaunchPadBase machine = machine();
        if (!machine.canLaunch()) return new Object[] {false};
        return new Object[] {machine.sendCommandPosition(x, -1, z)};
    }

    @LuaFunction
    public final Object[] getPos() {
        return read(s -> new Object[] {s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
