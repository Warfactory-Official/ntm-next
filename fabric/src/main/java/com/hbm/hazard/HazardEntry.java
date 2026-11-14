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

package com.hbm.hazard;

import com.hbm.hazard.modifier.HazardModifier;
import com.hbm.hazard.type.HazardKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardEntry implements Cloneable {

    public final HazardKind type;
    public final double baseLevel;

    private @Nullable List<HazardModifier> mods;

    public HazardEntry(HazardKind type) {
        this(type, 1D);
    }

    public HazardEntry(HazardKind type, double level) {
        this.type = type;
        this.baseLevel = level;
    }

    public HazardEntry addMod(HazardModifier mod) {
        if (mods == null) mods = new ArrayList<>(1);
        mods.add(mod);
        return this;
    }

    public List<HazardModifier> getMods() {
        return mods == null ? List.of() : mods;
    }

    public void applyHazard(ItemStack stack, LivingEntity entity) {
        type.onUpdate(
                entity,
                (float) HazardModifier.evalAllModifiers(stack, entity, baseLevel, getMods()),
                stack);
    }

    @Override
    public HazardEntry clone() {
        try {
            return (HazardEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public HazardEntry clone(double mult) {
        HazardEntry clone = new HazardEntry(type, baseLevel * mult);
        clone.mods = this.mods;
        return clone;
    }
}
