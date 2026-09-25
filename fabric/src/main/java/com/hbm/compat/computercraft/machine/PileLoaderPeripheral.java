// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.machine;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.tileentity.machine.pile.BlockEntityPileLoader;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.item.ItemStack;

public final class PileLoaderPeripheral extends SnapshotPeripheral<BlockEntityPileLoader> {
    private static final int TEMP = 0;
    private static final int DEPLETION = 1;
    private static final int LIFETIME = 2;
    private static final int TYPE = 3;
    private static final int LOADING_TYPE = 4;
    private static final int LOADING = 5;

    public PileLoaderPeripheral(BlockEntityPileLoader machine) {
        super(machine, "ntm_pile_loader", 6, 0);
    }

    @Override
    protected void capture(BlockEntityPileLoader machine) {
        ItemStack channel = machine.channelStack;
        ItemStack staged = machine.getItem(0);
        put(TEMP, machine.channelTemp);

        put(DEPLETION, ItemPileRodMK2.getDepletion(channel));
        put(LIFETIME, channel.getItem() instanceof ItemPileRodMK2 rod ? rod.type.life : 0D);
        put(TYPE, channel.getItem() instanceof ItemPileRodMK2 rod ? rod.type.ordinal() : -1);
        put(LOADING_TYPE, staged.getItem() instanceof ItemPileRodMK2 rod ? rod.type.ordinal() : -1);
        put(LOADING, machine.loading);
    }

    @LuaFunction
    public final Object[] getTemp() {
        return read(s -> new Object[] {s.doubleAt(TEMP)});
    }

    @LuaFunction
    public final Object[] getDepletion() {
        return read(s -> new Object[] {s.doubleAt(DEPLETION)});
    }

    @LuaFunction
    public final Object[] getLifetime() {
        return read(s -> new Object[] {s.doubleAt(LIFETIME)});
    }

    @LuaFunction("getType")
    public final Object[] rodType() {
        return read(s -> new Object[] {s.intAt(TYPE)});
    }

    @LuaFunction
    public final Object[] getLoadingType() {
        return read(s -> new Object[] {s.intAt(LOADING_TYPE)});
    }

    @LuaFunction
    public final Object[] isLoading() {
        return read(s -> new Object[] {s.booleanAt(LOADING)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] load() {
        machine().startLoading();
        return new Object[] {};
    }
}
