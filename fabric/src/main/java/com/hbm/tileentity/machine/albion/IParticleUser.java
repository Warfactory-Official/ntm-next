// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.albion;

import com.hbm.tileentity.machine.albion.BlockEntityPASource.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

public interface IParticleUser {

    boolean canParticleEnter(Particle particle, Direction dir, BlockPos pos);

    void onEnter(Particle particle, Direction dir);

    @Nullable BlockPos getExitPos(Particle particle);
}
