// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.api.entity;

import net.minecraft.world.damagesource.DamageSource;

public interface IResistanceProvider {

    float[] getCurrentDTDR(DamageSource damage, float amount, float pierceDT, float pierce);

    void onDamageDealt(DamageSource damage, float amount);
}
