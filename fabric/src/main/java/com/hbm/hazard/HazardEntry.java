// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard;

import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.type.IHazardType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class HazardEntry implements Cloneable {

    public final IHazardType type;
    public final double baseLevel;

    private @Nullable List<IHazardModifier> mods;

    public HazardEntry(IHazardType type) {
        this(type, 1D);
    }

    public HazardEntry(IHazardType type, double level) {
        this.type = type;
        this.baseLevel = level;
    }

    public HazardEntry addMod(IHazardModifier mod) {
        if (mods == null) mods = new ArrayList<>(1);
        mods.add(mod);
        return this;
    }

    public List<IHazardModifier> getMods() {
        return mods == null ? List.of() : mods;
    }

    public void applyHazard(ItemStack stack, LivingEntity entity) {
        type.onUpdate(
                entity,
                IHazardModifier.evalAllModifiers(stack, entity, baseLevel, getMods()),
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
