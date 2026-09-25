// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.client.ClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.interfaces.StoredItems;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityBobmazon extends Entity implements StoredItems {

    private static final EntityDataAccessor<Integer> STATUS =
            SynchedEntityData.defineId(EntityBobmazon.class, EntityDataSerializers.INT);

    public ItemStack payload = ItemStack.EMPTY;

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (visitor.visit(payload) && payload.isEmpty()) payload = ItemStack.EMPTY;
    }

    public EntityBobmazon(EntityType<? extends EntityBobmazon> type, Level level) {
        super(type, level);
    }

    public EntityBobmazon(Level level) {
        this(ModEntities.BOBMAZON_DELIVERY.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATUS, 0);
    }

    public int getStatus() {
        return entityData.get(STATUS);
    }

    public void setStatus(int status) {
        entityData.set(STATUS, status);
    }

    @Override
    public void tick() {

        setDeltaMovement(0, -0.5, 0);

        for (int i = 0; i < 4; i++) {

            BlockPos nose =
                    new BlockPos((int) (getX() - 0.5), (int) (getY() + 1), (int) (getZ() - 0.5));
            if (!level().getBlockState(nose).isAir()
                    && !level().isClientSide()
                    && getStatus() != 1) {
                ExplosionLarge.spawnParticles(level(), getX(), getY() + 1, getZ(), 50);

                level().playSound(
                                null,
                                getX(),
                                getY(),
                                getZ(),
                                ModSounds.OLD_EXPLOSION.get(),
                                SoundSource.AMBIENT,
                                10.0F,
                                0.5F + random.nextFloat() * 0.1F);

                if (!payload.isEmpty()) {

                    ItemEntity pack = new ItemEntity(level(), getX(), getY() + 2, getZ(), payload);
                    pack.setDeltaMovement(0, pack.getDeltaMovement().y, 0);
                    level().addFreshEntity(pack);
                }

                discard();

                break;
            }

            this.setPos(
                    getX() + getDeltaMovement().x,
                    getY() + getDeltaMovement().y,
                    getZ() + getDeltaMovement().z);
        }

        if (level().isClientSide()) {

            ClientEffects.spawnRocketFlame(
                    level(), getX(), getY() + 1, getZ(), 1F, 0D, 0D, 0D, 300 + random.nextInt(50));
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.payload = input.read("payload", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {

        if (!payload.isEmpty()) output.store("payload", ItemStack.OPTIONAL_CODEC, payload);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }
}
