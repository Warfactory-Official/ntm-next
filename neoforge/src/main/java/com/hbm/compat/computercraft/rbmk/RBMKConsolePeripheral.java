// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKCooler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKStorage;
import com.hbm.tileentity.machine.rbmk.RBMKColor;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.util.ChunkUtil;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RBMKConsolePeripheral extends SnapshotPeripheral<BlockEntityRBMKConsole> {

    private static final int TARGET_X = 0;
    private static final int TARGET_Y = 1;
    private static final int TARGET_Z = 2;

    public RBMKConsolePeripheral(BlockEntityRBMKConsole machine) {
        super(machine, "rbmk_console", 3, 0);
    }

    @Override
    protected void capture(BlockEntityRBMKConsole machine) {
        put(TARGET_X, machine.targetX());
        put(TARGET_Y, machine.targetY());
        put(TARGET_Z, machine.targetZ());
    }

    private static String unlocalized(int registryId) {
        return "hbmfluid."
                + NTMFluids.legacyName(
                                registryId < 0 ? null : BuiltInRegistries.FLUID.byId(registryId))
                        .toLowerCase(Locale.US);
    }

    private static String itemName(ItemStack stack) {

        return stack.isEmpty() ? "" : stack.getItem().getDescriptionId();
    }

    private BlockEntity columnAt(int dx, int dz) {
        BlockEntityRBMKConsole console = machine();
        return ChunkUtil.blockEntityIfLoaded(
                console.getLevel(),
                new BlockPos(console.targetX() + dx, console.targetY(), console.targetZ() + dz));
    }

    @LuaFunction(mainThread = true)
    public final Object[] getColumnData(int gridX, int gridY) {
        BlockEntityRBMKConsole console = machine();
        int x = gridX - 7;
        int y = -gridY + 7;
        int i = x;
        int j = y;
        switch (console.rotation()) {
            case 1 -> {
                i = y;
                j = -x;
            }
            case 2 -> {
                i = -x;
                j = -y;
            }
            case 3 -> {
                i = -y;
                j = x;
            }
            default -> {}
        }
        int index = (j + 7) * 15 + (i + 7);

        if (!(columnAt(x, y) instanceof BlockEntityRBMKBase column)) return new Object[] {null};

        RBMKColumn cached =
                index >= 0 && index < console.columns.length ? console.columns[index] : null;
        if (cached == null) return OpenComputers.unknownError();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", column.getConsoleType().name());
        data.put("hullTemp", cached.heat);
        data.put("realSimWater", column.reasimWater);
        data.put("realSimSteam", column.reasimSteam);
        data.put("moderated", cached.moderated);
        data.put("level", cached instanceof RBMKColumn.ControlColumn control ? control.level : 0D);

        data.put(
                "targetLevel", column instanceof BlockEntityRBMKControl rod ? rod.targetLevel : 0D);
        data.put(
                "color",
                (short)
                        (column instanceof BlockEntityRBMKControlManual
                                        && cached instanceof RBMKColumn.ControlColumn control
                                ? control.color
                                : 0));
        RBMKColumn.FuelColumn fuel = cached instanceof RBMKColumn.FuelColumn f ? f : null;
        data.put("enrichment", fuel == null ? 0D : fuel.enrichment);
        data.put("xenon", fuel == null ? 0D : fuel.xenon);
        data.put("coreSkinTemp", fuel == null ? 0D : fuel.c_heat);
        data.put("coreTemp", fuel == null ? 0D : fuel.c_coreHeat);
        data.put("coreMaxTemp", fuel == null ? 0D : fuel.c_maxHeat);

        if (column instanceof BlockEntityRBMKControlAuto auto) {
            data.put("function", auto.function.toString());
            data.put("heatUpper", auto.heatUpper);
            data.put("heatLower", auto.heatLower);
            data.put("levelUpper", auto.levelUpper);
            data.put("levelLower", auto.levelLower);
        }
        if (column instanceof BlockEntityRBMKRod rod) {
            data.put("fluxQuantity", rod.lastFluxQuantity);
            data.put("fluxRatio", rod.fluxFastRatio);
            data.put("rodName", itemName(rod.getItem(0)));
        }
        if (column instanceof BlockEntityRBMKBoiler boiler) {
            data.put("water", boiler.feed.getFill());
            data.put("steam", boiler.steam.getFill());
            data.put(
                    "steamType",
                    unlocalized(cached instanceof RBMKColumn.BoilerColumn b ? b.steamType : -1));
        }
        if (column instanceof BlockEntityRBMKOutgasser outgasser) {
            data.put("fluxProgress", outgasser.progress);
            data.put("requiredFlux", outgasser.duration);
            ItemStack input = outgasser.getItem(BlockEntityRBMKOutgasser.SLOT_INPUT);
            data.put("craftingName", itemName(input));
            data.put("craftingNumber", input.isEmpty() ? 0 : input.getCount());
        }
        if (column instanceof BlockEntityRBMKHeater heater) {
            RBMKColumn.HeaterColumn h = cached instanceof RBMKColumn.HeaterColumn c ? c : null;
            data.put("coolant", heater.feed.getFill());
            data.put("hotcoolant", heater.steam.getFill());
            data.put("coldtype", unlocalized(h == null ? -1 : h.coldType));
            data.put("hottype", unlocalized(h == null ? -1 : h.hotType));
        }
        if (column instanceof BlockEntityRBMKCooler cooler) {
            data.put("cryogel", cooler.cold.getFill());
        }
        if (column instanceof BlockEntityRBMKStorage storage) {
            for (int k = 0; k < 12; k++) {
                ItemStack loaded = storage.getItem(k);
                if (loaded.getItem() instanceof ItemRBMKRod) {
                    data.put("slot" + k + "coreSkinTemp", ItemRBMKRod.getHullHeat(loaded));
                    data.put("slot" + k + "coreTemp", ItemRBMKRod.getCoreHeat(loaded));
                    data.put("slot" + k + "enrichment", ItemRBMKRod.getEnrichment(loaded));
                    data.put("slot" + k + "xenon", ItemRBMKRod.getPoisonLevel(loaded));
                    data.put("slot" + k + "rodName", loaded.getItem().getDescriptionId());
                }
            }
        }
        return new Object[] {data};
    }

    @LuaFunction
    public final Object[] getRBMKPos() {
        return read(
                s -> {
                    if (s.intAt(TARGET_X) == 0 && s.intAt(TARGET_Y) == 0 && s.intAt(TARGET_Z) == 0)
                        return new Object[] {null};
                    Map<String, Integer> data = new LinkedHashMap<>();
                    data.put("rbmkCenterX", s.intAt(TARGET_X));
                    data.put("rbmkCenterY", s.intAt(TARGET_Y));
                    data.put("rbmkCenterZ", s.intAt(TARGET_Z));
                    return new Object[] {data};
                });
    }

    private static boolean retarget(BlockEntity be, double level) {
        if (!(be instanceof BlockEntityRBMKControlManual rod)) return false;
        rod.setTarget(Math.min(1, Math.max(0, level)));
        rod.setChanged();
        return true;
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double level) {
        boolean found = false;
        for (int i = -7; i <= 7; i++) {
            for (int j = -7; j <= 7; j++) {
                found |= retarget(columnAt(i, j), level);
            }
        }
        return found ? new Object[] {} : new Object[] {"No control rods found"};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setColumnLevel(int gridX, int gridY, double level) {
        int x = gridX - 7;
        int y = -gridY + 7;
        if (retarget(columnAt(x, y), level)) return new Object[] {};
        return new Object[] {"No control rod found at " + (x + 7) + "," + (7 - y)};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setColorLevel(int color, double level) {
        if (color < 0 || color > 4) return new Object[] {"Color " + color + " does not exist"};
        boolean found = false;
        for (int i = -7; i <= 7; i++) {
            for (int j = -7; j <= 7; j++) {
                if (columnAt(i, j) instanceof BlockEntityRBMKControlManual rod
                        && rod.color == RBMKColor.VALUES[color]) {
                    found |= retarget(rod, level);
                }
            }
        }
        return found ? new Object[] {} : new Object[] {"No rods for color " + color + " found"};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setColor(int gridX, int gridY, int color) {
        if (color < 0 || color > 4) return new Object[] {"Color " + color + " does not exist"};
        int x = gridX - 7;
        int y = -gridY + 7;
        if (!(columnAt(x, y) instanceof BlockEntityRBMKControlManual rod)) {
            return new Object[] {"No control rod found at " + (x + 7) + "," + (7 - y)};
        }
        rod.color = RBMKColor.VALUES[color];
        rod.setChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] pressAZ5() {
        BlockEntityRBMKConsole console = machine();
        BlockPos pos = console.getBlockPos();
        console.getLevel()
                .playSound(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        ModSounds.RBMK_SHUTDOWN.get(),
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F);
        boolean found = false;
        for (int i = -7; i <= 7; i++) {
            for (int j = -7; j <= 7; j++) {
                found |= retarget(columnAt(i, j), 0);
            }
        }
        return found ? new Object[] {} : new Object[] {"No control rods found"};
    }
}
