// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityVortex extends EntityBlackHole {

    private float shrinkRate = 0.0025F;

    public EntityVortex(EntityType<? extends EntityVortex> type, Level level) {
        super(type, level);
    }

    public EntityVortex(Level level) {
        this(ModEntities.VORTEX.get(), level);
    }

    public EntityVortex(Level level, float size) {
        this(level);
        setSize(size);
    }

    public EntityVortex setShrinkRate(float shrinkRate) {
        this.shrinkRate = shrinkRate;
        return this;
    }

    @Override
    public void tick() {

        if (level() instanceof ServerLevel) {
            setSize(getSize() - shrinkRate);
            if (getSize() <= 0F) {
                discard();
                return;
            }
        }
        super.tick();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        shrinkRate = input.getFloatOr("shrinkRate", 0.0025F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("shrinkRate", shrinkRate);
    }
}
