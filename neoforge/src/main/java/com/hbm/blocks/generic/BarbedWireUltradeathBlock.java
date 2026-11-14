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

package com.hbm.blocks.generic;

import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.HbmPotion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class BarbedWireUltradeathBlock extends BarbedWireBlock {
    private static final float DAMAGE = 5.0F;

    public BarbedWireUltradeathBlock(Properties props) {
        super(props);
    }

    @Override
    protected void sting(Level level, Entity entity) {
        if (level instanceof ServerLevel server) {
            entity.hurtServer(
                    server, server.damageSources().source(ModDamageTypes.PINK_CLOUD), DAMAGE);
        }
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), EFFECT_TICKS, 9));
        }
    }
}
