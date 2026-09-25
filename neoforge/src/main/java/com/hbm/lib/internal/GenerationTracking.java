// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

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
