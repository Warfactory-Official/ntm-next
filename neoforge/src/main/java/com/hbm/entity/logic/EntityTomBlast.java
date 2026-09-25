// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.NuclearTech;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionTom;
import com.hbm.handler.threading.BombForkJoinPool;
import com.hbm.platform.Services;
import com.hbm.saveddata.TomSaveData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class EntityTomBlast extends EntityExplosionChunkloading
        implements BombForkJoinPool.IJobCancellable {

    public int age = 0;
    public int destructionRange = 0;
    public boolean did = false;
    private int terrain;
    private long seed;
    private @Nullable ExplosionTom exp;

    public EntityTomBlast(EntityType<? extends EntityTomBlast> type, Level level) {
        super(type, level);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        age = input.getIntOr("age", 0);
        destructionRange = input.getIntOr("destructionRange", 0);
        did = input.getBooleanOr("did", false);
        terrain = input.getIntOr("terrain", ExplosionTom.LEGACY_TERRAIN);
        seed = input.getLongOr("seed", 0L);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("age", age);
        output.putInt("destructionRange", destructionRange);
        output.putBoolean("did", did);
        output.putInt("terrain", terrain);
        output.putLong("seed", seed);
    }

    @Override
    public void cancelJob() {
        if (!(level() instanceof ServerLevel server)) return;
        if (server.getServer().isSameThread()) discard();
        else
            server.getServer()
                    .execute(
                            () -> {
                                if (!isRemoved()) discard();
                            });
    }

    @Override
    public void remove(RemovalReason reason) {
        if (exp != null) exp.close();
        super.remove(reason);
    }

    @Override
    public void tick() {
        super.tick();

        if (!(level() instanceof ServerLevel server)) return;

        if (!this.did) {
            if (Services.CONFIG.runtime().extendedLogging())
                NuclearTech.LOGGER.info(
                        "[NUKE] Initialized TOM explosion at {} / {} / {} with strength {}!",
                        getX(),
                        getY(),
                        getZ(),
                        destructionRange);
            terrain = ExplosionTom.groundLevel(server, (int) getX(), (int) getZ());
            seed = random.nextLong();
            this.did = true;
        }

        if (exp == null)
            exp =
                    new ExplosionTom(
                            server,
                            (int) getX(),
                            (int) getZ(),
                            destructionRange,
                            terrain,
                            seed,
                            this);

        boolean flag = exp.tick();
        if (flag) {
            this.discard();
            TomSaveData data = TomSaveData.get(server);
            data.impact = true;
            data.fire = 1F;
            data.setDirty();
        }

        if (this.random.nextInt(5) == 0)
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            SoundEvents.GENERIC_EXPLODE,
                            SoundSource.BLOCKS,
                            10000.0F,
                            0.8F + this.random.nextFloat() * 0.2F);

        if (!flag) {
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.WEATHER,
                            10000.0F,
                            0.8F + this.random.nextFloat() * 0.2F);
            ExplosionNukeGeneric.dealDamage(
                    level(), getX(), getY(), getZ(), this.destructionRange * 2);
        }

        age++;
    }
}
