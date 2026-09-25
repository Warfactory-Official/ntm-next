// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;

public enum VectorBackend {
    NONE(false, false, false, false, false, false),

    GRAAL(true, true, true, false, false, true),

    C2(true, true, true, true, true, true);

    private final boolean arithmetic;
    private final boolean masks;
    private final boolean converts;
    private final boolean gathers;
    private final boolean transcendentals;
    private final boolean fma;

    VectorBackend(
            boolean arithmetic,
            boolean masks,
            boolean converts,
            boolean gathers,
            boolean transcendentals,
            boolean fma) {
        this.arithmetic = arithmetic;
        this.masks = masks;
        this.converts = converts;
        this.gathers = gathers;
        this.transcendentals = transcendentals;
        this.fma = fma;
    }

    public boolean masks() {
        return masks;
    }

    public boolean arithmetic() {
        return arithmetic;
    }

    public boolean converts() {
        return converts;
    }

    public boolean gathers() {
        return gathers;
    }

    public boolean transcendentals() {
        return transcendentals;
    }

    public boolean fma() {
        return fma && FusedMath.AVAILABLE;
    }

    boolean claimsFma() {
        return fma;
    }

    static VectorBackend detect() {
        try {
            HotSpotDiagnosticMXBean bean =
                    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            return Boolean.parseBoolean(bean.getVMOption("UseJVMCICompiler").getValue())
                    ? GRAAL
                    : C2;
        } catch (IllegalArgumentException e) {

            return C2;
        } catch (RuntimeException | LinkageError e) {

            return GRAAL;
        }
    }
}
