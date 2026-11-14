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

package com.hbm.entity.logic;

import com.hbm.NuclearTech;
import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.config.BombConfig;
import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.ExplosionChunkloadingEntity;
import com.hbm.explosion.ExplosionHurtUtil;
import com.hbm.explosion.NuclearShockwave;
import com.hbm.explosion.spiral.FleijaColumnProcessor;
import com.hbm.explosion.spiral.SoliniumColumnProcessor;
import com.hbm.explosion.spiral.SpiralExplosion;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlasmaBlastLayout;
import com.hbm.saveddata.satellites.RadiationBursts;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import com.hbm.util.ChunkUtil;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class NukeExplosionMK3Entity extends ExplosionChunkloadingEntity {
    public static Map<ATEntry, Long> at = new HashMap<>();
    public int age = 0;
    public int destructionRange = 0;
    public SpiralExplosion expl;
    public SpiralExplosion sol;
    public int speed = 1;
    public float coefficient = 1;
    public float coefficient2 = 1;
    public boolean did = false;
    public int extType = 0;

    public NukeExplosionMK3Entity(EntityType<? extends NukeExplosionMK3Entity> type, Level level) {
        super(type, level);
    }

    public static NukeExplosionMK3Entity statFacFleija(
            Level world, double x, double y, double z, int range) {

        NukeExplosionMK3Entity entity =
                new NukeExplosionMK3Entity(ModEntities.NUKE_EXPLOSION_MK3.get(), world);
        entity.setPos(x, y, z);
        entity.destructionRange = range;
        entity.speed = BombConfig.blastSpeed;
        entity.coefficient = 1.0F;

        Iterator<Entry<ATEntry, Long>> it = at.entrySet().iterator();

        while (it.hasNext()) {

            Entry<ATEntry, Long> next = it.next();
            if (next.getValue() < world.getGameTime()) {
                it.remove();
                continue;
            }

            ATEntry entry = next.getKey();
            if (entry.dim != world.dimension()) continue;

            Vec3 vec = new Vec3(x - entry.x, y - entry.y, z - entry.z);

            if (vec.length() < 300) {
                entity.discard();

                if (world instanceof ServerLevel server) {

                    for (int i = 0; i < 2; i++) {
                        double ix = i == 0 ? x : (entry.x + 0.5);
                        double iy = i == 0 ? y : (entry.y + 0.5);
                        double iz = i == 0 ? z : (entry.z + 0.5);

                        WorldSounds.play(
                                world,
                                null,
                                ix,
                                iy,
                                iz,
                                ModSounds.UFO_BLAST.get(),
                                SoundSource.BLOCKS,
                                15.0F,
                                0.7F + world.getRandom().nextFloat() * 0.2F);

                        PacketWire.sendToAllAround(
                                PlasmaBlastLayout.encode(
                                        ix, iy, iz, 0.0F, 0.75F, 1.0F, 0F, 0F, 7.5F),
                                new TargetPoint(server, ix, iy, iz, 150));
                    }
                }

                break;
            }
        }

        if (!entity.isRemoved()) {
            ChunkUtil.holdOwnChunk(entity);
        }

        return entity;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (!did) arm(server);
        var finished = false;
        for (var budget = ++speed; budget > 0 && !finished; budget--) finished = carveColumn();
        if (finished) discard();
        WorldSounds.play(
                server,
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER,
                10000.0F,
                0.8F + random.nextFloat() * 0.2F);
        if (extType == 1)
            ExplosionHurtUtil.doRadiation(
                    server, getX(), getY(), getZ(), 15000, 250000, destructionRange);
        else NuclearShockwave.sweep(server, getX(), getY(), getZ(), destructionRange * 2);
        age++;
    }

    private void arm(ServerLevel server) {
        AwardRegions.inLevel(server, p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.NUKE));
        if (((RuntimeConfig) HbmConfig.RUNTIME).extendedLogging()) {
            NuclearTech.LOGGER.info(
                    "[NUKE] Initialized mk3 explosion at {} / {} / {} with strength {}!",
                    getX(),
                    getY(),
                    getZ(),
                    destructionRange);
        }
        initExplosion();
        RadiationBursts.report(server, RadiationBursts.Intensity.HIGH, getX(), getZ());
        did = true;
    }

    private boolean carveColumn() {
        return switch (extType) {
            case 0 -> expl.advanceColumn();
            case 1 -> sol.advanceColumn();
            default -> false;
        };
    }

    private void initExplosion() {
        int x = (int) getX();
        int y = (int) getY();
        int z = (int) getZ();
        if (extType == 0)
            expl =
                    new SpiralExplosion(
                            level(),
                            x,
                            y,
                            z,
                            this.destructionRange,
                            FleijaColumnProcessor.INSTANCE);
        if (extType == 1)
            sol =
                    new SpiralExplosion(
                            level(),
                            x,
                            y,
                            z,
                            this.destructionRange,
                            SoliniumColumnProcessor.INSTANCE);
    }

    public NukeExplosionMK3Entity makeSol() {
        this.extType = 1;
        return this;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        age = input.getIntOr("age", 0);
        destructionRange = input.getIntOr("destructionRange", 0);
        speed = input.getIntOr("speed", 1);
        coefficient = input.getFloatOr("coefficient", 1F);
        coefficient2 = input.getFloatOr("coefficient2", 1F);
        did = input.getBooleanOr("did", false);
        extType = input.getIntOr("extType", 0);

        long savedGameTick = input.getLongOr("savedGameTick", 0L);

        if (BombConfig.limitExplosionLifespan > 0
                && level().getGameTime() - savedGameTick
                        > BombConfig.limitExplosionLifespan * 20L) {
            this.discard();
        }

        initExplosion();
        if (expl != null) expl.read(input, "expl_");
        if (sol != null) sol.read(input, "sol_");

        this.did = true;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("age", age);
        output.putInt("destructionRange", destructionRange);
        output.putInt("speed", speed);
        output.putFloat("coefficient", coefficient);
        output.putFloat("coefficient2", coefficient2);
        output.putBoolean("did", did);
        output.putInt("extType", extType);

        output.putLong("savedGameTick", level().getGameTime());

        if (expl != null) expl.write(output, "expl_");
        if (sol != null) sol.write(output, "sol_");
    }

    public record ATEntry(ResourceKey<Level> dim, int x, int y, int z) {

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            ATEntry other = (ATEntry) obj;
            if (dim != other.dim) return false;
            if (x != other.x) return false;
            if (y != other.y) return false;
            return z == other.z;
        }
    }
}
