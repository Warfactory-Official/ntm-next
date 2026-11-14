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

import com.hbm.config.HbmConfig;
import com.hbm.config.RuntimeConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.BombletZetaEntity;
import com.hbm.entity.projectile.BoxcarEntity;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.sound.EntityAudioLoop;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class BomberEntity extends PlaneBaseEntity {

    private static final EntityDataAccessor<Byte> STYLE =
            SynchedEntityData.defineId(BomberEntity.class, EntityDataSerializers.BYTE);
    private static final Map<Integer, Supplier<SoundEvent>> ADDON_LOOPS = new HashMap<>();

    private static final byte[] ROLLED_AIRFRAMES = {1, 1, 2, 2, 5, 6, 7};
    private static final byte[] RARE_AIRFRAMES = {0, 3, 4, 8};
    private static final byte[] HEAVY_AIRFRAMES = {5, 6, 7};
    private static final int LAST_SMALL_ENGINE_STYLE = 4;

    private enum Payload {
        CARPET(50, 100, 2, bomber -> bomber.dropBomblet(true)),
        NAPALM(50, 100, 5, bomber -> bomber.dropBomblet(false)),
        CHLORINE(50, 100, 4, bomber -> bomber.dropBomblet(false)),
        ORANGE(75, 125, 1, bomber -> bomber.spray(bomber.getY() - 1F, 0.5D, 3)),
        ATOMIC(60, 70, 65, bomber -> bomber.dropBomblet(false)),
        STINGER(50, 150, 10, _ -> {}),
        BOXCAR(50, 150, 10, BomberEntity::launchBoxcar),
        CLOUD(75, 125, 1, bomber -> bomber.spray(bomber.surfaceBelow() + 2, 1D, 2));

        private static final Payload[] BY_TYPE = values();

        final int start;
        final int stop;
        final int rate;
        final Consumer<BomberEntity> release;

        Payload(int start, int stop, int rate, Consumer<BomberEntity> release) {
            this.start = start;
            this.stop = stop;
            this.rate = rate;
            this.release = release;
        }
    }

    private int bombStart = 75;
    private int bombStop = 125;
    private int bombRate = 3;
    private int type;
    private final EntityAudioLoop engine =
            new EntityAudioLoop(this, this::engineSound, SoundSource.PLAYERS, 2F, 250F);

    public BomberEntity(EntityType<? extends BomberEntity> type, Level level) {
        super(type, level);
    }

    public static BomberEntity statFacCarpet(Level level, double x, double y, double z) {
        return sortie(level, x, y, z, Payload.CARPET);
    }

    public static BomberEntity statFacNapalm(Level level, double x, double y, double z) {
        return sortie(level, x, y, z, Payload.NAPALM);
    }

    public static BomberEntity statFacChlorine(Level level, double x, double y, double z) {
        return sortie(level, x, y, z, Payload.CHLORINE);
    }

    public static BomberEntity statFacOrange(Level level, double x, double y, double z) {
        return sortie(level, x, y, z, Payload.ORANGE);
    }

    public static BomberEntity statFacABomb(Level level, double x, double y, double z) {
        BomberEntity bomber = sortie(level, x, y, z, Payload.ATOMIC);
        RandomSource random = level.getRandom();
        byte style = HEAVY_AIRFRAMES[random.nextInt(HEAVY_AIRFRAMES.length)];
        bomber.entityData.set(STYLE, random.nextInt(100) == 0 ? 8 : style);
        return bomber;
    }

    public static BomberEntity statFacStinger(Level level, double x, double y, double z) {
        return restyled(sortie(level, x, y, z, Payload.STINGER), 4);
    }

    public static BomberEntity statFacBoxcar(Level level, double x, double y, double z) {
        return restyled(sortie(level, x, y, z, Payload.BOXCAR), 6);
    }

    public static BomberEntity statFacPC(Level level, double x, double y, double z) {
        return restyled(sortie(level, x, y, z, Payload.CLOUD), 6);
    }

    public static BomberEntity variant(
            Level level,
            double x,
            double y,
            double z,
            int start,
            int stop,
            int rate,
            int type,
            int style) {
        return restyled(create(level, x, y, z, start, stop, rate, type), style);
    }

    public static void addonLoop(int style, Supplier<SoundEvent> sound) {
        if (ADDON_LOOPS.putIfAbsent(style, sound) != null) {
            throw new IllegalStateException("duplicate bomber loop for style " + style);
        }
    }

    private static BomberEntity sortie(Level level, double x, double y, double z, Payload payload) {
        return create(level, x, y, z, payload.start, payload.stop, payload.rate, payload.ordinal());
    }

    private static BomberEntity restyled(BomberEntity bomber, int style) {
        bomber.entityData.set(STYLE, (byte) style);
        return bomber;
    }

    private static BomberEntity create(
            Level level, double x, double y, double z, int start, int stop, int rate, int type) {
        var bomber = new BomberEntity(ModEntities.BOMBER.get(), level);
        bomber.schedule(start, stop, rate, type);
        RandomSource random = level.getRandom();
        PlaneApproach.lineUp(
                bomber,
                random,
                x,
                y,
                z,
                ((RuntimeConfig) HbmConfig.RUNTIME).enableBomberShortMode() ? 1D : 2D,
                50D);
        bomber.entityData.set(STYLE, rollAirframe(random));
        return bomber;
    }

    private void schedule(int start, int stop, int rate, int payloadType) {
        this.timer = 200;
        this.bombStart = start;
        this.bombStop = stop;
        this.bombRate = rate;
        this.type = payloadType;
    }

    private static byte rollAirframe(RandomSource random) {
        byte style = ROLLED_AIRFRAMES[random.nextInt(ROLLED_AIRFRAMES.length)];
        return random.nextInt(100) == 0
                ? RARE_AIRFRAMES[random.nextInt(RARE_AIRFRAMES.length)]
                : style;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STYLE, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            this.engine.tick(this.health > 0);
            return;
        }
        if (isRemoved() || this.health <= 0) return;

        WorldSounds.entityLoop(this, this.tickCount == 1);
        int tick = this.tickCount;
        if (tick > this.bombStart && tick < this.bombStop && tick % this.bombRate == 0) release();
    }

    private void release() {
        if (this.type >= 0 && this.type < Payload.BY_TYPE.length)
            Payload.BY_TYPE[this.type].release.accept(this);
        else dropBomblet(false);
    }

    private SoundEvent engineSound() {
        int style = getStyle();
        Supplier<SoundEvent> addon = ADDON_LOOPS.get(style);
        if (addon != null) return addon.get();
        return (style <= LAST_SMALL_ENGINE_STYLE
                        ? ModSounds.ENTITY_BOMBER_SMALL_LOOP
                        : ModSounds.ENTITY_BOMBER_LOOP)
                .get();
    }

    private void dropBomblet(boolean scattered) {
        playFromBay(ModSounds.BOMBER_WHISTLE.get(), 10F, 0.9F + random.nextFloat() * 0.2F);
        var bomblet = new BombletZetaEntity(ModEntities.BOMBLET_ZETA.get(), level());
        bomblet.rotation();
        bomblet.setType(this.type);
        placeBelow(bomblet);
        Vec3 motion = getDeltaMovement();
        double mx = motion.x;
        double mz = motion.z;
        if (scattered) {
            mx += random.nextGaussian() * 0.15D;
            mz += random.nextGaussian() * 0.15D;
        }
        bomblet.setDeltaMovement(mx, 0D, mz);
        level().addFreshEntity(bomblet);
    }

    private void launchBoxcar() {
        playFromBay(ModSounds.MISSILE_TAKE_OFF.get(), 10F, 0.9F + random.nextFloat() * 0.2F);
        var rocket = new BoxcarEntity(ModEntities.BOXCAR.get(), level());
        placeBelow(rocket);
        level().addFreshEntity(rocket);
    }

    private void spray(double cloudY, double speed, int cloudType) {
        playFromBay(
                SoundEvents.FIRE_EXTINGUISH,
                5F,
                2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
        ExplosionChaos.spawnPoisonCloud(level(), getX(), cloudY, getZ(), 10, speed, cloudType);
    }

    private int surfaceBelow() {
        return level().getHeight(
                        Heightmap.Types.WORLD_SURFACE, Mth.floor(getX()), Mth.floor(getZ()));
    }

    private void playFromBay(SoundEvent sound, float volume, float pitch) {
        WorldSounds.play(
                level(),
                null,
                getX() + 0.5D,
                getY() + 0.5D,
                getZ() + 0.5D,
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch);
    }

    private void placeBelow(Entity drop) {
        drop.setPos(
                getX() + random.nextDouble() - 0.5D,
                getY() - random.nextDouble(),
                getZ() + random.nextDouble() - 0.5D);
    }

    public int getStyle() {
        return this.entityData.get(STYLE);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.bombStart = input.getIntOr("bombStart", 75);
        this.bombStop = input.getIntOr("bombStop", 125);
        this.bombRate = input.getIntOr("bombRate", 3);
        this.type = input.getIntOr("type", 0);
        this.entityData.set(STYLE, input.getByteOr("style", (byte) 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("bombStart", this.bombStart);
        output.putInt("bombStop", this.bombStop);
        output.putInt("bombRate", this.bombRate);
        output.putInt("type", this.type);
        output.putByte("style", this.entityData.get(STYLE));
    }
}
