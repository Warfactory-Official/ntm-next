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

    default int farTrackingRange() {
        return store().get(ConfigSchema.FAR_TRACKING_RANGE);
    }

    default int itemHazardDropTickrate() {
        return store().get(ConfigSchema.ITEM_HAZARD_DROP_TICKRATE);
    }

    default boolean unstableRecoverySweep() {
        return store().get(ConfigSchema.UNSTABLE_RECOVERY_SWEEP);
    }

    default boolean enableMOTD() {
        return store().get(ConfigSchema.ENABLE_MOTD);
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

    default int ciwsAccuracy() {
        return store().get(ConfigSchema.CIWS_ACCURACY);
    }

    default boolean waypointDebug() {
        return store().get(ConfigSchema.WAYPOINT_DEBUG);
    }

    default int toolRecursionDepth() {
        return store().get(ConfigSchema.TOOL_RECURSION_DEPTH);
    }
}
