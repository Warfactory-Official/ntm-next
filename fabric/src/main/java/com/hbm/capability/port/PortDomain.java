// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.capability.port;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public final class PortDomain<D> {

    private static final Map<Class<?>, PortDomain<? extends PortView>> BY_CAPABILITY =
            new ConcurrentHashMap<>();

    public static final PortDomain<ItemPort> ITEM = of("item");
    public static final PortDomain<FluidPort> FLUID = of("fluid", IFluidHandlerMK2.class);
    public static final PortDomain<EnergyPort> ENERGY = of("energy", IEnergyHandlerMK2.class);

    private final String name;

    private PortDomain(String name) {
        this.name = name;
    }

    public static <D extends PortView> PortDomain<D> of(String name, Class<?>... capabilities) {
        PortDomain<D> domain = new PortDomain<>(name);
        for (Class<?> capability : capabilities) {

            PortDomain<? extends PortView> prev = BY_CAPABILITY.putIfAbsent(capability, domain);
            if (prev != null) {
                throw new IllegalStateException(
                        capability.getName()
                                + " is already claimed by domain "
                                + prev
                                + "; two domains cannot both answer for one capability");
            }
        }
        return domain;
    }

    public static @Nullable PortDomain<? extends PortView> forCapability(Class<?> type) {
        return BY_CAPABILITY.get(type);
    }

    @Override
    public String toString() {
        return name;
    }
}
