// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class RBMKOutgasserPeripheral extends RBMKColumnPeripheral<BlockEntityRBMKOutgasser> {

    private static final int GAS = SLOTS;
    private static final int GAS_MAX = SLOTS + 1;
    private static final int PROGRESS = SLOTS + 2;
    private static final int GAS_ID = SLOTS + 3;
    private static final int INPUT_SIZE = SLOTS + 4;
    private static final int GAS_NAME = 0;
    private static final int INPUT_NAME = 1;

    public RBMKOutgasserPeripheral(BlockEntityRBMKOutgasser machine) {
        super(machine, "rbmk_outgasser", SLOTS + 5, 2);
    }

    @Override
    protected void capture(BlockEntityRBMKOutgasser machine) {
        super.capture(machine);
        put(GAS, machine.gas.getFill());
        put(GAS_MAX, machine.gas.getMaxFill());
        put(PROGRESS, machine.progress);
        put(GAS_ID, NTMFluids.legacyId(machine.gas.getTankType()));
        putRef(GAS_NAME, NTMFluids.legacyName(machine.gas.getTankType()));
        ItemStack input = machine.getItem(BlockEntityRBMKOutgasser.SLOT_INPUT);

        putRef(INPUT_NAME, input.isEmpty() ? "" : input.getItem().getDescriptionId());
        put(INPUT_SIZE, input.isEmpty() ? 0 : input.getCount());
    }

    @LuaFunction
    public final Object[] getGas() {
        return read(s -> new Object[] {s.intAt(GAS)});
    }

    @LuaFunction
    public final Object[] getGasMax() {
        return read(s -> new Object[] {s.intAt(GAS_MAX)});
    }

    @LuaFunction
    public final Object[] getGasType() {
        return read(s -> new Object[] {s.refAt(GAS_NAME)});
    }

    @LuaFunction
    public final Object[] getProgress() {
        return read(s -> new Object[] {s.doubleAt(PROGRESS)});
    }

    @LuaFunction
    public final Object[] getCrafting() {
        return read(s -> new Object[] {s.refAt(INPUT_NAME), s.intAt(INPUT_SIZE)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(GAS),
                            s.intAt(GAS_MAX),
                            s.doubleAt(PROGRESS),
                            s.intAt(GAS_ID),
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z),
                            s.refAt(INPUT_NAME),
                            s.intAt(INPUT_SIZE)
                        });
    }
}
