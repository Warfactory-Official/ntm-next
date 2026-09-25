// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge.GaugeUnit;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class RBMKGaugePeripheral extends SnapshotPeripheral<BlockEntityRBMKGauge> {

    private static final int N = BlockEntityRBMKGauge.GAUGES;
    private static final String INVALID = "Invalid index (1-4)";
    private static final int ACTIVE = 0;
    private static final int POLLING = 1;
    private static final int COLOR = 2;
    private static final int MIN = 3;
    private static final int MAX = 4;
    private static final int VALUE = 5;
    private static final int STRIDE = 6;
    private static final int LABEL = 0;
    private static final int CHANNEL = 1;

    public RBMKGaugePeripheral(BlockEntityRBMKGauge machine) {
        super(machine, "rbmk_gauge", N * STRIDE, N * 2);
    }

    @Override
    protected void capture(BlockEntityRBMKGauge machine) {
        for (int u = 0; u < N; u++) {
            GaugeUnit gauge = machine.gauges[u];
            int b = u * STRIDE;
            put(b + ACTIVE, gauge.active);
            put(b + POLLING, gauge.polling);
            put(b + COLOR, gauge.color);
            put(b + MIN, gauge.min);
            put(b + MAX, gauge.max);
            put(b + VALUE, gauge.value);
            putRef(u * 2 + LABEL, gauge.label);
            putRef(u * 2 + CHANNEL, gauge.rtty);
        }
    }

    @LuaFunction
    public final Object[] getGaugeInfo(int index) {
        int idx = index - 1;
        if (idx < 0 || idx >= N) return new Object[] {null, INVALID};
        return read(
                s -> {
                    int b = idx * STRIDE;
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("active", s.booleanAt(b + ACTIVE));
                    map.put("polling", s.booleanAt(b + POLLING));
                    map.put("color", s.intAt(b + COLOR));
                    map.put("label", s.refAt(idx * 2 + LABEL));
                    map.put("channel", s.refAt(idx * 2 + CHANNEL));
                    map.put("min", s.longAt(b + MIN));
                    map.put("max", s.longAt(b + MAX));
                    map.put("value", s.longAt(b + VALUE));
                    return new Object[] {map};
                });
    }

    private Object[] set(int index, Consumer<GaugeUnit> change) {
        int idx = index - 1;
        if (idx < 0 || idx >= N) return new Object[] {false, INVALID};
        BlockEntityRBMKGauge machine = machine();
        change.accept(machine.gauges[idx]);
        machine.setChanged();
        return new Object[] {true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGaugeActive(int index, boolean active) {
        return set(index, g -> g.active = active);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGaugePolling(int index, boolean polling) {
        return set(index, g -> g.polling = polling);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGaugeColor(int index, int color) {
        return set(index, g -> g.color = Math.clamp(color, 0, 0xffffff));
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGaugeLabel(int index, String label) {
        return set(index, g -> g.label = label);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGaugeChannel(int index, String channel) {
        return set(index, g -> g.rtty = channel);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGaugeMin(int index, int min) {
        return set(index, g -> g.min = min);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGaugeMax(int index, int max) {
        return set(index, g -> g.max = max);
    }

    @LuaFunction(mainThread = true)
    public final Object[] setGaugeValue(int index, int value) {
        return set(index, g -> g.value = value);
    }
}
