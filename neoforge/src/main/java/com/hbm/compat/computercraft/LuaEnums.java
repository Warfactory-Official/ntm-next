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

package com.hbm.compat.computercraft;

import com.hbm.blockentity.machine.ControlCurve;
import com.hbm.blockentity.machine.albion.PASourceBlockEntity.PAState;
import com.hbm.blockentity.machine.rbmk.RBMKColumnType;
import com.hbm.blockentity.machine.rbmk.RodGroup;
import com.hbm.items.weapon.MissileItem.MissileTier;
import com.hbm.uninos.ConnectionPriority;
import org.jspecify.annotations.Nullable;

public final class LuaEnums {

    private static final String[] DIRECTIONS = {"north", "east", "south", "west"};
    private static final ConnectionPriority[] PRIORITIES = {
        ConnectionPriority.LOWEST,
        ConnectionPriority.LOW,
        ConnectionPriority.NORMAL,
        ConnectionPriority.HIGH,
        ConnectionPriority.HIGHEST
    };
    private static final ConnectionPriority[] STORAGE_PRIORITIES = {
        ConnectionPriority.LOW, ConnectionPriority.NORMAL, ConnectionPriority.HIGH
    };
    private static final ControlCurve[] CURVES = {
        ControlCurve.LINEAR, ControlCurve.RISING, ControlCurve.FALLING
    };
    private static final RodGroup[] GROUPS = {
        RodGroup.RED, RodGroup.YELLOW, RodGroup.GREEN, RodGroup.BLUE, RodGroup.PURPLE
    };

    private LuaEnums() {}

    public static String direction(int code) {
        return code > 0 && code < DIRECTIONS.length ? DIRECTIONS[code] : DIRECTIONS[0];
    }

    public static int directionCode(String name) {
        for (var code = 0; code < DIRECTIONS.length; code++) {
            if (DIRECTIONS[code].equals(name)) return code;
        }
        return -1;
    }

    public static String acceleratorState(PAState state) {
        return switch (state) {
            case IDLE -> "IDLE";
            case RUNNING -> "RUNNING";
            case SUCCESS -> "SUCCESS";
            case PAUSE_UNLOADED -> "PAUSE_UNLOADED";
            case CRASH_DEFOCUS -> "CRASH_DEFOCUS";
            case CRASH_DERAIL -> "CRASH_DERAIL";
            case CRASH_CANNOT_ENTER -> "CRASH_CANNOT_ENTER";
            case CRASH_NOCOOL -> "CRASH_NOCOOL";
            case CRASH_NOPOWER -> "CRASH_NOPOWER";
            case CRASH_NOCOIL -> "CRASH_NOCOIL";
            case CRASH_OVERSPEED -> "CRASH_OVERSPEED";
            case CRASH_UNDERSPEED -> "CRASH_UNDERSPEED";
            case CRASH_NORECIPE -> "CRASH_NORECIPE";
        };
    }

    public static String priority(ConnectionPriority tier) {
        return switch (tier) {
            case LOWEST -> "LOWEST";
            case LOW -> "LOW";
            case NORMAL -> "NORMAL";
            case HIGH -> "HIGH";
            case HIGHEST -> "HIGHEST";
        };
    }

    public static @Nullable ConnectionPriority priority(int index) {
        return index >= 0 && index < PRIORITIES.length ? PRIORITIES[index] : null;
    }

    public static int storagePriority(ConnectionPriority tier) {
        return switch (tier) {
            case LOWEST -> -1;
            case LOW -> 0;
            case NORMAL -> 1;
            case HIGH -> 2;
            case HIGHEST -> 3;
        };
    }

    public static @Nullable ConnectionPriority storagePriority(int index) {
        return index >= 0 && index < STORAGE_PRIORITIES.length ? STORAGE_PRIORITIES[index] : null;
    }

    public static String columnKind(RBMKColumnType type) {
        return switch (type) {
            case BLANK -> "BLANK";
            case FUEL -> "FUEL";
            case FUEL_SIM -> "FUEL_SIM";
            case CONTROL -> "CONTROL";
            case CONTROL_AUTO -> "CONTROL_AUTO";
            case BOILER -> "BOILER";
            case MODERATOR -> "MODERATOR";
            case ABSORBER -> "ABSORBER";
            case REFLECTOR -> "REFLECTOR";
            case OUTGASSER -> "OUTGASSER";
            case BREEDER -> "BREEDER";
            case STORAGE -> "STORAGE";
            case COOLER -> "COOLER";
            case HEATEX -> "HEATEX";
        };
    }

    public static String rodFunction(ControlCurve curve) {
        return switch (curve) {
            case LINEAR -> "LINEAR";
            case RISING -> "QUAD_UP";
            case FALLING -> "QUAD_DOWN";
        };
    }

    public static int curveIndex(ControlCurve curve) {
        return switch (curve) {
            case LINEAR -> 0;
            case RISING -> 1;
            case FALLING -> 2;
        };
    }

    public static ControlCurve curve(int index) {
        return CURVES[index];
    }

    public static int groupIndex(RodGroup group) {
        return switch (group) {
            case RED -> 0;
            case YELLOW -> 1;
            case GREEN -> 2;
            case BLUE -> 3;
            case PURPLE -> 4;
        };
    }

    public static @Nullable RodGroup group(int index) {
        return index >= 0 && index < GROUPS.length ? GROUPS[index] : null;
    }

    public static int missileTier(MissileTier tier) {
        return switch (tier) {
            case TIER0 -> 0;
            case TIER1 -> 1;
            case TIER2 -> 2;
            case TIER3 -> 3;
            case TIER4 -> 4;
        };
    }
}
