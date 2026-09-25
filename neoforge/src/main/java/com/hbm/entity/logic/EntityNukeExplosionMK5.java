// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.NuclearTech;
import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionNukeRayBatched;
import com.hbm.explosion.ExplosionNukeRayParallelized;
import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.interfaces.IExplosionRay;
import com.hbm.platform.Services;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.SectionGeneration;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityNukeExplosionMK5 extends EntityExplosionChunkloading
        implements BombForkJoinPool.IJobCancellable {

    private int strength;
    private int radius;
    private int algorithm;
    private boolean fallout = true;
    private int falloutAdd = 0;

    private IExplosionRay explosion;
    private boolean initialized = false;
    private boolean generationHeld;
    private ForkJoinPool generationPool;
    private long explosionStart;

    private UUID detonator;

    public EntityNukeExplosionMK5(EntityType<? extends EntityNukeExplosionMK5> type, Level level) {
        super(type, level);
    }

    public static EntityNukeExplosionMK5 statFac(Level level, int r, double x, double y, double z) {
        if (!level.isClientSide() && Services.CONFIG.runtime().extendedLogging()) {
            NuclearTech.LOGGER.info(
                    "[NUKE] Initialized explosion at {} / {} / {} with strength {}!", x, y, z, r);
        }
        if (r == 0) r = 25;

        EntityNukeExplosionMK5 mk5 =
                new EntityNukeExplosionMK5(ModEntities.NUKE_EXPLOSION_MK5.get(), level);
        mk5.strength = 2 * r;
        mk5.radius = r;
        mk5.algorithm = BombConfig.explosionAlgorithm;
        mk5.setPos(x, y, z);
        if (ExplosionData.DISABLE_NUCLEAR.get()) mk5.fallout = false;
        return mk5;
    }

    public int getRadius() {
        return radius;
    }

    public static EntityNukeExplosionMK5 statFacNoRad(
            Level level, int r, double x, double y, double z) {
        EntityNukeExplosionMK5 mk5 = statFac(level, r, x, y, z);
        mk5.fallout = false;
        return mk5;
    }

    public EntityNukeExplosionMK5 setDetonator(Entity detonator) {
        if (detonator instanceof Player) this.detonator = detonator.getUUID();
        return this;
    }

    public EntityNukeExplosionMK5 moreFallout(int fallout) {
        this.falloutAdd = fallout;
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            discard();
            return;
        }

        if (strength == 0) {
            discard();
            return;
        }

        AwardRegions.inLevel(
                (ServerLevel) level(), p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.NUKE));

        double r2 = this.radius * 2.0D;
        ExplosionNukeGeneric.dealDamage(level(), getX(), getY(), getZ(), r2);

        if (initialized && fallout && tickCount < 10 && strength >= 75) {
            ContaminationUtil.radiate(
                    level(), getX(), getY(), getZ(), radius * 2, 2_500_000F / (tickCount * 5 + 1));
        }

        if (!initialized) {
            explosionStart = System.currentTimeMillis();
            if (algorithm > 0) {
                generationPool = BombForkJoinPool.acquire();
                BombForkJoinPool.register(generationPool, level().dimension().identifier(), this);
                try {
                    SectionGeneration.acquire();
                    generationHeld = true;
                    explosion =
                            new ExplosionNukeRayParallelized(
                                    level(), getX(), getY(), getZ(), strength, radius, algorithm);
                } catch (RuntimeException | Error cause) {
                    discard();
                    throw cause;
                }
            } else {
                explosion =
                        new ExplosionNukeRayBatched(
                                level(),
                                Mth.floor(getX()),
                                Mth.floor(getY()),
                                Mth.floor(getZ()),
                                strength,
                                radius);
            }
            explosion.setDetonator(detonator);
            if (level() instanceof ServerLevel server) {
                SatelliteDetector.reportEvent(
                        server,
                        SatelliteDetector.DURATION_HIGH,
                        SatelliteDetector.BurstIntensity.HIGH,
                        getX(),
                        getZ());
            }
            initialized = true;
        }

        if (explosion.hasFailed()) {
            discard();
        } else if (!explosion.isComplete()) {
            explosion.update(BombConfig.mk5);
        } else {
            if (Services.CONFIG.runtime().extendedLogging()) {
                NuclearTech.LOGGER.info(
                        "[NUKE] Explosion complete. Time elapsed: {}ms",
                        System.currentTimeMillis() - explosionStart);
            }
            spawnFallout();
            discard();
        }
    }

    private void spawnFallout() {
        if (!fallout) return;

        int falloutScale =
                Math.max(
                        1,
                        (int) (radius * 2.5 + falloutAdd)
                                * ExplosionData.FALLOUT_RANGE.get()
                                / 100);
        EntityFalloutRain rain =
                EntityFalloutRain.statFac(level(), falloutScale, getX(), getY(), getZ());
        if (level().addFreshEntity(rain) && !rain.isRemoved()) rain.holdGenerationTracking();
    }

    @Override
    public void remove(RemovalReason reason) {
        try {
            if (explosion != null) explosion.cancel();
        } finally {
            if (generationHeld) {
                generationHeld = false;
                SectionGeneration.release();
            }
            if (generationPool != null) {
                BombForkJoinPool.unregister(generationPool, level().dimension().identifier(), this);
                BombForkJoinPool.release(generationPool);
                generationPool = null;
            }
            super.remove(reason);
        }
    }

    @Override
    public void cancelJob() {
        ServerLevel server = (ServerLevel) level();
        if (server.getServer().isSameThread()) discard();
        else server.getServer().execute(this::discard);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        radius = input.getIntOr("radius", 0);
        strength = input.getIntOr("strength", 0);
        algorithm = input.getIntOr("algorithm", BombConfig.explosionAlgorithm);
        fallout = input.getBooleanOr("fallout", true);
        detonator = input.read("detonator", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("radius", radius);
        output.putInt("strength", strength);
        output.putInt("algorithm", algorithm);
        output.putBoolean("fallout", fallout);
        if (detonator != null) output.store("detonator", UUIDUtil.CODEC, detonator);
    }
}
