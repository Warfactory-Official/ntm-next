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

package com.hbm.items.weapon.grenade;

import com.hbm.entity.grenade.GrenadeUniversalEntity;
import com.hbm.items.weapon.grenade.GrenadeFillingItem;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GrenadeExtraItem extends Item {

    public final EnumGrenadeExtra type;

    public GrenadeExtraItem(Item.Properties properties, EnumGrenadeExtra type) {
        super(properties);
        this.type = type;
    }

    public static void updateTick(GrenadeUniversalEntity grenade) {
        EnumGrenadeExtra extra = grenade.getExtra();
        if (extra == null) return;
        switch (extra) {
            case PROXY_FUZE -> proximityFuze(grenade);
            case GLUE, FRAG_SLEEVE, TRIPLEX -> {}
        }
    }

    public static void onImpact(GrenadeUniversalEntity grenade, HitResult impact) {
        EnumGrenadeExtra extra = grenade.getExtra();
        if (extra == null) return;
        switch (extra) {
            case GLUE -> glue(grenade, impact);
            case PROXY_FUZE, FRAG_SLEEVE, TRIPLEX -> {}
        }
    }

    public static void onExplode(GrenadeUniversalEntity grenade) {
        EnumGrenadeExtra extra = grenade.getExtra();
        if (extra == null) return;
        switch (extra) {
            case FRAG_SLEEVE -> GrenadeFillingItem.standardFragmentation(grenade, 25F);
            case TRIPLEX -> spawnTriplex(grenade);
            case GLUE, PROXY_FUZE -> {}
        }
    }

    private static void glue(GrenadeUniversalEntity grenade, HitResult impact) {
        if (!(impact instanceof BlockHitResult blockHit)) return;
        Vec3 hit = blockHit.getLocation();
        grenade.setPos(hit.x, hit.y, hit.z);
        grenade.getStuck(blockHit.getBlockPos(), blockHit.getDirection().get3DDataValue());
    }

    private static void proximityFuze(GrenadeUniversalEntity grenade) {
        if (grenade.getTimer() < 10 || grenade.getTimer() % 3 != 0) return;
        List<LivingEntity> living =
                grenade.level()
                        .getEntitiesOfClass(
                                LivingEntity.class,
                                new AABB(
                                                grenade.getX(),
                                                grenade.getY(),
                                                grenade.getZ(),
                                                grenade.getX(),
                                                grenade.getY(),
                                                grenade.getZ())
                                        .inflate(10D));
        for (LivingEntity e : living) {
            if (e == grenade.getThrower()) continue;
            if (e.distanceTo(grenade) <= 10F) {
                grenade.explode();
                return;
            }
        }
    }

    private static void spawnTriplex(GrenadeUniversalEntity grenade) {
        ItemStack frag =
                GrenadeUniversalItem.make(
                        grenade.getShell(),
                        grenade.getFilling(),
                        GrenadeFuzeItem.EnumGrenadeFuze.S3);

        double angle = grenade.level().getRandom().nextDouble() * Math.PI * 2D;

        for (int i = 0; i < 3; i++) {
            GrenadeUniversalEntity triplet =
                    new GrenadeUniversalEntity(grenade.level(), frag)
                            .setTrail(GrenadeUniversalEntity.TRAIL_TRIPLET);
            triplet.setPos(grenade.getX(), grenade.getY(), grenade.getZ());
            triplet.setThrower(grenade.getThrower());
            triplet.setDeltaMovement(Math.cos(angle) * 0.25D, 0.75D, -Math.sin(angle) * 0.25D);
            grenade.level().addFreshEntity(triplet);
            angle += Math.toRadians(120D);
        }
    }

    public enum EnumGrenadeExtra {
        GLUE,
        PROXY_FUZE,
        FRAG_SLEEVE,
        TRIPLEX;

        public static final EnumGrenadeExtra[] VALUES = values();
    }
}
