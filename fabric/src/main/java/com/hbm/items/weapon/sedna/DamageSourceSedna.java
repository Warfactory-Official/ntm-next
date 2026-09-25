// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class DamageSourceSedna extends DamageSource {

    public DamageSourceSedna(Holder<DamageType> type) {
        super(type);
    }

    public DamageSourceSedna(Holder<DamageType> type, Entity projectile, Entity shooter) {
        super(type, projectile, shooter);
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
