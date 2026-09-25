// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public abstract class EntityBeamBase extends Entity {

    private static final EntityDataAccessor<String> OWNER_NAME =
            SynchedEntityData.defineId(EntityBeamBase.class, EntityDataSerializers.STRING);

    public EntityBeamBase(EntityType<? extends EntityBeamBase> type, Level level) {
        super(type, level);
    }

    public EntityBeamBase(EntityType<? extends EntityBeamBase> type, Level level, Player player) {
        this(type, level);

        this.setOwnerName(player.getDisplayName().getString());

        Vec3 vec = player.getLookAngle().yRot(-90F);
        float l = 0.075F;
        vec = new Vec3(vec.x * l, vec.y * l, vec.z * l);

        Vec3 vec0 = player.getLookAngle();
        float d = 0.1F;
        vec0 = new Vec3(vec0.x * d, vec0.y * d, vec0.z * d);

        this.setPos(
                player.getX() + vec.x + vec0.x,
                player.getY() + player.getEyeHeight() + vec0.y,
                player.getZ() + vec.z + vec0.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_NAME, "");
    }

    public String getOwnerName() {
        return this.entityData.get(OWNER_NAME);
    }

    public void setOwnerName(String name) {
        this.entityData.set(OWNER_NAME, name);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}
}
