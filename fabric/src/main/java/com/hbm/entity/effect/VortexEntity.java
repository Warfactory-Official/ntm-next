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

package com.hbm.entity.effect;

import com.hbm.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VortexEntity extends BlackHoleEntity {

    private float shrinkRate = 0.0025F;

    public VortexEntity(EntityType<? extends VortexEntity> type, Level level) {
        super(type, level);
    }

    public VortexEntity(Level level) {
        this(ModEntities.VORTEX.get(), level);
    }

    public VortexEntity(Level level, float size) {
        this(level);
        setSize(size);
    }

    public VortexEntity setShrinkRate(float shrinkRate) {
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
