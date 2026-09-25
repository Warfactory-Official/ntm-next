// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.entity.projectile.EntityBoxcar;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.platform.Services;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityBomber extends EntityPlaneBase {

    private static final EntityDataAccessor<Byte> STYLE =
            SynchedEntityData.defineId(EntityBomber.class, EntityDataSerializers.BYTE);

    private int bombStart = 75;
    private int bombStop = 125;
    private int bombRate = 3;
    private int type;
    private AudioWrapper audio;

    public EntityBomber(EntityType<? extends EntityBomber> type, Level level) {
        super(type, level);
    }

    public static EntityBomber statFacCarpet(Level level, double x, double y, double z) {
        return create(level, x, y, z, 50, 100, 2, 0);
    }

    public static EntityBomber statFacNapalm(Level level, double x, double y, double z) {
        return create(level, x, y, z, 50, 100, 5, 1);
    }

    public static EntityBomber statFacChlorine(Level level, double x, double y, double z) {
        return create(level, x, y, z, 50, 100, 4, 2);
    }

    public static EntityBomber statFacOrange(Level level, double x, double y, double z) {
        return create(level, x, y, z, 75, 125, 1, 3);
    }

    public static EntityBomber statFacABomb(Level level, double x, double y, double z) {
        EntityBomber bomber = create(level, x, y, z, 60, 70, 65, 4);
        int style =
                switch (level.getRandom().nextInt(3)) {
                    case 0 -> 5;
                    case 1 -> 6;
                    default -> 7;
                };
        if (level.getRandom().nextInt(100) == 0) style = 8;
        bomber.entityData.set(STYLE, (byte) style);
        return bomber;
    }

    public static EntityBomber statFacStinger(Level level, double x, double y, double z) {
        EntityBomber bomber = create(level, x, y, z, 50, 150, 10, 5);
        bomber.entityData.set(STYLE, (byte) 4);
        return bomber;
    }

    public static EntityBomber statFacBoxcar(Level level, double x, double y, double z) {
        EntityBomber bomber = create(level, x, y, z, 50, 150, 10, 6);
        bomber.entityData.set(STYLE, (byte) 6);
        return bomber;
    }

    public static EntityBomber statFacPC(Level level, double x, double y, double z) {
        EntityBomber bomber = create(level, x, y, z, 75, 125, 1, 7);
        bomber.entityData.set(STYLE, (byte) 6);
        return bomber;
    }

    private static EntityBomber create(
            Level level, double x, double y, double z, int start, int stop, int rate, int type) {
        EntityBomber bomber = new EntityBomber(ModEntities.BOMBER.get(), level);
        bomber.timer = 200;
        bomber.bombStart = start;
        bomber.bombStop = stop;
        bomber.bombRate = rate;
        bomber.fac(x, y, z);
        bomber.type = type;
        return bomber;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STYLE, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        if (level().isClientSide()) {
            tickClientAudio();
            return;
        }
        if (health > 0
                && tickCount > bombStart
                && tickCount < bombStop
                && tickCount % bombRate == 0) {
            bomb();
        }
    }

    private void tickClientAudio() {
        if (health > 0) {
            if (audio == null || !audio.isPlaying()) {
                audio =
                        AudioSystem.getLoopedSound(
                                getStyle() <= 4
                                        ? ModSounds.ENTITY_BOMBER_SMALL_LOOP.get()
                                        : ModSounds.ENTITY_BOMBER_LOOP.get(),
                                SoundSource.AMBIENT,
                                (float) getX(),
                                (float) getY(),
                                (float) getZ(),
                                2F,
                                250F,
                                1F,
                                20);
                if (audio != null) audio.startSound();
            }
            if (audio != null) {
                audio.keepAlive();
                audio.updatePosition((float) getX(), (float) getY(), (float) getZ());
            }
        } else if (audio != null && audio.isPlaying()) {
            audio.stopSound();
            audio = null;
        }
    }

    private void bomb() {
        if (type == 3) {
            fizz(getX(), getY(), getZ());
            ExplosionChaos.spawnPoisonCloud(level(), getX(), getY() - 1F, getZ(), 10, 0.5D, 3);
        } else if (type == 5) {

        } else if (type == 6) {
            level().playSound(
                            null,
                            getX() + 0.5F,
                            getY() + 0.5F,
                            getZ() + 0.5F,
                            ModSounds.MISSILE_TAKE_OFF.get(),
                            SoundSource.PLAYERS,
                            10F,
                            0.9F + random.nextFloat() * 0.2F);
            EntityBoxcar rocket = new EntityBoxcar(ModEntities.BOXCAR.get(), level());
            rocket.setPos(
                    getX() + random.nextDouble() - 0.5D,
                    getY() - random.nextDouble(),
                    getZ() + random.nextDouble() - 0.5D);
            level().addFreshEntity(rocket);
        } else if (type == 7) {
            fizz(getX(), getY(), getZ());
            ExplosionChaos.spawnPoisonCloud(
                    level(),
                    getX(),
                    level().getHeight(
                                            Heightmap.Types.WORLD_SURFACE,
                                            Mth.floor(getX()),
                                            Mth.floor(getZ()))
                            + 2,
                    getZ(),
                    10,
                    1D,
                    2);
        } else {
            level().playSound(
                            null,
                            getX() + 0.5F,
                            getY() + 0.5F,
                            getZ() + 0.5F,
                            ModSounds.BOMBER_WHISTLE.get(),
                            SoundSource.PLAYERS,
                            10F,
                            0.9F + random.nextFloat() * 0.2F);
            EntityBombletZeta zeta = new EntityBombletZeta(ModEntities.BOMBLET_ZETA.get(), level());

            zeta.rotation();
            zeta.setType(type);
            zeta.setPos(
                    getX() + random.nextDouble() - 0.5D,
                    getY() - random.nextDouble(),
                    getZ() + random.nextDouble() - 0.5D);
            Vec3 motion = getDeltaMovement();
            zeta.setDeltaMovement(
                    type == 0 ? motion.x + random.nextGaussian() * 0.15D : motion.x,
                    motion.y,
                    type == 0 ? motion.z + random.nextGaussian() * 0.15D : motion.z);
            level().addFreshEntity(zeta);
        }
    }

    private void fizz(double x, double y, double z) {
        level().playSound(
                        null,
                        x + 0.5D,
                        y + 0.5D,
                        z + 0.5D,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS,
                        5F,
                        2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
    }

    private void fac(double x, double y, double z) {
        Vec3 vector =
                new Vec3(random.nextDouble() - 0.5D, 0D, random.nextDouble() - 0.5D).normalize();
        vector = vector.scale(Services.CONFIG.runtime().enableBomberShortMode() ? 1D : 2D);
        setPos(x - vector.x * 100D, y + 50D, z - vector.z * 100D);
        setDeltaMovement(vector.x, 0D, vector.z);
        rotation();
        forceSpawnChunk();

        int style =
                switch (random.nextInt(7)) {
                    case 0, 1 -> 1;
                    case 2, 3 -> 2;
                    case 4 -> 5;
                    case 5 -> 6;
                    default -> 7;
                };
        if (random.nextInt(100) == 0)
            style =
                    switch (random.nextInt(4)) {
                        case 0 -> 0;
                        case 1 -> 3;
                        case 2 -> 4;
                        default -> 8;
                    };
        entityData.set(STYLE, (byte) style);
    }

    public int getStyle() {
        return entityData.get(STYLE);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        bombStart = input.getIntOr("bombStart", 75);
        bombStop = input.getIntOr("bombStop", 125);
        bombRate = input.getIntOr("bombRate", 3);
        type = input.getIntOr("type", 0);
        entityData.set(STYLE, input.getByteOr("style", (byte) 0));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("bombStart", bombStart);
        output.putInt("bombStop", bombStop);
        output.putInt("bombRate", bombRate);
        output.putInt("type", type);
        output.putByte("style", entityData.get(STYLE));
    }
}
