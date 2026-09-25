// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.NuclearTech;
import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionFleija;
import com.hbm.explosion.ExplosionHurtUtil;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionSolinium;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.PlasmaBlastPayload;
import com.hbm.platform.Services;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.sound.ModSounds;
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

public class EntityNukeExplosionMK3 extends EntityExplosionChunkloading {

    public static Map<ATEntry, Long> at = new HashMap<>();
    public int age = 0;
    public int destructionRange = 0;
    public ExplosionFleija expl;
    public ExplosionSolinium sol;
    public int speed = 1;
    public float coefficient = 1;
    public float coefficient2 = 1;
    public boolean did = false;
    public int extType = 0;

    public EntityNukeExplosionMK3(EntityType<? extends EntityNukeExplosionMK3> type, Level level) {
        super(type, level);
    }

    public static EntityNukeExplosionMK3 statFacFleija(
            Level world, double x, double y, double z, int range) {

        EntityNukeExplosionMK3 entity =
                new EntityNukeExplosionMK3(ModEntities.NUKE_EXPLOSION_MK3.get(), world);
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

                        world.playSound(
                                null,
                                ix,
                                iy,
                                iz,
                                ModSounds.UFO_BLAST.get(),
                                SoundSource.BLOCKS,
                                15.0F,
                                0.7F + world.getRandom().nextFloat() * 0.2F);

                        Services.NETWORK.sendToAllAround(
                                new PlasmaBlastPayload(ix, iy, iz, 0.0F, 0.75F, 1.0F, 0F, 0F, 7.5F),
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

        if (!this.did) {
            if (level() instanceof ServerLevel server) {
                AwardRegions.inLevel(
                        server, p -> HbmCriteria.detonation(p, DetonationTrigger.Kind.NUKE));
            }
            if (!level().isClientSide() && Services.CONFIG.runtime().extendedLogging())
                NuclearTech.LOGGER.info(
                        "[NUKE] Initialized mk3 explosion at {} / {} / {} with strength {}!",
                        getX(),
                        getY(),
                        getZ(),
                        destructionRange);

            initExplosion();
            if (level() instanceof ServerLevel server) {
                SatelliteDetector.reportEvent(
                        server,
                        SatelliteDetector.DURATION_HIGH,
                        SatelliteDetector.BurstIntensity.HIGH,
                        getX(),
                        getZ());
            }
            this.did = true;
        }

        speed += 1;

        if (!level().isClientSide())
            for (int i = 0; i < this.speed; i++) {

                if (extType == 0 && expl.update()) {
                    this.discard();
                }
                if (extType == 1 && sol.update()) {
                    this.discard();
                }
            }

        if (!level().isClientSide()) {
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.WEATHER,
                            10000.0F,
                            0.8F + this.random.nextFloat() * 0.2F);

            if (extType != 1) {
                ExplosionNukeGeneric.dealDamage(
                        level(), getX(), getY(), getZ(), this.destructionRange * 2);
            } else {
                ExplosionHurtUtil.doRadiation(
                        level(), getX(), getY(), getZ(), 15000, 250000, this.destructionRange);
            }
        }

        age++;
    }

    private void initExplosion() {
        int x = (int) getX();
        int y = (int) getY();
        int z = (int) getZ();
        if (extType == 0)
            expl =
                    new ExplosionFleija(
                            x,
                            y,
                            z,
                            level(),
                            this.destructionRange,
                            this.coefficient,
                            this.coefficient2);
        if (extType == 1)
            sol =
                    new ExplosionSolinium(
                            x,
                            y,
                            z,
                            level(),
                            this.destructionRange,
                            this.coefficient,
                            this.coefficient2);
    }

    public EntityNukeExplosionMK3 makeSol() {
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

        long time = input.getLongOr("milliTime", 0L);

        if (BombConfig.limitExplosionLifespan > 0
                && System.currentTimeMillis() - time > BombConfig.limitExplosionLifespan * 1000L) {
            this.discard();
        }

        initExplosion();
        if (expl != null) expl.readFromNbt(input, "expl_");
        if (sol != null) sol.readFromNbt(input, "sol_");

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

        output.putLong("milliTime", System.currentTimeMillis());

        if (expl != null) expl.saveToNbt(output, "expl_");
        if (sol != null) sol.saveToNbt(output, "sol_");
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
