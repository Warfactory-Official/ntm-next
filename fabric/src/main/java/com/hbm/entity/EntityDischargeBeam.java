// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.sound.ModSounds;
import com.hbm.util.DamageClass;
import java.util.Optional;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityDischargeBeam extends Entity {

    private double headingX;
    private double headingY;
    private double headingZ;

    public EntityDischargeBeam(EntityType<? extends EntityDischargeBeam> type, Level level) {
        super(type, level);
    }

    public static void explodeDischarge(Level level, double x, double y, double z) {
        new ExplosionVNT(level, x, y, z, 5F)
                .setEntityProcessor(
                        new EntityProcessorCrossSmooth(1D, 20F)
                                .setDamageClass(DamageClass.ELECTRIC))
                .setPlayerProcessor(new PlayerProcessorStandard())
                .setSFX(new ExplosionEffectStandard())
                .explode();
        level.playSound(
                null,
                x,
                y,
                z,
                ModSounds.UFO_BLAST.get(),
                SoundSource.BLOCKS,
                5F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
    }

    public void setRotationsFromVector(double dx, double dy, double dz) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float pitch = (float) (-Math.asin(dy / len) * 180D / Math.PI);
        float yaw = (float) (-Math.atan2(dx, dz) * 180D / Math.PI);
        setXRot(pitch);
        setYRot(yaw);
        float y = yaw / 180F * Mth.PI;
        float p = pitch / 180F * Mth.PI;
        headingX = -Mth.sin(y) * Mth.cos(p);
        headingZ = Mth.cos(y) * Mth.cos(p);
        headingY = -Mth.sin(p);
    }

    public void performHitscan(double range) {
        if (!(level() instanceof ServerLevel)) return;

        Vec3 start = position();
        Vec3 end = start.add(headingX * range, headingY * range, headingZ * range);

        BlockHitResult blockHit =
                level().clip(
                                new ClipContext(
                                        start,
                                        end,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));
        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        Vec3 sweepEnd = hitBlock ? blockHit.getLocation() : end;

        Entity hit = null;
        Vec3 hitVec = null;
        double nearestSqr = 0D;
        AABB sweep =
                getBoundingBox()
                        .expandTowards(headingX * range, headingY * range, headingZ * range)
                        .inflate(1D);
        for (Entity e : level().getEntities(this, sweep)) {
            if (!e.isAlive() || !e.isPickable()) continue;
            Optional<Vec3> clip = e.getBoundingBox().inflate(0.3D).clip(start, sweepEnd);
            if (clip.isPresent()) {
                double distSqr = start.distanceToSqr(clip.get());
                if (hit == null || distSqr < nearestSqr) {
                    hit = e;
                    nearestSqr = distSqr;
                    hitVec = clip.get();
                }
            }
        }

        if (hit != null) {
            explodeDischarge(level(), hitVec.x, hitVec.y, hitVec.z);
        } else if (hitBlock) {
            level().destroyBlock(blockHit.getBlockPos(), false);
            Vec3 at = blockHit.getLocation();
            explodeDischarge(level(), at.x, at.y, at.z);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > 3) discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
