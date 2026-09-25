// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityChopperMine extends Entity {

    public int timer = 0;

    public Entity shooter;

    public EntityChopperMine(EntityType<? extends EntityChopperMine> type, Level level) {
        super(type, level);
    }

    public EntityChopperMine(
            Level level,
            double x,
            double y,
            double z,
            double moX,
            double moY,
            double moZ,
            Entity shooter) {
        this(ModEntities.CHOPPER_MINE.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(moX, moY, moZ);
        this.shooter = shooter;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        if (!level().isClientSide()) {

            Vec3 from = position();
            Vec3 to = from.add(getDeltaMovement());

            BlockHitResult blockHit =
                    level().clip(
                                    new ClipContext(
                                            from,
                                            to,
                                            ClipContext.Block.COLLIDER,
                                            ClipContext.Fluid.NONE,
                                            this));
            HitResult mop = blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
            if (mop != null) to = mop.getLocation();

            Entity hit = null;
            List<Entity> list =
                    level().getEntities(
                                    this,
                                    this.getBoundingBox()
                                            .expandTowards(this.getDeltaMovement())
                                            .inflate(1.0D, 1.0D, 1.0D));
            double nearest = 0.0D;

            for (int i = 0; i < list.size(); ++i) {
                Entity candidate = list.get(i);

                if (candidate.isPickable() && candidate != this.shooter) {
                    float slack = 0.3F;
                    AABB box = candidate.getBoundingBox().inflate(slack, slack, slack);
                    Vec3 hitVec = box.clip(from, to).orElse(null);

                    if (hitVec != null) {
                        double dist = from.distanceTo(hitVec);

                        if (dist < nearest || nearest == 0.0D) {
                            hit = candidate;
                            nearest = dist;
                        }
                    }
                }
            }

            if (hit != null) mop = new EntityHitResult(hit);

            if (mop instanceof EntityHitResult entityHit
                    && entityHit.getEntity() instanceof Player) {
                this.detonate();
            }

            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            ModSounds.NULL_MINE.get(),
                            SoundSource.PLAYERS,
                            10.0F,
                            1F);

            if (this.timer >= 100
                    || !level().getBlockState(
                                    new BlockPos((int) getX(), (int) getY(), (int) getZ()))
                            .isAir()) {
                this.detonate();
            }
        }

        Vec3 motion = getDeltaMovement();
        if (motion.y > -0.85D) motion = new Vec3(motion.x, motion.y - 0.05D, motion.z);
        motion = new Vec3(motion.x * 0.9D, motion.y, motion.z * 0.9D);
        this.setDeltaMovement(motion);

        this.setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        this.timer++;
    }

    private void detonate() {
        level().explode(
                        this.shooter,
                        null,
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        5F,
                        false,
                        Level.ExplosionInteraction.NONE);
        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
