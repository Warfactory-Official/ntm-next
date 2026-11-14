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

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.WaypointEntity;
import com.hbm.entity.mob.ParasiteMaggotEntity;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.mob.glyphid.GlyphidStats;
import com.hbm.explosion.MiniNukeAftermath;
import com.hbm.explosion.vanillant.ComposableExplosion;
import com.hbm.explosion.vanillant.standard.AttenuatedRayBlockSelector;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.CellDestruction;
import com.hbm.explosion.vanillant.standard.SinglePointExplosionEntityProcessor;
import com.hbm.main.ResourceManager;
import com.hbm.sound.ModSounds;
import com.hbm.util.MobUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public class GlyphidNuclearEntity extends GlyphidEntity {

    private static final int ALLY_SHIELD_FRAME = 90;
    private static final int DETONATION_FRAME = 100;
    private static final int COUNTDOWN_PING_PERIOD = 10;
    private static final double ALLY_SHIELD_REACH = 8D;
    private static final double UNGUIDED_SIGNAL_REACH = 4D;

    public int deathTicks;
    private boolean retreatSignalled;
    private boolean lootless;

    public GlyphidNuclearEntity(EntityType<? extends GlyphidNuclearEntity> type, Level level) {
        super(type, level);
    }

    private static StatBundle nuclearStats() {
        return GlyphidStats.getStats().getNuclear();
    }

    public static AttributeSupplier.Builder createAttributes() {
        var stats = nuclearStats();
        return GlyphidEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, stats.health)
                .add(Attributes.MOVEMENT_SPEED, MobUtil.oldAiWalkSpeed(stats.speed))
                .add(Attributes.ATTACK_DAMAGE, stats.damage);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        var stats = nuclearStats();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(stats.health);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(MobUtil.oldAiWalkSpeed(stats.speed));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(stats.damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsNuclear;
    }

    @Override
    public boolean isNuclearType() {
        return true;
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_nuclear_tex;
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(2.5F, 1.75F);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public double getGlyphidScale() {
        return 2D;
    }

    @Override
    public boolean isArmorBroken(float amount) {
        double chance = Math.min(Math.pow(amount * 0.12, 2), 100);
        return random.nextInt(100) <= chance;
    }

    @Override
    public boolean doesInfectedSpawnMaggots() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (tickCount % 20 == 0) reviewTask(level);
    }

    private void reviewTask(ServerLevel level) {
        switch (getCurrentTask()) {
            case TASK_FOLLOW -> {
                if (isAtDestination()) setCurrentTask(TASK_IDLE, null);
            }
            case TASK_BUILD_HIVE -> {
                if (getLastHurtByMob() == null)
                    addEffect(new MobEffectInstance(MobEffects.SPEED, 10 * 20, 3));
            }
            case TASK_TERRAFORM -> killWithoutLoot(level);
            default -> {}
        }
    }

    private void killWithoutLoot(ServerLevel level) {
        lootless = true;
        try {
            kill(level);
        } finally {
            lootless = false;
        }
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return !lootless && super.shouldDropLoot(level);
    }

    @Override
    public void communicate(int task, @Nullable WaypointEntity waypoint) {
        var reach = waypoint == null ? UNGUIDED_SIGNAL_REACH : waypoint.radius;
        for (var scout :
                level().getEntitiesOfClass(
                                GlyphidEntity.class, around(reach), GlyphidEntity::isScoutType)) {
            if (scout.getCurrentTask() != task) scout.setCurrentTask(task, waypoint);
        }
    }

    private AABB around(double reach) {
        return new AABB(position(), position()).inflate(reach);
    }

    @Override
    protected void tickDeath() {
        int frame = ++deathTicks;

        if (!retreatSignalled) {
            retreatSignalled = true;
            communicate(TASK_INITIATE_RETREAT, null);
        }

        if (frame == ALLY_SHIELD_FRAME) shieldAllies();

        if (frame == DETONATION_FRAME) {
            if (level() instanceof ServerLevel server) detonate(server);
            discard();
            return;
        }

        if (frame % COUNTDOWN_PING_PERIOD == 0 && level() instanceof ServerLevel server) {
            server.playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSounds.FSTBMB_PING.get(),
                    SoundSource.BLOCKS,
                    5F,
                    1F);
        }
    }

    private void shieldAllies() {
        for (var ally :
                level().getEntitiesOfClass(
                                GlyphidEntity.class, around(ALLY_SHIELD_REACH), g -> g != this)) {
            ally.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, 20, 6));
            ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 15 * 20, 1));
        }
    }

    private void detonate(ServerLevel server) {
        var blast =
                new ComposableExplosion(server, getX(), getY(), getZ(), 25, this)
                        .entityProcessor(new SinglePointExplosionEntityProcessor());
        if (subtype() != TYPE_INFECTED) {
            if (EventHooks.canEntityGrief(server, (Entity) this)) {
                blast.blockSelector(new AttenuatedRayBlockSelector(24))
                        .blockProcessor(lavaDebris());
            }
        } else {
            releaseMaggots(server);
        }
        blast.explode();

        var at = position();
        MiniNukeAftermath.soundWithoutVibration(server, at, 15F, 1F);
        MiniNukeAftermath.cloud(server, at, 0.5D, MiniNukeAftermath.balefireFlash(random));
    }

    private static CellDestruction lavaDebris() {
        var lava = ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState();
        return CellDestruction.noDrops().transform(new BlockMutatorDebris(lava));
    }

    private void releaseMaggots(ServerLevel server) {
        var type = ModEntities.PARASITE_MAGGOT.get();
        int count = 15 + random.nextInt(6);
        for (int i = 0; i < count; i++) {
            float side = (i % 2 - 0.5F) * 0.5F;
            float ahead = (i / 2 - 0.5F) * 0.5F;
            var maggot = new ParasiteMaggotEntity(type, server);
            maggot.snapTo(
                    getX() + side, getY() + 0.5D, getZ() + ahead, random.nextFloat() * 360F, 0F);
            maggot.setDeltaMovement(side, 0D, ahead);
            ((Entity) maggot).hurtMarked = true;
            server.addFreshEntity(maggot);
        }
    }
}
