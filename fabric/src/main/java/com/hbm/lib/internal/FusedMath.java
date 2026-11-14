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
import org.slf4j.LoggerFactory;

public final class FusedMath {

    public static final boolean AVAILABLE = detect();

    private FusedMath() {}

    private static boolean detect() {
        boolean fma = readUseFma();
        if (!fma) {
            LoggerFactory.getLogger("NTM")
                    .warn(
                            "No hardware FMA; Math.fma takes its BigDecimal fallback and the "
                                    + "Java fused kernels run slow");
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
