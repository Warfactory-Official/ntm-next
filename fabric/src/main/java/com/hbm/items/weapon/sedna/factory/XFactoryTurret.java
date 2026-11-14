/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.items.weapon.sedna.factory;

import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.items.AmmoTypes.Ammo240Shell;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;
import net.minecraft.world.phys.Vec3;

public class XFactoryTurret {

    public static BulletConfig dgk_normal;

    public static Casing.Template CASINNG240MM =
            Casing.template(CasingShape.BOTTLENECK)
                    .scale(7.5F)
                    .bounce(0.02F, 0.05F)
                    .tints(Casing.BRASS);
    public static BulletConfig shell_normal;
    public static BulletConfig shell_explosive;
    public static BulletConfig shell_ap;
    public static BulletConfig shell_du;
    public static BulletConfig shell_w9;

    public static void init() {
        dgk_normal = new BulletConfig().setItem(() -> ModItems.AMMO_DGK.get());

        shell_normal =
                new BulletConfig()
                        .setItem(() -> ModItems.AMMO_SHELL.get(Ammo240Shell.STOCK))
                        .setDamage(1F)
                        .setCasing(CASINNG240MM.copy().register("240standard"))
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Lego.standardExplode(bullet, mop, 10F);
                                    bullet.discard();
                                });
        shell_explosive =
                new BulletConfig()
                        .setItem(() -> ModItems.AMMO_SHELL.get(Ammo240Shell.EXPLOSIVE))
                        .setDamage(1.5F)
                        .setCasing(CASINNG240MM.copy().register("240ext"))
                        .setOnImpact(
                                (bullet, mop) -> {
                                    Vec3 hit = mop.getLocation();
                                    ComposableExplosion vnt =
                                            new ComposableExplosion(
                                                    bullet.level(), hit.x, hit.y, hit.z, 10F);
                                    vnt.blockSelector(new AttenuatedRayBlockSelector());
                                    vnt.blockProcessor(CellDestruction.decay());
                                    vnt.entityProcessor(
                                            new WeaponExplosionEntityProcessor(1, bullet.damage));
                                    vnt.effects(new SmallExplosionEffect(10, 2.5F, 1F));
                                    vnt.explode();
                                    bullet.discard();
                                });
        shell_ap =
                new BulletConfig()
                        .setItem(() -> ModItems.AMMO_SHELL.get(Ammo240Shell.APFSDS_T))
                        .setDamage(2F)
                        .setDoesPenetrate(true)
                        .setCasing(CASINNG240MM.copy().register("240w"));
        shell_du =
                new BulletConfig()
                        .setItem(() -> ModItems.AMMO_SHELL.get(Ammo240Shell.APFSDS_DU))
                        .setDamage(2.5F)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setCasing(CASINNG240MM.copy().register("240u"));
        shell_w9 =
                new BulletConfig()
                        .setItem(() -> ModItems.AMMO_SHELL.get(Ammo240Shell.W9))
                        .setDamage(2.5F)
                        .setCasing(CASINNG240MM.copy().register("240n"))
                        .setOnImpact(XFactoryCatapult.LAMBDA_NUKE_STANDARD);
    }
}
