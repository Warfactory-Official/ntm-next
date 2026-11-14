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

package com.hbm.compat.computercraft.rbmk;

import com.hbm.blockentity.machine.rbmk.ColumnSnapshot;
import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKBoilerBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKCoolerBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKHeaterBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKIrradiationColumnBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKManualControlBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKStorageBlockEntity;
import com.hbm.blockentity.machine.rbmk.RodGroup;
import com.hbm.compat.computercraft.FluidKeys;
import com.hbm.compat.computercraft.LuaEnums;
import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.RBMKRodItem;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class RBMKControlRoomPeripheral extends SnapshotPeripheral<RBMKConsoleBlockEntity> {

    private static final int CENTER_X = 0;
    private static final int CENTER_Y = 1;
    private static final int CENTER_Z = 2;
    private static final int RADIUS = 7;
    private static final int STORAGE_SLOTS = 12;
    private static final String[][] STORAGE_KEYS = new String[STORAGE_SLOTS][];
    private static final String NO_RODS = "No control rods found";
    private static final Object[] DONE = new Object[0];

    static {
        for (var k = 0; k < STORAGE_SLOTS; k++) {
            var slot = "slot" + k;
            STORAGE_KEYS[k] =
                    new String[] {
                        slot + "coreSkinTemp",
                        slot + "coreTemp",
                        slot + "enrichment",
                        slot + "xenon",
                        slot + "rodName"
                    };
        }
    }

    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public RBMKControlRoomPeripheral(RBMKConsoleBlockEntity machine) {
        super(machine, "rbmk_console", 3, 0);
    }

    @Override
    protected void capture(RBMKConsoleBlockEntity machine) {
        put(CENTER_X, machine.targetX());
        put(CENTER_Y, machine.targetY());
        put(CENTER_Z, machine.targetZ());
    }

    @LuaFunction
    public Object[] getRBMKPos() {
        return read(
                s -> {
                    int x = s.intAt(CENTER_X), y = s.intAt(CENTER_Y), z = s.intAt(CENTER_Z);
                    if (x == 0 && y == 0 && z == 0) return new Object[] {null};
                    Map<String, Object> center = new HashMap<>(4);
                    center.put("rbmkCenterX", x);
                    center.put("rbmkCenterY", y);
                    center.put("rbmkCenterZ", z);
                    return new Object[] {center};
                });
    }

    private @Nullable RBMKBaseBlockEntity columnAt(RBMKConsoleBlockEntity console, int dx, int dz) {
        cursor.set(console.targetX() + dx, console.targetY(), console.targetZ() + dz);
        return ChunkUtil.blockEntityIfLoaded(console.getLevel(), cursor)
                        instanceof RBMKBaseBlockEntity column
                ? column
                : null;
    }

    private static int scanIndex(int rotation, int dx, int dz) {
        int i, j;
        switch (rotation) {
            case 1 -> {
                i = dz;
                j = -dx;
            }
            case 2 -> {
                i = -dx;
                j = -dz;
            }
            case 3 -> {
                i = -dz;
                j = dx;
            }
            default -> {
                i = dx;
                j = dz;
            }
        }
        return (j + RADIUS) * (2 * RADIUS + 1) + i + RADIUS;
    }

    @LuaFunction(mainThread = true)
    public Object[] getColumnData(int gx, int gy) {
        if (gx < 0 || gx > 2 * RADIUS || gy < 0 || gy > 2 * RADIUS) return new Object[] {null};
        var console = machine();
        int dx = gx - RADIUS, dz = RADIUS - gy;
        var column = columnAt(console, dx, dz);
        if (column == null) return new Object[] {null};
        var scan = console.columns[scanIndex(console.rotation(), dx, dz)];
        if (scan == null) return OpenComputers.unknownError();

        Map<String, Object> data = new HashMap<>();
        data.put("type", LuaEnums.columnKind(column.getConsoleType()));
        data.put("hullTemp", scan.heat);
        data.put("realSimWater", column.reasimWater);
        data.put("realSimSteam", column.reasimSteam);
        data.put("moderated", scan.moderated);
        var control = scan instanceof ColumnSnapshot.Control projection ? projection : null;
        data.put("level", control != null ? control.extraction : 0D);
        data.put("targetLevel", column instanceof RBMKControlBlockEntity rod ? rod.target : 0D);
        data.put(
                "color",
                control != null && column instanceof RBMKManualControlBlockEntity
                        ? control.dye < 0 ? -1 : LuaEnums.groupIndex(RodGroup.VALUES[control.dye])
                        : 0);
        var fuel = scan instanceof ColumnSnapshot.Fuel projection ? projection : null;
        data.put("enrichment", fuel != null ? fuel.enrichment : 0D);
        data.put("xenon", fuel != null ? fuel.xenon : 0D);
        data.put("coreSkinTemp", fuel != null ? fuel.hullHeat : 0D);
        data.put("coreTemp", fuel != null ? fuel.coreHeat : 0D);
        data.put("coreMaxTemp", fuel != null ? fuel.hullMeltingPoint : 0D);

        switch (column) {
            case RBMKControlAutoBlockEntity auto -> {
                data.put("function", LuaEnums.rodFunction(auto.function));
                data.put("heatUpper", auto.heatUpper);
                data.put("heatLower", auto.heatLower);
                data.put("levelUpper", auto.levelUpper);
                data.put("levelLower", auto.levelLower);
            }
            case RBMKRodBlockEntity rod -> {
                data.put("fluxQuantity", rod.lastFluxQuantity);
                data.put("fluxRatio", rod.fluxFastRatio);
                data.put("rodName", itemName(rod.getItem(0)));
            }
            case RBMKBoilerBlockEntity boiler -> {
                data.put("water", boiler.feed.getFill());
                data.put("steam", boiler.steam.getFill());
                data.put(
                        "steamType",
                        fluidKey(scan instanceof ColumnSnapshot.Boiler b ? b.steamFluid : -1));
            }
            case RBMKIrradiationColumnBlockEntity outgasser -> {
                data.put("fluxProgress", outgasser.progress);
                data.put("requiredFlux", outgasser.threshold);
                var input = outgasser.getItem(RBMKIrradiationColumnBlockEntity.SLOT_INPUT);
                data.put("craftingName", itemName(input));
                data.put("craftingNumber", input.getCount());
            }
            case RBMKHeaterBlockEntity heater -> {
                data.put("coolant", heater.feed.getFill());
                data.put("hotcoolant", heater.steam.getFill());
                var types = scan instanceof ColumnSnapshot.Heater h ? h : null;
                data.put("coldtype", fluidKey(types != null ? types.feedFluid : -1));
                data.put("hottype", fluidKey(types != null ? types.productFluid : -1));
            }
            case RBMKCoolerBlockEntity cooler -> data.put("cryogel", cooler.coolant.getFill());
            case RBMKStorageBlockEntity storage -> {
                for (var k = 0; k < STORAGE_SLOTS; k++) {
                    var stack = storage.getItem(k);
                    if (!(stack.getItem() instanceof RBMKRodItem)) continue;
                    var keys = STORAGE_KEYS[k];
                    data.put(keys[0], RBMKRodItem.getHullHeat(stack));
                    data.put(keys[1], RBMKRodItem.getCoreHeat(stack));
                    data.put(keys[2], RBMKRodItem.getEnrichment(stack));
                    data.put(keys[3], RBMKRodItem.getPoisonLevel(stack));
                    data.put(keys[4], itemName(stack));
                }
            }
            default -> {}
        }
        return new Object[] {data};
    }

    private static String itemName(ItemStack stack) {
        return stack.isEmpty() ? "" : stack.getItem().getDescriptionId();
    }

    private static String fluidKey(int registryId) {
        return FluidKeys.of(registryId < 0 ? null : BuiltInRegistries.FLUID.byId(registryId));
    }

    private int retargetSquare(
            RBMKConsoleBlockEntity console, double level, @Nullable RodGroup group) {
        var found = 0;
        for (var dz = -RADIUS; dz <= RADIUS; dz++) {
            for (var dx = -RADIUS; dx <= RADIUS; dx++) {
                if (columnAt(console, dx, dz) instanceof RBMKManualControlBlockEntity rod
                        && (group == null || rod.group == group)) {
                    rod.setTarget(level);
                    found++;
                }
            }
        }
        return found;
    }

    private static String noRodAt(int gx, int gy) {
        return "No control rod found at " + gx + "," + gy;
    }

    private static String noColor(int color) {
        return "Color " + color + " does not exist";
    }

    @LuaFunction(mainThread = true)
    public Object[] setLevel(double level) {
        return retargetSquare(machine(), level, null) > 0 ? DONE : new Object[] {NO_RODS};
    }

    @LuaFunction(mainThread = true)
    public Object[] setColumnLevel(int gx, int gy, double level) {
        if (!(columnAt(machine(), gx - RADIUS, RADIUS - gy)
                instanceof RBMKManualControlBlockEntity rod)) {
            return new Object[] {noRodAt(gx, gy)};
        }
        rod.setTarget(level);
        return DONE;
    }

    @LuaFunction(mainThread = true)
    public Object[] setColorLevel(int color, double level) {
        var group = LuaEnums.group(color);
        if (group == null) return new Object[] {noColor(color)};
        return retargetSquare(machine(), level, group) > 0
                ? DONE
                : new Object[] {"No rods for color " + color + " found"};
    }

    @LuaFunction(mainThread = true)
    public Object[] setColor(int gx, int gy, int color) {
        var group = LuaEnums.group(color);
        if (group == null) return new Object[] {noColor(color)};
        if (!(columnAt(machine(), gx - RADIUS, RADIUS - gy)
                instanceof RBMKManualControlBlockEntity rod)) {
            return new Object[] {noRodAt(gx, gy)};
        }
        rod.setGroup(group);
        return DONE;
    }

    @LuaFunction(mainThread = true)
    public Object[] pressAZ5() {
        var console = machine();
        console.getLevel()
                .playSound(
                        null,
                        console.getBlockPos(),
                        ModSounds.RBMK_SHUTDOWN.get(),
                        SoundSource.BLOCKS,
                        1F,
                        1F);
        return retargetSquare(console, 0D, null) > 0 ? DONE : new Object[] {NO_RODS};
    }
}
