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

package com.hbm.items.weapon.sedna;

import com.hbm.lib.ModifiedDamageSource;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class DamageSourceSedna extends ModifiedDamageSource {

    public DamageSourceSedna(Holder<DamageType> type) {
        super(type, null, null);
    }

    public DamageSourceSedna(Holder<DamageType> type, Entity projectile, Entity shooter) {
        super(type, projectile, shooter);
    }

    private DamageSourceSedna(
            DamageSource source,
            boolean bypassCooldown,
            double knockbackMultiplier,
            float pierceDT,
            float pierceDR) {
        super(source, bypassCooldown, knockbackMultiplier, pierceDT, pierceDR);
    }

    @Override
    protected DamageSourceSedna withModifiers(
            boolean bypassCooldown, double knockbackMultiplier, float pierceDT, float pierceDR) {
        return new DamageSourceSedna(this, bypassCooldown, knockbackMultiplier, pierceDT, pierceDR);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity died) {
        Entity shooter = getEntity();
        String key = "death.sedna." + getMsgId();
        return shooter == null
                ? Component.translatable(key, died.getDisplayName())
                : Component.translatable(
                        key + ".attacker", died.getDisplayName(), shooter.getDisplayName());
    }
}
