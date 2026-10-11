// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability;

import com.hbm.interfaces.injected.ForeignBridgeHost;
import java.util.IdentityHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public final class ForeignBridges {

    private @Nullable IdentityHashMap<Object, Object> fluid;
    private @Nullable IdentityHashMap<Object, Object> energy;
    private @Nullable Object items;

    public static ForeignBridges of(BlockEntity be) {
        return ((ForeignBridgeHost) be).hbm$bridges();
    }

    public @Nullable Object fluid(Object handler) {
        return fluid == null ? null : fluid.get(handler);
    }

    public void fluid(Object handler, Object staging) {
        if (fluid == null) fluid = new IdentityHashMap<>(2);
        fluid.put(handler, staging);
    }

    public @Nullable Object energy(Object handler) {
        return energy == null ? null : energy.get(handler);
    }

    public void energy(Object handler, Object staging) {
        if (energy == null) energy = new IdentityHashMap<>(2);
        energy.put(handler, staging);
    }

    public @Nullable Object items() {
        return items;
    }

    public void items(Object state) {
        items = state;
    }
}
