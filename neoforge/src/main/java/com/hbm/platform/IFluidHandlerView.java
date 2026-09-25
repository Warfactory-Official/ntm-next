// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import net.minecraft.world.level.material.Fluid;

public interface IFluidHandlerView {

    Object handlerIdentity();

    long extractable(Fluid fluid);

    long extract(Fluid fluid, long maxMb);

    long insertable(Fluid fluid);

    long insert(Fluid fluid, long maxMb);
}
