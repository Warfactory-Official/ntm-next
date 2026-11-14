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
import java.lang.invoke.MutableCallSite;

public final class GenerationTracking {
    private final MethodHandle inactive;
    private final MutableCallSite site;
    private final MutableCallSite[] sites;
    private final MethodHandle invoker;
    private final MethodHandle active;
    private int users;

    public GenerationTracking(MethodHandle active) {
        assert active.type().returnType() == void.class;
        this.active = active;
        this.inactive = MethodHandles.empty(active.type());
        this.site = new MutableCallSite(inactive);
        this.sites = new MutableCallSite[] {site};
        this.invoker = site.dynamicInvoker();
    }

    public MethodHandle invoker() {
        return invoker;
    }

    public synchronized void acquire() {
        if (users++ == 0) {
            site.setTarget(active);
            MutableCallSite.syncAll(sites);
        }
    }

    public synchronized void release() {
        assert users > 0;
        if (--users == 0) {
            site.setTarget(inactive);
            MutableCallSite.syncAll(sites);
        }
    }
}
