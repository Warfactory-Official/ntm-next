// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal;

import com.hbm.lib.internal.natives.NativeLibrary;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;
import org.slf4j.LoggerFactory;

public final class FusedMath {

    public static final boolean AVAILABLE = detect();

    private FusedMath() {}

    private static boolean detect() {
        boolean fma = readUseFma();
        if (!fma && !NativeLibrary.AVAILABLE) {
            LoggerFactory.getLogger("NTM")
                    .warn(
                            "No hardware FMA and no native kernel bound; Math.fma takes its "
                                    + "BigDecimal fallback and the Java fused kernels run slow");
        }
        return fma;
    }

    private static boolean readUseFma() {
        try {
            HotSpotDiagnosticMXBean bean =
                    ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            return Boolean.parseBoolean(bean.getVMOption("UseFMA").getValue());
        } catch (IllegalArgumentException e) {

            return false;
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
