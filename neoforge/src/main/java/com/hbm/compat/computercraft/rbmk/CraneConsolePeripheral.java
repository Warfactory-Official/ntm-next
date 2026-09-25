// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.compat.computercraft.rbmk;

import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

public final class CraneConsolePeripheral extends SnapshotPeripheral<BlockEntityCraneConsole> {

    private static final double SPEED = 0.05D;
    private static final int HAS_ROD = 0;
    private static final int SKIN = 1;
    private static final int CORE = 2;
    private static final int ENRICHMENT = 3;
    private static final int XENON_LEVEL = 4;
    private static final int XENON = 5;
    private static final int CRANE_X = 6;
    private static final int CRANE_Z = 7;
    private static final int ROD_NAME = 0;

    public CraneConsolePeripheral(BlockEntityCraneConsole machine) {
        super(machine, "rbmk_crane", 8, 1);
    }

    @Override
    protected void capture(BlockEntityCraneConsole machine) {
        ItemStack rod = machine.getItem(0);
        boolean loaded = rod.getItem() instanceof ItemRBMKRod;
        put(HAS_ROD, loaded);
        if (loaded) {
            put(SKIN, ItemRBMKRod.getHullHeat(rod));
            put(CORE, ItemRBMKRod.getCoreHeat(rod));
            put(ENRICHMENT, ItemRBMKRod.getEnrichment(rod));
            put(XENON_LEVEL, ItemRBMKRod.getPoisonLevel(rod));
            put(XENON, ItemRBMKRod.getPoison(rod));
            putRef(ROD_NAME, rod.getItem().getDescriptionId());
        }
        Direction dir = machine.coreDir();
        Direction left = dir.getCounterClockWise();
        put(
                CRANE_X,
                (int)
                        Math.floor(
                                machine.centerX
                                        - dir.getStepX() * machine.posFront
                                        - left.getStepX() * machine.posLeft
                                        + 0.5D));
        put(
                CRANE_Z,
                (int)
                        Math.floor(
                                machine.centerZ
                                        - dir.getStepZ() * machine.posFront
                                        - left.getStepZ() * machine.posLeft
                                        + 0.5D));
    }

    @LuaFunction(mainThread = true)
    public final Object[] move(String direction) {
        BlockEntityCraneConsole machine = machine();
        if (!machine.setUpCrane) return new Object[] {"Crane not found"};
        switch (direction) {
            case "up" -> {
                machine.tiltFront = 30;
                machine.posFront += SPEED;
            }
            case "down" -> {
                machine.tiltFront = -30;
                machine.posFront -= SPEED;
            }
            case "left" -> {
                machine.tiltLeft = 30;
                machine.posLeft += SPEED;
            }
            case "right" -> {
                machine.tiltLeft = -30;
                machine.posLeft -= SPEED;
            }
            default -> {}
        }
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public final Object[] load() {
        BlockEntityCraneConsole machine = machine();
        if (!machine.setUpCrane) return new Object[] {"Crane not found"};
        machine.requestLoad();
        return new Object[] {};
    }

    @LuaFunction
    public final Object[] getRodInfo() {
        return read(
                s -> {
                    if (!s.booleanAt(HAS_ROD)) return new Object[] {false, "No rod loaded"};
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("coreSkinTemp", s.doubleAt(SKIN));
                    map.put("coreTemp", s.doubleAt(CORE));
                    map.put("enrichment", s.doubleAt(ENRICHMENT));
                    map.put("xenon", s.doubleAt(XENON_LEVEL));
                    map.put("rodName", s.refAt(ROD_NAME));
                    return new Object[] {map};
                });
    }

    @LuaFunction
    public final Object[] getDepletion() {
        return read(
                s -> new Object[] {s.booleanAt(HAS_ROD) ? (Object) s.doubleAt(ENRICHMENT) : "N/A"});
    }

    @LuaFunction
    public final Object[] getXenonPoison() {
        return read(s -> new Object[] {s.booleanAt(HAS_ROD) ? (Object) s.doubleAt(XENON) : "N/A"});
    }

    @LuaFunction
    public final Object[] getCranePos() {
        return read(s -> new Object[] {s.intAt(CRANE_X), s.intAt(CRANE_Z)});
    }
}
