// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal;

import com.hbm.platform.Services;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import jdk.internal.misc.Unsafe;

final class UnsafeBootstrap {
    static final MethodHandles.Lookup IMPL_LOOKUP = Services.TRUSTED_LOOKUP.implLookup();

    static {
        exportJavaBasePackage("jdk.internal.misc");
    }

    static final Unsafe U = Unsafe.getUnsafe();

    private UnsafeBootstrap() {}

    private static void exportJavaBasePackage(String packageName) {
        try {
            MethodHandle implAddExports =
                    IMPL_LOOKUP.findVirtual(
                            Module.class,
                            "implAddExports",
                            MethodType.methodType(void.class, String.class, Module.class));
            implAddExports.invokeExact(
                    Object.class.getModule(), packageName, UnsafeBootstrap.class.getModule());
        } catch (Throwable t) {
            throw new RuntimeException(
                    "Failed to export " + packageName + " to " + UnsafeBootstrap.class.getModule(),
                    t);
        }
    }
}
