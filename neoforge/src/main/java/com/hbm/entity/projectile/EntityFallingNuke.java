// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionNukeCustom;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityFallingNuke extends ThrowableProjectile {

    private static final EntityDataAccessor<Byte> NUKE_META =
            SynchedEntityData.defineId(EntityFallingNuke.class, EntityDataSerializers.BYTE);

    float tnt;
    float nuke;
    float hydro;
    float amat;
    float dirty;
    float schrab;
    float euph;

    public EntityFallingNuke(EntityType<? extends EntityFallingNuke> type, Level level) {
        super(type, level);

        refreshDimensions();
    }

    public EntityFallingNuke(
            Level level,
            float tnt,
            float nuke,
            float hydro,
            float amat,
            float dirty,
            float schrab,
            float euph) {
        this(ModEntities.FALLING_BOMB.get(), level);
        this.tnt = tnt;
        this.nuke = nuke;
        this.hydro = hydro;
        this.amat = amat;
        this.dirty = dirty;
        this.schrab = schrab;
        this.euph = euph;

        this.setYRot(90F);
        this.yRotO = 90F;
        this.setXRot(90F);
        this.xRotO = 90F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(NUKE_META, (byte) 0);
    }

    public int getNukeMeta() {
        return this.entityData.get(NUKE_META);
    }

    public void setNukeMeta(int meta) {
        this.entityData.set(NUKE_META, (byte) meta);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(0.98F, 0.98F);
    }

    @Override
    public void tick() {

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        setDeltaMovement(motion.x * 0.99D, Math.max(motion.y - 0.05D, -1.0D), motion.z * 0.99D);

        rotation();

        if (!level().getBlockState(BlockPos.containing(getX(), getY(), getZ())).isAir()) {
            if (!level().isClientSide()) {
                ExplosionNukeCustom.explodeCustom(
                        level(), getX(), getY(), getZ(), tnt, nuke, hydro, amat, dirty, schrab,
                        euph);
                discard();
            }
        }
    }

    public void rotation() {
        this.xRotO = getXRot();

        if (getXRot() > -75F) setXRot(getXRot() - 2F);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        tnt = input.getFloatOr("tnt", 0F);
        nuke = input.getFloatOr("nuke", 0F);
        hydro = input.getFloatOr("hydro", 0F);
        amat = input.getFloatOr("amat", 0F);
        dirty = input.getFloatOr("dirty", 0F);
        schrab = input.getFloatOr("schrab", 0F);
        euph = input.getFloatOr("euph", 0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("tnt", tnt);
        output.putFloat("nuke", nuke);
        output.putFloat("hydro", hydro);
        output.putFloat("amat", amat);
        output.putFloat("dirty", dirty);
        output.putFloat("schrab", schrab);
        output.putFloat("euph", euph);
    }
}
