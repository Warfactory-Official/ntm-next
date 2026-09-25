// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.logic;

import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityDeathBlast extends Entity {

    public static final int maxAge = 60;

    public EntityDeathBlast(EntityType<? extends EntityDeathBlast> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        ChunkUtil.holdOwnChunk(this);
        super.tick();
        if (this.tickCount >= maxAge && !level().isClientSide()) {
            this.discard();

            level().addFreshEntity(
                            EntityNukeExplosionMK5.statFacNoRad(
                                    level(), 40, getX(), getY(), getZ()));

            if (level() instanceof ServerLevel server) {
                Services.NETWORK.sendToAllAround(
                        new MukePayload(getX(), getY() + 0.5D, getZ(), false, false),
                        new TargetPoint(server, getX(), getY(), getZ(), 250));
            }

            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                            SoundSource.BLOCKS,
                            25.0F,
                            0.9F);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }
}
