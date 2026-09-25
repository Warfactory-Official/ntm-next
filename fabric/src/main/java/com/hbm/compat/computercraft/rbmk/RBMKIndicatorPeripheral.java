// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator.IndicatorUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKIndicator;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class RBMKIndicatorPeripheral extends SnapshotPeripheral<BlockEntityRBMKIndicator> {

    private static final int N = BlockEntityRBMKIndicator.INDICATORS;
    private static final String INVALID = "Invalid index (1-6)";
    private static final int ACTIVE = 0;
    private static final int POLLING = 1;
    private static final int LIGHT = 2;
    private static final int COLOR = 3;
    private static final int MIN = 4;
    private static final int MAX = 5;
    private static final int STRIDE = 6;
    private static final int LABEL = 0;
    private static final int CHANNEL = 1;

    public RBMKIndicatorPeripheral(BlockEntityRBMKIndicator machine) {
        super(machine, "rbmk_indicator", N * STRIDE, N * 2);
    }

    @Override
    protected void capture(BlockEntityRBMKIndicator machine) {
        for (int u = 0; u < N; u++) {
            IndicatorUnit indicator = machine.indicators[u];
            int b = u * STRIDE;
            put(b + ACTIVE, indicator.active);
            put(b + POLLING, indicator.polling);
            put(b + LIGHT, indicator.light);
            put(b + COLOR, indicator.color);
            put(b + MIN, indicator.min);
            put(b + MAX, indicator.max);
            putRef(u * 2 + LABEL, indicator.label);
            putRef(u * 2 + CHANNEL, indicator.rtty);
        }
    }

    @LuaFunction
    public final Object[] getIndicatorInfo(int index) {
        int idx = index - 1;
        if (idx < 0 || idx >= N) return new Object[] {null, INVALID};
        return read(
                s -> {
                    int b = idx * STRIDE;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("active", s.booleanAt(b + ACTIVE));
                    map.put("polling", s.booleanAt(b + POLLING));
                    map.put("light", s.booleanAt(b + LIGHT));
                    map.put("color", s.intAt(b + COLOR));
                    map.put("label", s.refAt(idx * 2 + LABEL));
                    map.put("channel", s.refAt(idx * 2 + CHANNEL));
                    map.put("min", s.longAt(b + MIN));
                    map.put("max", s.longAt(b + MAX));
                    return new Object[] {map};
                });
    }

    private Object[] set(int index, Consumer<IndicatorUnit> change) {
        int idx = index - 1;
        if (idx < 0 || idx >= N) return new Object[] {false, INVALID};
        BlockEntityRBMKIndicator machine = machine();
        change.accept(machine.indicators[idx]);
        machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setIndicatorActive(int index, boolean active) {
        return set(index, i -> i.active = active);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setIndicatorLight(int index, boolean light) {
        return set(index, i -> i.light = light);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setIndicatorColor(int index, int color) {
        return set(index, i -> i.color = Math.clamp(color, 0, 0xffffff));
    }

    @LuaFunction(mainThread = true)
    public final Object[] setIndicatorLabel(int index, String label) {
        return set(index, i -> i.label = label);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setIndicatorBounds(int index, int min, int max) {
        return set(
                index,
                i -> {
                    i.min = min;
                    i.max = max;
                });
    }
}
