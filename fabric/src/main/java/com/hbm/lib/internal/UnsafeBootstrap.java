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

package com.hbm.lib.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;

final class UnsafeBootstrap {
    static final MethodHandles.Lookup IMPL_LOOKUP = lookup();

    static {
        exportJavaBasePackage("jdk.internal.misc", UnsafeBootstrap.class.getModule());
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

    static void exportJavaBasePackage(String packageName, Module target) {
        try {
            MethodHandle implAddExports =
                    IMPL_LOOKUP.findVirtual(
                            Module.class,
                            "implAddExports",
                            MethodType.methodType(void.class, String.class, Module.class));
            implAddExports.invokeExact(Object.class.getModule(), packageName, target);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to export " + packageName + " to " + target, t);
        }
    }
}
