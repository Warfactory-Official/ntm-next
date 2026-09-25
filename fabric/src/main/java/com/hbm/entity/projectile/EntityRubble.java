// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class EntityRubble extends ThrowableProjectile {

    private static final EntityDataAccessor<Integer> STATE_ID =
            SynchedEntityData.defineId(EntityRubble.class, EntityDataSerializers.INT);

    public EntityRubble(EntityType<? extends EntityRubble> type, Level level) {
        super(type, level);
    }

    public EntityRubble(Level level, double x, double y, double z) {
        this(ModEntities.RUBBLE.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE_ID, Block.getId(Blocks.STONE.defaultBlockState()));
    }

    public BlockState getBlockState() {
        return Block.stateById(entityData.get(STATE_ID));
    }

    public void setBlockState(BlockState state) {
        entityData.set(STATE_ID, Block.getId(state));
    }

    @Override
    protected float getAirDrag() {
        return 1F;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel server) {
            result.getEntity()
                    .hurtServer(server, damageSources().source(ModDamageTypes.RUBBLE), 15F);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (level() instanceof ServerLevel server && tickCount > 2) {
            server.playSound(
                    null,
                    getX(),
                    getY(),
                    getZ(),
                    ModSounds.BLOCK_DEBRIS.get(),
                    SoundSource.BLOCKS,
                    1.5F,
                    1.0F);

            server.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, getBlockState()),
                    getX(),
                    getY(),
                    getZ(),
                    64,
                    0.25D,
                    0.25D,
                    0.25D,
                    0.0D);
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("blockState", entityData.get(STATE_ID));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(
                STATE_ID,
                input.getIntOr("blockState", Block.getId(Blocks.STONE.defaultBlockState())));
    }
}
