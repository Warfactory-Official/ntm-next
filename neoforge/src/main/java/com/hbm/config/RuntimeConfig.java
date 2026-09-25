// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.config;

public interface RuntimeConfig {

    ConfigStore store();

    default int conveyorCramMax() {
        return store().get(ConfigSchema.CONVEYOR_CRAM_MAX);
    }

    default boolean conveyorCramExplode() {
        return store().get(ConfigSchema.CONVEYOR_CRAM_EXPLODE);
    }

    default boolean ultraLarpMode() {
        return store().get(ConfigSchema.ULTRA_LARP_MODE);
    }

    default boolean damageCompatibilityMode() {
        return store().get(ConfigSchema.DAMAGE_COMPATIBILITY_MODE);
    }

    default int itemHazardDropTickrate() {
        return store().get(ConfigSchema.ITEM_HAZARD_DROP_TICKRATE);
    }

    default boolean unstableRecoverySweep() {
        return store().get(ConfigSchema.UNSTABLE_RECOVERY_SWEEP);
    }

    default boolean extendedLogging() {
        return store().get(ConfigSchema.ENABLE_EXTENDED_LOGGING);
    }

    default boolean enableBomberShortMode() {
        return store().get(ConfigSchema.ENABLE_BOMBER_SHORT_MODE);
    }

    default double foreignExportReserve() {
        return store().get(ConfigSchema.FOREIGN_EXPORT_RESERVE);
    }

    default boolean waypointDebug() {
        return store().get(ConfigSchema.WAYPOINT_DEBUG);
    }

    default int toolRecursionDepth() {
        return store().get(ConfigSchema.TOOL_RECURSION_DEPTH);
    }
}
