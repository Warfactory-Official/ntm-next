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

package com.hbm.handler.resistance;

import com.hbm.lib.ModDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.jspecify.annotations.Nullable;

public enum DamageCategory {
    EXPL,
    FIRE,
    PHYS,
    EN;

    private final String translationKey = "damage.category." + name();

    public String translationKey() {
        return translationKey;
    }

    public static @Nullable DamageCategory of(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) return EXPL;
        if (source.is(DamageTypeTags.IS_FIRE)) return FIRE;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) return PHYS;
        if (source.is(ModDamageTypes.LASER)
                || source.is(ModDamageTypes.PLASMA)
                || source.is(ModDamageTypes.MICROWAVE)
                || source.is(ModDamageTypes.SUBATOMIC)
                || source.is(ModDamageTypes.ELECTRIC)) {
            return EN;
        }
        if (source.is(DamageTypes.CACTUS) || source.is(ModDamageTypes.SPIKES)) return PHYS;
        if (source.is(ModDamageTypes.ELECTRICITY)) return EN;
        return source.getEntity() != null || source.getDirectEntity() != null ? PHYS : null;
    }
}
