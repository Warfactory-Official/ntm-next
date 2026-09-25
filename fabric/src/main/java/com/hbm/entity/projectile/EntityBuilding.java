// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityBuilding extends Entity {

    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    public EntityBuilding(EntityType<? extends EntityBuilding> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) return;

        if (this.tickCount == 1) {
            for (int i = 0; i < 100; i++) {
                FlameCreator.composeEffect(
                        level(),
                        getX() + (random.nextDouble() - 0.5) * 15,
                        getY() + (random.nextDouble() - 0.5) * 15,
                        getZ() + (random.nextDouble() - 0.5) * 15,
                        FlameCreator.META_BALEFIRE);
            }
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        double mY = motion.y - 0.03;
        if (mY < -1.5) mY = -1.5;
        setDeltaMovement(motion.x, mY, motion.z);

        if (!level().getBlockState(blockPosition()).isAir()) {
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            ModSounds.OLD_EXPLOSION.get(),
                            SoundSource.AMBIENT,
                            10000.0F,
                            0.5F + this.random.nextFloat() * 0.1F);
            discard();
            ExplosionLarge.spawnParticles(level(), getX(), getY() + 3, getZ(), 150);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 6);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 4);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);

            List<Entity> list =
                    level().getEntities(
                                    null,
                                    new AABB(
                                            getX() - 8,
                                            getY() - 8,
                                            getZ() - 8,
                                            getX() + 8,
                                            getY() + 8,
                                            getZ() + 8));
            if (level() instanceof ServerLevel server) {
                for (Entity e : list)
                    e.hurtServer(
                            server, level().damageSources().source(ModDamageTypes.BUILDING), 1000);
            }

            for (int i = 0; i < 250; i++) {

                double pitch = this.random.nextFloat() * Math.PI / 2;
                double yaw = this.random.nextFloat() * Math.PI * 2;

                EntityRubble rubble = new EntityRubble(level(), getX(), getY() + 3, getZ());
                rubble.setBlockState(Blocks.BRICKS.defaultBlockState());
                rubble.setDeltaMovement(
                        Math.cos(pitch) * Math.cos(yaw),
                        Math.sin(pitch),
                        -Math.cos(pitch) * Math.sin(yaw));
                level().addFreshEntity(rubble);
            }
        }
    }
}
