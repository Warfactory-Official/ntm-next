// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class EntityQuasar extends EntityBlackHole {

    public EntityQuasar(EntityType<? extends EntityQuasar> type, Level level) {
        super(type, level);
    }

    public EntityQuasar(Level level, float size) {
        this(ModEntities.DIGAMMA_QUASAR.get(), level);
        setSize(size);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
