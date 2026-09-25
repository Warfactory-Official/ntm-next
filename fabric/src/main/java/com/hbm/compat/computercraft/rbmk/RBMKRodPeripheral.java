// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.blocks.machine.rbmk.RBMKRod;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class RBMKRodPeripheral extends RBMKColumnPeripheral<BlockEntityRBMKRod> {

    private static final int FLUX_QUANTITY = SLOTS;
    private static final int FLUX_RATIO = SLOTS + 1;
    private static final int HAS_ROD = SLOTS + 2;
    private static final int ENRICHMENT = SLOTS + 3;
    private static final int POISON = SLOTS + 4;
    private static final int CORE_HEAT = SLOTS + 5;
    private static final int HULL_HEAT = SLOTS + 6;
    private static final int MODERATED = SLOTS + 7;
    private static final int ROD_TYPE = 0;

    public RBMKRodPeripheral(BlockEntityRBMKRod machine) {
        super(machine, "rbmk_fuel_rod", SLOTS + 8, 1);
    }

    @Override
    protected void capture(BlockEntityRBMKRod machine) {
        super.capture(machine);
        put(FLUX_QUANTITY, machine.lastFluxQuantity);
        put(FLUX_RATIO, machine.lastFluxRatio);
        put(
                MODERATED,
                machine.getBlockState().getBlock() instanceof RBMKRod channel && channel.moderated);
        ItemStack stack = machine.getItem(0);
        boolean fuelled = stack.getItem() instanceof ItemRBMKRod;
        put(HAS_ROD, fuelled);
        if (fuelled) {
            put(ENRICHMENT, ItemRBMKRod.getEnrichment(stack));
            put(POISON, ItemRBMKRod.getPoison(stack));
            put(CORE_HEAT, ItemRBMKRod.getCoreHeat(stack));
            put(HULL_HEAT, ItemRBMKRod.getHullHeat(stack));

            putRef(ROD_TYPE, stack.getItem().getDescriptionId());
        }
    }

    private static Object orNA(SnapshotPeripheral<?> s, int slot) {
        return s.booleanAt(HAS_ROD) ? (Object) s.doubleAt(slot) : "N/A";
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[] {s.doubleAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getFluxQuantity() {
        return read(s -> new Object[] {s.doubleAt(FLUX_QUANTITY)});
    }

    @LuaFunction
    public final Object[] getFluxRatio() {
        return read(s -> new Object[] {s.doubleAt(FLUX_RATIO)});
    }

    @LuaFunction
    public final Object[] getDepletion() {
        return read(s -> new Object[] {orNA(s, ENRICHMENT)});
    }

    @LuaFunction
    public final Object[] getXenonPoison() {
        return read(s -> new Object[] {orNA(s, POISON)});
    }

    @LuaFunction
    public final Object[] getCoreHeat() {
        return read(s -> new Object[] {orNA(s, CORE_HEAT)});
    }

    @LuaFunction
    public final Object[] getSkinHeat() {
        return read(s -> new Object[] {orNA(s, HULL_HEAT)});
    }

    @LuaFunction("getType")
    public final Object[] rodType() {
        return read(s -> new Object[] {s.booleanAt(HAS_ROD) ? s.refAt(ROD_TYPE) : "N/A"});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(
                s ->
                        new Object[] {
                            s.doubleAt(HEAT),
                            orNA(s, HULL_HEAT),
                            orNA(s, CORE_HEAT),
                            s.doubleAt(FLUX_QUANTITY),
                            s.doubleAt(FLUX_RATIO),
                            orNA(s, ENRICHMENT),
                            orNA(s, POISON),
                            s.booleanAt(HAS_ROD) ? s.refAt(ROD_TYPE) : "N/A",
                            s.booleanAt(MODERATED),
                            s.intAt(X),
                            s.intAt(Y),
                            s.intAt(Z)
                        });
    }

    @LuaFunction
    public final Object[] getModerated() {
        return read(s -> new Object[] {s.booleanAt(MODERATED)});
    }
}
