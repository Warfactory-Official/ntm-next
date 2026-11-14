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

package com.hbm.capability.port;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.fluidmk2.FluidHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

public final class PortDomain<D> {

    private static final Map<Class<?>, PortDomain<? extends PortView>> BY_CAPABILITY =
            new ConcurrentHashMap<>();

    public static final PortDomain<ItemPort> ITEM = of("item");
    public static final PortDomain<FluidPort> FLUID = of("fluid", FluidHandler.class);
    public static final PortDomain<EnergyPort> ENERGY = of("energy", EnergyHandler.class);

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
