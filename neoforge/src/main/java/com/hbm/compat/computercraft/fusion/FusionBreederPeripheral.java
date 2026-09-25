// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.fusion;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.tileentity.machine.fusion.BlockEntityFusionBreeder;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public final class FusionBreederPeripheral extends SnapshotPeripheral<BlockEntityFusionBreeder> {

    private static final int NEUTRON = 0;
    private static final int PROGRESS = 1;
    private static final int FILL_0 = 2;
    private static final int MAX_0 = 3;
    private static final int FILL_1 = 4;
    private static final int MAX_1 = 5;
    private static final int INPUT_SIZE = 6;
    private static final int OUTPUT_SIZE = 7;
    private static final int NAME_0 = 0;
    private static final int NAME_1 = 1;
    private static final int INPUT_NAME = 2;
    private static final int OUTPUT_NAME = 3;

    public FusionBreederPeripheral(BlockEntityFusionBreeder machine) {
        super(machine, "ntm_fusion_breeder", 8, 4);
    }

    @Override
    protected void capture(BlockEntityFusionBreeder machine) {
        put(NEUTRON, machine.neutronEnergySync);
        put(PROGRESS, machine.progress / BlockEntityFusionBreeder.CAPACITY);
        put(FILL_0, machine.tanks[0].getFill());
        put(MAX_0, machine.tanks[0].getMaxFill());
        put(FILL_1, machine.tanks[1].getFill());
        put(MAX_1, machine.tanks[1].getMaxFill());
        putRef(
                NAME_0,
                "hbmfluid."
                        + NTMFluids.legacyName(machine.tanks[0].getTankType())
                                .toLowerCase(Locale.US));
        putRef(
                NAME_1,
                "hbmfluid."
                        + NTMFluids.legacyName(machine.tanks[1].getTankType())
                                .toLowerCase(Locale.US));
        ItemStack input = machine.getItem(BlockEntityFusionBreeder.SLOT_INPUT);
        ItemStack output = machine.getItem(BlockEntityFusionBreeder.SLOT_OUTPUT);

        putRef(INPUT_NAME, input.isEmpty() ? "" : input.getItem().getDescriptionId());
        put(INPUT_SIZE, input.isEmpty() ? 0 : input.getCount());
        putRef(OUTPUT_NAME, output.isEmpty() ? "" : output.getItem().getDescriptionId());
        put(OUTPUT_SIZE, output.isEmpty() ? 0 : output.getCount());
    }

    @LuaFunction
    public final Object[] getNeutronEnergy() {
        return read(s -> new Object[] {s.doubleAt(NEUTRON)});
    }

    @LuaFunction
    public final Object[] getProgress() {
        return read(s -> new Object[] {s.doubleAt(PROGRESS)});
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(
                s ->
                        new Object[] {
                            s.intAt(FILL_0),
                            s.intAt(MAX_0),
                            s.refAt(NAME_0),
                            s.intAt(FILL_1),
                            s.intAt(MAX_1),
                            s.refAt(NAME_1)
                        });
    }

    @LuaFunction
    public final Object[] getCrafting() {
        return read(
                s ->
                        new Object[] {
                            s.refAt(INPUT_NAME),
                            s.intAt(INPUT_SIZE),
                            s.refAt(OUTPUT_NAME),
                            s.intAt(OUTPUT_SIZE)
                        });
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(NEUTRON),
                            s.doubleAt(PROGRESS),
                            s.intAt(FILL_0),
                            s.intAt(MAX_0),
                            s.refAt(NAME_0),
                            s.intAt(FILL_1),
                            s.intAt(MAX_1),
                            s.refAt(NAME_1),
                            s.refAt(INPUT_NAME),
                            s.intAt(INPUT_SIZE),
                            s.refAt(OUTPUT_NAME),
                            s.intAt(OUTPUT_SIZE)
                        });
    }
}
