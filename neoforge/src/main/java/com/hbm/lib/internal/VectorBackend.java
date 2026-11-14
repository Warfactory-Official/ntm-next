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
