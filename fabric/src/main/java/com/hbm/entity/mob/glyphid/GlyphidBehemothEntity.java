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

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.MistEntity;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.mob.glyphid.GlyphidStats;
import com.hbm.entity.projectile.ChemicalEntity;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.main.ResourceManager;
import com.hbm.util.MobUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.Level;

public class GlyphidBehemothEntity extends GlyphidEntity {

    public int timer = 120;
    public int breathTime = 0;

    public GlyphidBehemothEntity(EntityType<? extends GlyphidBehemothEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GlyphidEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBehemoth().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBehemoth().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBehemoth().damage);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(2.5F, 1.5F);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_behemoth_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 1.5D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getBehemoth().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBehemoth().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getBehemoth().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBehemoth;
    }

    @Override
    public void tick() {
        boolean lockYaw = !level().isClientSide() && breathTime > 0;
        float lockedYaw = getYRot();
        float lockedBodyYaw = yBodyRot;
        super.tick();

        if (lockYaw) {
            setYRot(lockedYaw);
            yBodyRot = lockedBodyYaw;
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (getTarget() == null) {
            timer = 120;
            breathTime = 0;
        } else if (breathTime > 0) {
            if (!((LivingEntity) this).isSwinging())
                ((LivingEntity) this)
                        .swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            acidAttack();
            breathTime--;
        } else if (--timer <= 0) {
            breathTime = 120;
            timer = 120;
        }
    }

    public void acidAttack() {
        LivingEntity target = getTarget();
        if (!level().isClientSide() && target != null && distanceTo(target) < 20) {
            addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 2 * 20, 6));
            ChemicalEntity chem =
                    new ChemicalEntity(ModEntities.CHEMICAL.get(), level(), this, 0, 0, 0);
            chem.setFluid(NTMFluids.SULFURIC_ACID);
            level().addFreshEntity(chem);
        }
    }

    @Override
    protected void handleKillingBlow() {
        super.handleKillingBlow();

        if (!level().isClientSide()) {
            MistEntity mist = new MistEntity(level());
            mist.setType(NTMFluids.SULFURIC_ACID);
            mist.setPos(getX(), getY(), getZ());
            mist.setArea(10, 4);
            mist.setDuration(120);
            level().addFreshEntity(mist);
        }
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.15, 2), 100);
    }

    @Override
    public int swingDuration() {
        return 100;
    }
}
