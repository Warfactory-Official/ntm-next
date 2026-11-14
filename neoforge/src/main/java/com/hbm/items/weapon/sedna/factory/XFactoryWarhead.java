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

package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class XFactoryWarhead {
    public static BulletConfig warhead_turbine;

    public static final float TURBINE_BLADE_DAMAGE = 125F;

    private XFactoryWarhead() {}

    public static void init() {
        warhead_turbine =
                new BulletConfig()
                        .setVel(1F)
                        .setGrav(0D)
                        .setLife(200)
                        .setSpread(0F)
                        .setRicochetCount(0)
                        .setupDamageClass(ModDamageTypes.PHYSICAL)
                        .setOnImpact(
                                (blade, mop) -> {
                                    if (!(mop instanceof BlockHitResult hit)) return;
                                    BlockState state =
                                            blade.level().getBlockState(hit.getBlockPos());
                                    if (state.getDestroySpeed(blade.level(), hit.getBlockPos())
                                            <= 120F) {
                                        blade.level().destroyBlock(hit.getBlockPos(), false);
                                    }
                                });
    }
}
