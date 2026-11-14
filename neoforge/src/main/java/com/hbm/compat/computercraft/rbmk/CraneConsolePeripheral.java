/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.compat.computercraft.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKCraneConsoleBlockEntity;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.items.machine.RBMKRodItem;
import dan200.computercraft.api.lua.LuaFunction;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

public final class CraneConsolePeripheral extends SnapshotPeripheral<RBMKCraneConsoleBlockEntity> {

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

    public CraneConsolePeripheral(RBMKCraneConsoleBlockEntity machine) {
        super(machine, "rbmk_crane", 8, 1);
    }

    @Override
    protected void capture(RBMKCraneConsoleBlockEntity machine) {
        ItemStack rod = machine.getItem(0);
        boolean loaded = rod.getItem() instanceof RBMKRodItem;
        put(HAS_ROD, loaded);
        if (loaded) {
            put(SKIN, RBMKRodItem.getHullHeat(rod));
            put(CORE, RBMKRodItem.getCoreHeat(rod));
            put(ENRICHMENT, RBMKRodItem.getEnrichment(rod));
            put(XENON_LEVEL, RBMKRodItem.getPoisonLevel(rod));
            put(XENON, RBMKRodItem.getPoison(rod));
            putRef(ROD_NAME, rod.getItem().getDescriptionId());
        }
        put(CRANE_X, machine.hookX());
        put(CRANE_Z, machine.hookZ());
    }

    @LuaFunction(mainThread = true)
    public Object[] move(String direction) {
        RBMKCraneConsoleBlockEntity machine = machine();
        if (!machine.rigged) return new Object[] {"Crane not found"};
        switch (direction) {
            case "up" -> {
                machine.tiltFront = 30;
                machine.hbm$syncSet$RBMKCraneConsoleBlockEntity$hookFront(
                        (double) (machine.hookFront + (SPEED)));
            }
            case "down" -> {
                machine.tiltFront = -30;
                machine.hbm$syncSet$RBMKCraneConsoleBlockEntity$hookFront(
                        (double) (machine.hookFront - (SPEED)));
            }
            case "left" -> {
                machine.tiltLeft = 30;
                machine.hbm$syncSet$RBMKCraneConsoleBlockEntity$hookLeft(
                        (double) (machine.hookLeft + (SPEED)));
            }
            case "right" -> {
                machine.tiltLeft = -30;
                machine.hbm$syncSet$RBMKCraneConsoleBlockEntity$hookLeft(
                        (double) (machine.hookLeft - (SPEED)));
            }
            default -> {}
        }
        machine.markChanged();
        return new Object[] {};
    }

    @LuaFunction(mainThread = true)
    public Object[] load() {
        RBMKCraneConsoleBlockEntity machine = machine();
        if (!machine.rigged) return new Object[] {"Crane not found"};
        machine.requestLift();
        return new Object[] {};
    }

    @LuaFunction
    public Object[] getRodInfo() {
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
    public Object[] getDepletion() {
        return read(
                s -> new Object[] {s.booleanAt(HAS_ROD) ? (Object) s.doubleAt(ENRICHMENT) : "N/A"});
    }

    @LuaFunction
    public Object[] getXenonPoison() {
        return read(s -> new Object[] {s.booleanAt(HAS_ROD) ? (Object) s.doubleAt(XENON) : "N/A"});
    }

    @LuaFunction
    public Object[] getCranePos() {
        return read(s -> new Object[] {s.intAt(CRANE_X), s.intAt(CRANE_Z)});
    }
}
