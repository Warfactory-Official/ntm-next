// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.particle;

import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionNukeGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityOrangeFX extends Entity {

    private int particleAge;
    private int maxAge;

    public EntityOrangeFX(EntityType<? extends EntityOrangeFX> type, Level level) {
        super(type, level);
        noPhysics = false;
    }

    public EntityOrangeFX(
            Level level,
            double x,
            double y,
            double z,
            double motionX,
            double motionY,
            double motionZ) {
        this(ModEntities.AGENT_ORANGE.get(), level);
        setPos(x, y, z);
        setDeltaMovement(ModFXMotion.spawn(motionX, motionY, motionZ));
    }

    public int particleAge() {
        return particleAge;
    }

    public int maxAge() {
        return maxAge;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    public void tick() {
        xo = getX();
        yo = getY();
        zo = getZ();
        super.tick();
        if (maxAge < 900) maxAge = random.nextInt(301) + 900;
        if (!level().isClientSide() && random.nextInt(50) == 0)
            ExplosionChaos.poison(level(), getX(), getY(), getZ(), 2D);
        if (++particleAge >= maxAge) {
            discard();
            return;
        }
        double motionX = getDeltaMovement().x * 0.86D;
        double motionY = getDeltaMovement().y * 0.86D - 0.1D;
        double motionZ = getDeltaMovement().z * 0.86D;
        setDeltaMovement(motionX, motionY, motionZ);
        for (int i = 0; i < 4; i++) {
            setPos(getX() + motionX / 4D, getY() + motionY / 4D, getZ() + motionZ / 4D);
            BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ());
            if (level().getBlockState(pos).isAir()) continue;
            discard();
            for (int x = -1; x < 2; x++)
                for (int y = -1; y < 2; y++)
                    for (int z = -1; z < 2; z++) {
                        BlockPos affected = pos.offset(x, y, z);
                        if (level().getBlockState(affected).is(Blocks.GRASS_BLOCK)) {
                            level().setBlock(affected, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                        } else {
                            ExplosionNukeGeneric.solinium(level(), affected);
                        }
                    }
        }
    }
}
