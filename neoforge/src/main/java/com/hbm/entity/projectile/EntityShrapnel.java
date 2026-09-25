// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorLava;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;

public class EntityShrapnel extends ThrowableProjectile {

    public static final int TRAIL_FLAME = 1;

    public static final int TRAIL_VOLCANO = 2;

    public static final int TRAIL_WATZ = 3;

    public static final int TRAIL_RAD_VOLCANO = 4;

    public static final int GRACE_TICKS = 5;
    private static final EntityDataAccessor<Byte> TRAIL =
            SynchedEntityData.defineId(EntityShrapnel.class, EntityDataSerializers.BYTE);

    public EntityShrapnel(EntityType<? extends EntityShrapnel> type, Level level) {
        super(type, level);
    }

    public EntityShrapnel(Level level, double x, double y, double z) {
        this(ModEntities.SHRAPNEL.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TRAIL, (byte) 0);
    }

    public void setTrail(int trail) {
        entityData.set(TRAIL, (byte) trail);
    }

    public int getTrail() {
        return entityData.get(TRAIL);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && entityData.get(TRAIL) == TRAIL_FLAME) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel server) {
            result.getEntity()
                    .hurtServer(server, damageSources().source(ModDamageTypes.SHRAPNEL), 15F);
        }

        fizz(null);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        fizz(result.getBlockPos());
    }

    private void fizz(@Nullable BlockPos hit) {
        if (!(level() instanceof ServerLevel server) || tickCount <= GRACE_TICKS) return;

        int trail = entityData.get(TRAIL);

        if (trail == TRAIL_VOLCANO || trail == TRAIL_RAD_VOLCANO) {
            if (hit != null) erupt(server, hit, trail == TRAIL_RAD_VOLCANO);
        } else if (trail == TRAIL_WATZ) {
            if (hit != null && server.getBlockState(hit.above()).canBeReplaced()) {
                server.setBlockAndUpdate(
                        hit.above(), ModBlocks.MUD_BLOCK.get().defaultBlockState());
            }
        } else {
            server.sendParticles(
                    ParticleTypes.LAVA, getX(), getY(), getZ(), 5, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        server.playSound(
                null,
                getX(),
                getY(),
                getZ(),
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        discard();
    }

    private void erupt(ServerLevel server, BlockPos hit, boolean radioactive) {
        Block lava =
                radioactive ? ModBlocks.RAD_LAVA_BLOCK.get() : ModBlocks.VOLCANIC_LAVA_BLOCK.get();
        double motionY = getDeltaMovement().y;

        if (motionY < -0.2D) {
            if (server.getBlockState(hit.above()).canBeReplaced()) {
                server.setBlockAndUpdate(hit.above(), lava.defaultBlockState());
            }

            BlockState monoxide = ModBlocks.GAS_MONOXIDE.get().defaultBlockState();
            for (int x = hit.getX() - 1; x <= hit.getX() + 1; x++) {
                for (int y = hit.getY(); y <= hit.getY() + 2; y++) {
                    for (int z = hit.getZ() - 1; z <= hit.getZ() + 1; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (server.getBlockState(pos).isAir())
                            server.setBlockAndUpdate(pos, monoxide);
                    }
                }
            }
        }

        if (motionY > 0) {

            new ExplosionVNT(server, hit.getX() + 0.5, hit.getY() + 0.5, hit.getZ() + 0.5, 7)
                    .setBlockAllocator(new BlockAllocatorStandard())
                    .setBlockProcessor(
                            new BlockProcessorStandard()
                                    .setNoDrop()
                                    .withBlockEffect(new BlockMutatorLava(lava)))
                    .explode();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        discard();
    }
}
