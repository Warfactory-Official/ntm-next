// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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

    static ConfigEntry<Boolean> bool(
            Domain domain, String section, String key, boolean def, String comment) {
        return new ConfigEntry<>(domain, section, Kind.BOOL, key, comment, def, 0, 0);
    }

    static ConfigEntry<Integer> integer(
            Domain domain, String section, String key, int def, int min, int max, String comment) {
        return new ConfigEntry<>(domain, section, Kind.INT, key, comment, def, min, max);
    }

    static ConfigEntry<Double> real(
            Domain domain,
            String section,
            String key,
            double def,
            double min,
            double max,
            String comment) {
        return new ConfigEntry<>(domain, section, Kind.DOUBLE, key, comment, def, min, max);
    }

    static ConfigEntry<String> text(
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
