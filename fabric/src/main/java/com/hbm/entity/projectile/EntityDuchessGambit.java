// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.sound.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityDuchessGambit extends Entity {

    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    public EntityDuchessGambit(EntityType<? extends EntityDuchessGambit> type, Level level) {
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
            for (int i = 0; i < 50; i++) {
                FlameCreator.composeEffect(
                        level(),
                        getX() + (random.nextDouble() - 0.5) * 5,
                        getY() + (random.nextDouble() - 0.5) * 7,
                        getZ() + (random.nextDouble() - 0.5) * 20,
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
                            ModSounds.ALARM_GAMBIT.get(),
                            SoundSource.RECORDS,
                            10000.0F,
                            1F);
            discard();

            List<Entity> list =
                    level().getEntities(
                                    null,
                                    new AABB(
                                            getX() - 5,
                                            getY() - 2,
                                            getZ() - 9,
                                            getX() + 5,
                                            getY() + 2,
                                            getZ() + 9));
            if (level() instanceof ServerLevel server) {
                for (Entity e : list)
                    e.hurtServer(server, level().damageSources().source(ModDamageTypes.BOAT), 1000);
            }

            ExplosionLarge.explode(level(), getX(), getY(), getZ() - 6, 2, true, false, false);
            ExplosionLarge.explode(level(), getX(), getY(), getZ() - 3, 2, true, false, false);
            ExplosionLarge.explode(level(), getX(), getY(), getZ(), 2, true, false, false);
            ExplosionLarge.explode(level(), getX(), getY(), getZ() + 3, 2, true, false, false);
            ExplosionLarge.explode(level(), getX(), getY(), getZ() + 6, 2, true, false, false);

            level().setBlockAndUpdate(
                            new BlockPos(
                                    (int) (getX() - 0.5),
                                    (int) (getY() + 0.5),
                                    (int) (getZ() - 0.5)),
                            ModBlocks.BOAT.get().defaultBlockState());

            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 2.5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 2);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 1.5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 1);
        }
    }
}
