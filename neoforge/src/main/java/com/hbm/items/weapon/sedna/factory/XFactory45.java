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

import com.hbm.items.variant.CasingType;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.particle.casing.Casing;
import com.hbm.particle.casing.CasingShape;

public class XFactory45 {

    public static BulletConfig p45_sp;
    public static BulletConfig p45_fmj;
    public static BulletConfig p45_jhp;
    public static BulletConfig p45_ap;
    public static BulletConfig p45_du;

    public static void init() {
        Casing.Template casing9 =
                Casing.template(CasingShape.STRAIGHT).tints(Casing.BRASS).scale(1F, 1F, 0.75F);
        p45_sp =
                new BulletConfig()
                        .setItem(EnumAmmo.P45_SP)
                        .setCasing(CasingType.SMALL, 8)
                        .setCasing(casing9.copy().register("p45"));
        p45_fmj =
                new BulletConfig()
                        .setItem(EnumAmmo.P45_FMJ)
                        .setCasing(CasingType.SMALL, 8)
                        .setDamage(0.8F)
                        .setThresholdNegation(2F)
                        .setArmorPiercing(0.1F)
                        .setCasing(casing9.copy().register("p45fmj"));
        p45_jhp =
                new BulletConfig()
                        .setItem(EnumAmmo.P45_JHP)
                        .setCasing(CasingType.SMALL, 8)
                        .setDamage(1.5F)
                        .setHeadshot(1.5F)
                        .setArmorPiercing(-0.25F)
                        .setCasing(casing9.copy().register("p45jhp"));
        p45_ap =
                new BulletConfig()
                        .setItem(EnumAmmo.P45_AP)
                        .setCasing(CasingType.SMALL_STEEL, 8)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(1.25F)
                        .setThresholdNegation(5F)
                        .setArmorPiercing(0.15F)
                        .setCasing(casing9.copy().tints(Casing.MAGNUM_44).register("p45ap"));
        p45_du =
                new BulletConfig()
                        .setItem(EnumAmmo.P45_DU)
                        .setCasing(CasingType.SMALL_STEEL, 8)
                        .setDoesPenetrate(true)
                        .setDamageFalloffByPen(false)
                        .setDamage(2.5F)
                        .setThresholdNegation(15F)
                        .setArmorPiercing(0.25F)
                        .setCasing(casing9.copy().tints(Casing.MAGNUM_44).register("p45du"));
    }
}
