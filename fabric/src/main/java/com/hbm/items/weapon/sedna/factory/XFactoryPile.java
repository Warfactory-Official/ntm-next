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
import com.hbm.particle.helper.FlameCreator;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.Level;

public final class XFactoryPile {
    public static BulletConfig debris;

    private XFactoryPile() {}

    public static void init() {
        debris =
                new BulletConfig()
                        .setLife(200)
                        .setVel(1F)
                        .setGrav(0.1D)
                        .setOnUpdate(
                                bullet -> {
                                    if (bullet.level().isClientSide()
                                            && bullet.level()
                                                            .getNearestPlayer(
                                                                    bullet.getX(),
                                                                    bullet.getY(),
                                                                    bullet.getZ(),
                                                                    100D,
                                                                    EntitySelector
                                                                            .ENTITY_STILL_ALIVE)
                                                    != null)
                                        FlameCreator.composeEffectClient(
                                                bullet.level(),
                                                bullet.getX(),
                                                bullet.getY() - 0.125D,
                                                bullet.getZ(),
                                                FlameCreator.META_FIRE);
                                })
                        .setOnImpact(
                                (bullet, hit) -> {
                                    bullet.level()
                                            .explode(
                                                    bullet,
                                                    bullet.getX(),
                                                    bullet.getY(),
                                                    bullet.getZ(),
                                                    5F,
                                                    true,
                                                    Level.ExplosionInteraction.NONE);
                                    bullet.discard();
                                });
    }
}
