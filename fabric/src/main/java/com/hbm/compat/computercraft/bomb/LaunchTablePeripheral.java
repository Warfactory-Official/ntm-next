// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.bomb;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.bomb.BlockEntityLaunchTable;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public final class LaunchTablePeripheral extends SnapshotPeripheral<BlockEntityLaunchTable> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int FUEL = 2;
    private static final int FUEL_MAX = 3;
    private static final int OXIDIZER = 4;
    private static final int OXIDIZER_MAX = 5;
    private static final int SOLID = 6;
    private static final int CAN_LAUNCH = 7;
    private static final int MISSILE_VALID = 8;
    private static final int HAS_DESIGNATOR = 9;
    private static final int HAS_FUEL = 10;
    private static final int FUEL_NAME = 0;
    private static final int OXIDIZER_NAME = 1;

    public LaunchTablePeripheral(BlockEntityLaunchTable machine) {
        super(machine, "ntm_custom_launch_pad", 11, 2);
    }

    @Override
    protected void capture(BlockEntityLaunchTable machine) {
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
        put(SOLID, machine.solid);
        put(CAN_LAUNCH, machine.canLaunch());
        put(MISSILE_VALID, machine.isMissileValid());
        put(HAS_DESIGNATOR, machine.hasDesignator());
        put(HAS_FUEL, machine.hasFuel());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[] {s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getContents() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FUEL),
                            s.intAt(FUEL_MAX),
                            s.refAt(FUEL_NAME),
                            s.intAt(OXIDIZER),
                            s.intAt(OXIDIZER_MAX),
                            s.refAt(OXIDIZER_NAME),
                            s.intAt(SOLID),
                            BlockEntityLaunchTable.MAX_SOLID
                        });
    }

    @LuaFunction
    public final Object[] getLaunchInfo() {
        return read(
                s ->
                        new Object[] {
                            s.booleanAt(CAN_LAUNCH),
                            s.booleanAt(MISSILE_VALID),
                            s.booleanAt(HAS_DESIGNATOR),
                            s.booleanAt(HAS_FUEL)
                        });
    }

    @LuaFunction(mainThread = true)
    public final Object[] getCoords() {
        ItemStack designator = machine().getItem(BlockEntityLaunchTable.SLOT_DESIGNATOR);
        if (!(designator.getItem() instanceof IDesignatorItem))
            return new Object[] {false, "Designator not found"};
        Long target = designator.get(ModDataComponents.TARGET_DESIGNATOR.get());
        if (target == null) return new Object[] {false};
        return new Object[] {IDesignatorItem.unpackX(target), IDesignatorItem.unpackZ(target)};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setCoords(int x, int z) {
        BlockEntityLaunchTable machine = machine();
        ItemStack designator = machine.getItem(BlockEntityLaunchTable.SLOT_DESIGNATOR);
        if (!(designator.getItem() instanceof IDesignatorItem))
            return new Object[] {false, "Designator not found"};
        designator.set(ModDataComponents.TARGET_DESIGNATOR.get(), IDesignatorItem.pack(x, z));
        machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] launch() {
        BlockEntityLaunchTable machine = machine();
        if (!machine.canLaunch()) return new Object[] {false};
        machine.launchFromDesignator();
        return new Object[] {true};
    }
}
