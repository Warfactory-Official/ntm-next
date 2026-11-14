/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
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

import com.hbm.entity.effect.MistEntity;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.mob.glyphid.GlyphidStats;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.main.ResourceManager;
import com.hbm.registration.Reg;
import com.hbm.util.MobUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class GlyphidBrendaEntity extends GlyphidEntity {

    public static Reg.@Nullable EntityHandle<GlyphidEntity> GRUNT_TYPE;

    public GlyphidBrendaEntity(EntityType<? extends GlyphidBrendaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GlyphidEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBrenda().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBrenda().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBrenda().damage);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(2.5F, 1.75F);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_brenda_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 2D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getBrenda().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBrenda().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getBrenda().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBrenda;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.12, 2), 100);
    }

    @Override
    protected void handleKillingBlow() {
        super.handleKillingBlow();

        if (!level().isClientSide() && getHealth() <= 0.0F) {
            MistEntity mist = new MistEntity(level());
            mist.setType(NTMFluids.PHEROMONE);
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(14, 6);
            mist.setDuration(80);
            level().addFreshEntity(mist);

            if (GRUNT_TYPE != null && level() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 12; i++) {
                    GlyphidEntity glyphid = new GlyphidEntity(GRUNT_TYPE.get(), serverLevel);
                    glyphid.snapTo(
                            getX(), getY() + 0.5D, getZ(), random.nextFloat() * 360.0F, 0.0F);
                    serverLevel.addFreshEntity(glyphid);
                    glyphid.move(
                            MoverType.SELF,
                            new Vec3(random.nextGaussian(), 0, random.nextGaussian()));
                }
            }
        }
    }
}
