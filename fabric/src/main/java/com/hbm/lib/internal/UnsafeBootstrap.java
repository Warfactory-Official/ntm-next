// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

final class UnsafeBootstrap {
    static final MethodHandles.Lookup IMPL_LOOKUP = lookup();

    static {
        exportJavaBasePackage("jdk.internal.misc");
    }

    static final Unsafe U = Unsafe.getUnsafe();

    private UnsafeBootstrap() {}

    private static MethodHandles.Lookup lookup() {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
            Field lookup =
                    Class.forName("java.lang.invoke.MethodHandles$Lookup")
                            .getDeclaredField("IMPL_LOOKUP");
            return (MethodHandles.Lookup)
                    unsafe.getObject(
                            unsafe.staticFieldBase(lookup), unsafe.staticFieldOffset(lookup));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

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
