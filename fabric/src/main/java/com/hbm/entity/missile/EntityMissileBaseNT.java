// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.client.ClientEffects;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.special.Autogen;
import com.hbm.items.special.MaterialShapeItem;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.util.ChunkUtil;
import java.util.List;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class EntityMissileBaseNT extends Projectile implements IRadarDetectableNT {

    private static final EntityDataAccessor<Byte> FACING =
            SynchedEntityData.defineId(EntityMissileBaseNT.class, EntityDataSerializers.BYTE);

    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    public int startX;
    public int startZ;
    public int targetX;
    public int targetZ;
    public double velocity;
    public double decelY;
    public double accelXZ;
    public boolean isCluster = false;
    public int health = 50;

    public float renderPitch;
    public float renderPitchO;
    public float renderYaw;
    public float renderYawO;

    public EntityMissileBaseNT(EntityType<? extends EntityMissileBaseNT> type, Level level) {
        super(type, level);
    }

    public void launch(double x, double y, double z, int targetX, int targetZ) {
        setPos(x, y, z);
        this.startX = (int) x;
        this.startZ = (int) z;
        this.targetX = targetX;
        this.targetZ = targetZ;
        this.setDeltaMovement(0, 2, 0);

        Vec3 vector = new Vec3(targetX - startX, 0, targetZ - startZ);
        this.accelXZ = this.decelY = 1 / vector.length();
        this.decelY *= 2;
        this.velocity = 0;

        this.setYRot((float) (Math.atan2(targetX - getX(), targetZ - getZ()) * 180.0D / Math.PI));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FACING, (byte) 5);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    public byte getFacing() {
        return entityData.get(FACING);
    }

    public void setFacing(int facing) {
        entityData.set(FACING, (byte) facing);
    }

    public boolean hasPropulsion() {
        return true;
    }

    protected float getContrailScale() {
        return 1F;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {

        Vec3 prePos = position();
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) {
            Vec3 step = position().subtract(prePos);
            updateRenderRotation(step);
            spawnContrail(step);
            return;
        }

        double mult = velocity;
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement().scale(mult));

        BlockHitResult mop =
                level().clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));
        if (mop.getType() != HitResult.Type.MISS) {
            onMissileImpact(mop);
            discard();
            return;
        }
        setPos(to.x, to.y, to.z);

        if (velocity < 4) velocity += Mth.clamp(this.tickCount / 60D * 0.05D, 0, 0.05);

        Vec3 motion = getDeltaMovement();
        if (hasPropulsion()) {
            double motY = motion.y - decelY * velocity;
            Vec3 dir = new Vec3(targetX - startX, 0, targetZ - startZ).normalize();
            double motX = motion.x;
            double motZ = motion.z;
            if (motY > 0) {
                motX += dir.x * accelXZ * velocity;
                motZ += dir.z * accelXZ * velocity;
            }
            if (motY < 0) {
                motX -= dir.x * accelXZ * velocity;
                motZ -= dir.z * accelXZ * velocity;
            }
            setDeltaMovement(motX, motY, motZ);
        } else {
            double motY = motion.y;
            if (motY > -1.5) motY -= 0.05;
            setDeltaMovement(motion.x * 0.99, motY, motion.z * 0.99);
        }

        if (getDeltaMovement().y < -1.5 && isCluster) {
            cluster();
            discard();
            return;
        }

        setYRot((float) (Math.atan2(targetX - getX(), targetZ - getZ()) * 180.0D / Math.PI));
        Vec3 flight = getDeltaMovement();
        float horiz = (float) Math.sqrt(flight.x * flight.x + flight.z * flight.z);
        this.xRot = (float) (Math.atan2(flight.y, horiz) * 180.0D / Math.PI) - 90F;

        ChunkUtil.holdOwnChunk(this);
    }

    private void updateRenderRotation(Vec3 step) {
        this.renderPitchO = this.renderPitch;
        this.renderYawO = this.renderYaw;
        float horiz = Mth.sqrt((float) (step.x * step.x + step.z * step.z));
        if (step.lengthSqr() > 1.0e-8) {
            this.renderPitch = (float) (Math.atan2(step.y, horiz) * 180.0D / Math.PI) - 90F;
        }
        if (horiz > 1.0e-3F) {
            this.renderYaw = (float) (Math.atan2(step.x, step.z) * 180.0D / Math.PI);
        }
    }

    protected void spawnContrail(Vec3 step) {
        spawnContrailWithOffset(step, 0, 0, 0);
    }

    protected void spawnContrailWithOffset(
            Vec3 step, double offsetX, double offsetY, double offsetZ) {
        double len = step.length();
        Vec3 back = step.lengthSqr() < 1.0e-8 ? Vec3.ZERO : step.normalize().scale(-1);

        Vec3 thrust = noseAxis();
        int count = (int) Math.max(Math.min(len, 10), 1);
        for (int i = 0; i < count; i++) {
            double d = len - i;
            ClientEffects.spawnRocketFlame(
                    level(),
                    getX() + back.x * d + offsetX,
                    getY() + back.y * d + offsetY,
                    getZ() + back.z * d + offsetZ,
                    getContrailScale(),
                    -thrust.x,
                    -thrust.y,
                    -thrust.z,
                    60 + random.nextInt(20));
        }
    }

    protected Vec3 noseAxis() {
        return new Vec3(0, 1, 0)
                .zRot(renderPitch * (float) Math.PI / 180F)
                .yRot((renderYaw + 90F) * (float) Math.PI / 180F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableToBase(source)) return false;

        if (health > 0) {
            health -= amount;
            if (health <= 0) killMissile();
        }
        return true;
    }

    protected void killMissile() {
        if (isRemoved()) return;
        Vec3 motion = getDeltaMovement();
        double x = getX(), y = getY(), z = getZ();
        discard();
        ExplosionLarge.explode(level(), x, y, z, 5F, true, false, true);
        ExplosionLarge.spawnShrapnelShower(
                level(), x, y, z, motion.x, motion.y, motion.z, 15, 0.075);
        ExplosionLarge.spawnMissileDebris(
                level(),
                x,
                y,
                z,
                motion.x,
                motion.y,
                motion.z,
                0.25,
                getDebris(),
                getDebrisRareDrop());
    }

    public List<ItemStack> getDebris() {
        return List.of();
    }

    protected static ItemStack autogen(MaterialShapes shape, NTMMaterial mat, int count) {
        MaterialShapeItem item = Autogen.find(shape, mat);
        if (item == null) throw new IllegalStateException("no autogen item for " + mat.tagPath);
        return new ItemStack(item, count);
    }

    public ItemStack getDebrisRareDrop() {
        return ItemStack.EMPTY;
    }

    public void explodeStandard(float strength, int resolution, boolean fire) {
        BlockProcessorStandard bp = new BlockProcessorStandard().setNoDrop();
        if (fire) bp.withBlockEffect(new BlockMutatorFire());
        new ExplosionVNT(level(), getX(), getY(), getZ(), strength)
                .setBlockAllocator(new BlockAllocatorStandard(resolution))
                .setBlockProcessor(bp)
                .setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(2F))
                .setPlayerProcessor(new PlayerProcessorStandard())
                .explode();
    }

    public abstract void onMissileImpact(BlockHitResult mop);

    public abstract ItemStack getMissileItemForInfo();

    public void cluster() {}

    @Override
    public String getRadarName() {
        ItemStack item = this.getMissileItemForInfo();
        if (item != null && item.getItem() instanceof ItemMissile missile) {
            return switch (missile.tier) {
                case TIER0 -> "radar.target.tier0";
                case TIER1 -> "radar.target.tier1";
                case TIER2 -> "radar.target.tier2";
                case TIER3 -> "radar.target.tier3";
                case TIER4 -> "radar.target.tier4";
            };
        }
        return "Unknown";
    }

    @Override
    public int getBlipLevel() {
        ItemStack item = this.getMissileItemForInfo();
        if (item != null && item.getItem() instanceof ItemMissile missile) {
            return switch (missile.tier) {
                case TIER0 -> IRadarDetectableNT.TIER0;
                case TIER1 -> IRadarDetectableNT.TIER1;
                case TIER2 -> IRadarDetectableNT.TIER2;
                case TIER3 -> IRadarDetectableNT.TIER3;
                case TIER4 -> IRadarDetectableNT.TIER4;
            };
        }
        return IRadarDetectableNT.SPECIAL;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanMissiles;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return !(params.smartMode && this.getDeltaMovement().y >= 0);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        startX = input.getIntOr("sX", startX);
        startZ = input.getIntOr("sZ", startZ);
        targetX = input.getIntOr("tX", targetX);
        targetZ = input.getIntOr("tZ", targetZ);
        velocity = input.getDoubleOr("veloc", velocity);
        decelY = input.getDoubleOr("decel", decelY);
        accelXZ = input.getDoubleOr("accel", accelXZ);
        health = input.getIntOr("health", health);
        isCluster = input.getBooleanOr("cluster", isCluster);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("sX", startX);
        output.putInt("sZ", startZ);
        output.putInt("tX", targetX);
        output.putInt("tZ", targetZ);
        output.putDouble("veloc", velocity);
        output.putDouble("decel", decelY);
        output.putDouble("accel", accelXZ);
        output.putInt("health", health);
        output.putBoolean("cluster", isCluster);
    }
}
