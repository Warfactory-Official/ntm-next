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
import java.lang.invoke.MethodType;
import java.util.Locale;
import org.slf4j.LoggerFactory;

public final class VectorApi {

    private static final String MODULE = "jdk.incubator.vector";

    public static final boolean AVAILABLE = bootstrap();

    private VectorApi() {}

    public static VectorBackend backend() {
        return Holder.BACKEND;
    }

    private static VectorBackend resolveBackend() {
        if (!AVAILABLE) return VectorBackend.NONE;
        String forced = System.getProperty("hbm.vector.backend");
        if (forced != null) {
            VectorBackend backend = VectorBackend.valueOf(forced.toUpperCase(Locale.ROOT));
            LoggerFactory.getLogger("NTM").info("Vector backend forced to {}", backend);
            return backend;
        }
        VectorBackend backend = VectorBackend.detect();
        LoggerFactory.getLogger("NTM")
                .info(
                        "Vector backend {} on {} {}",
                        backend,
                        System.getProperty("java.vm.name"),
                        System.getProperty("java.vm.version"));
        return backend;
    }

    private static final class Holder {
        static final VectorBackend BACKEND = resolveBackend();
    }

    private static boolean bootstrap() {
        if (!Boolean.parseBoolean(System.getProperty("hbm.vector", "true"))) {
            LoggerFactory.getLogger("NTM")
                    .info("Vector API disabled by hbm.vector; using scalar kernels");
            return false;
        }
        try {
            Module self = VectorApi.class.getModule();
            Module vector = ModuleLayer.boot().findModule(MODULE).orElse(null);
            if (vector == null) {
                MethodHandle loadModule =
                        UnsafeBootstrap.IMPL_LOOKUP.findStatic(
                                Class.forName("jdk.internal.module.Modules"),
                                "loadModule",
                                MethodType.methodType(Module.class, String.class));
                vector = (Module) loadModule.invokeExact(MODULE);
            }
            if (vector == null) return false;

            if (!self.canRead(vector)) {
                MethodHandle addReads =
                        UnsafeBootstrap.IMPL_LOOKUP.findStatic(
                                Class.forName("jdk.internal.module.Modules"),
                                "addReads",
                                MethodType.methodType(void.class, Module.class, Module.class));
                addReads.invokeExact(self, vector);
            }

            Class.forName(
                    "jdk.incubator.vector.DoubleVector", false, VectorApi.class.getClassLoader());
            boolean ok = self.canRead(vector);
            LoggerFactory.getLogger("NTM")
                    .info("Vector API {}", ok ? "resolved" : "unreadable; using scalar kernels");
            return ok;
        } catch (Throwable t) {
            LoggerFactory.getLogger("NTM")
                    .info("Vector API unavailable ({}); using scalar kernels", t.toString());
            return false;
        }
    }
}
