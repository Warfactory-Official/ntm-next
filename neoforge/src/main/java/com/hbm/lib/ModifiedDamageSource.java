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

package com.hbm.lib;

import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public class ModifiedDamageSource extends DamageSource {
    public final boolean bypassCooldown;
    public final double knockbackMultiplier;
    public final float pierceDT;
    public final float pierceDR;

    protected ModifiedDamageSource(
            Holder<DamageType> type, @Nullable Entity direct, @Nullable Entity causing) {
        super(type, direct, causing);
        bypassCooldown = false;
        knockbackMultiplier = Double.NaN;
        pierceDT = 0F;
        pierceDR = 0F;
    }

    protected ModifiedDamageSource(
            DamageSource source,
            boolean bypassCooldown,
            double knockbackMultiplier,
            float pierceDT,
            float pierceDR) {
        super(
                source.typeHolder(),
                source.getDirectEntity(),
                source.getEntity(),
                source.sourcePositionRaw());
        this.knockbackMultiplier = knockbackMultiplier;
        this.bypassCooldown = bypassCooldown;
        this.pierceDT = pierceDT;
        this.pierceDR = pierceDR;
    }

    public static ModifiedDamageSource of(
            DamageSource source,
            boolean bypassCooldown,
            double knockbackMultiplier,
            float pierceDT,
            float pierceDR) {
        if (source instanceof ModifiedDamageSource modified) {
            return modified.withModifiers(bypassCooldown, knockbackMultiplier, pierceDT, pierceDR);
        }
        if (source.getClass() != DamageSource.class) {
            throw new IllegalArgumentException(
                    "Damage source must preserve its subclass when adding modifiers: "
                            + source.getClass());
        }
        return new ModifiedDamageSource(
                source, bypassCooldown, knockbackMultiplier, pierceDT, pierceDR);
    }

    public static ModifiedDamageSource bypassingCooldown(DamageSource source) {
        if (source instanceof ModifiedDamageSource modified) {
            return modified.withModifiers(
                    true, modified.knockbackMultiplier, modified.pierceDT, modified.pierceDR);
        }
        return of(source, true, Double.NaN, 0F, 0F);
    }

    protected ModifiedDamageSource withModifiers(
            boolean bypassCooldown, double knockbackMultiplier, float pierceDT, float pierceDR) {
        return new ModifiedDamageSource(
                this, bypassCooldown, knockbackMultiplier, pierceDT, pierceDR);
    }

    @Override
    public boolean is(TagKey<DamageType> tag) {
        return bypassCooldown && tag == DamageTypeTags.BYPASSES_COOLDOWN || super.is(tag);
    }
}
