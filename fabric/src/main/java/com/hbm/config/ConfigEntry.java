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

public record ConfigEntry<T>(
        Domain domain,
        String section,
        Kind kind,
        String key,
        String comment,
        T defaultValue,
        double min,
        double max) {

    public static ConfigEntry<Boolean> bool(
            Domain domain, String section, String key, boolean def, String comment) {
        return new ConfigEntry<>(domain, section, Kind.BOOL, key, comment, def, 0, 0);
    }

    public static ConfigEntry<Integer> integer(
            Domain domain, String section, String key, int def, int min, int max, String comment) {
        return new ConfigEntry<>(domain, section, Kind.INT, key, comment, def, min, max);
    }

    public static ConfigEntry<Double> real(
            Domain domain,
            String section,
            String key,
            double def,
            double min,
            double max,
            String comment) {
        return new ConfigEntry<>(domain, section, Kind.DOUBLE, key, comment, def, min, max);
    }

    public static ConfigEntry<String> text(
            Domain domain, String section, String key, String def, String comment) {
        return new ConfigEntry<>(domain, section, Kind.STRING, key, comment, def, 0, 0);
    }

    public int intMin() {
        return (int) min;
    }

    public int intMax() {
        return (int) max;
    }

    public enum Domain {
        CONTENT,
        RUNTIME,
        CLIENT
    }

    public enum Kind {
        BOOL,
        INT,
        DOUBLE,
        STRING
    }
}
