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

package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.main.ResourceManager;
import com.hbm.registration.Reg;
import com.hbm.registration.Registrar;
import com.hbm.util.MobUtil;
import com.hbm.util.RegistryUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class GlyphidBlasterEntity extends GlyphidBombardierEntity {

    public static Reg.@Nullable EntityHandle<GlyphidBlasterEntity> TYPE;

    public static void register() {
        TYPE =
                Reg.entity(
                        "entity_glyphid_blaster",
                        () ->
                                EntityType.Builder.<GlyphidBlasterEntity>of(
                                                GlyphidBlasterEntity::new, MobCategory.MONSTER)
                                        .notInPeaceful()
                                        .sized(2F, 1.125F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        RegistryUtil.id(
                                                                "entity_glyphid_blaster"))));
        Registrar.registerLivingAttributes(TYPE, GlyphidBlasterEntity::createAttributes);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GlyphidBombardierEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBlaster().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBlaster().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBlaster().damage);
    }

    public GlyphidBlasterEntity(EntityType<? extends GlyphidBlasterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_blaster_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 1.25D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getBlaster().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBlaster().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getBlaster().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBlaster;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }

    @Override
    public float getBombDamage() {
        return 15F;
    }

    @Override
    public int getBombCount() {
        return 10;
    }

    @Override
    public float getSpreadMult() {
        return 0.5F;
    }

    @Override
    public double getV0() {
        return 1.25D;
    }
}
