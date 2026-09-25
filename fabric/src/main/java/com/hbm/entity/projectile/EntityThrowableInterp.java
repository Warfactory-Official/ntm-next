// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;

public abstract class EntityThrowableInterp extends EntityThrowableNT {

    private final InterpolationHandler interpolation =
            new InterpolationHandler(this, 3 + approachNum());

    public EntityThrowableInterp(EntityType<? extends EntityThrowableInterp> type, Level level) {
        super(type, level);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            super.tick();
        } else {

            this.interpolation.interpolate();
            this.baseTick();
        }
    }

    public int approachNum() {
        return 0;
    }
}
