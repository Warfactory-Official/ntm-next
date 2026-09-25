// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.*;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.packet.toclient.MukePayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityBombletZeta extends Entity {

    private int type;

    public float renderPitch;
    public float renderPitchO;

    public EntityBombletZeta(EntityType<? extends EntityBombletZeta> type, Level level) {
        super(type, level);
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        type = input.getIntOr("type", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("type", type);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000D;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {

        renderPitchO = renderPitch;
        yRotO = getYRot();
        xo = getX();
        yo = getY();
        zo = getZ();
        super.tick();
        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        setDeltaMovement(motion.x * 0.99D, motion.y - 0.05D, motion.z * 0.99D);
        rotation();
        BlockPos block = new BlockPos((int) getX(), (int) getY(), (int) getZ());
        if (level().getBlockState(block).isAir() || level().isClientSide()) return;
        if (type == 0) {
            ExplosionVNT vnt =
                    new ExplosionVNT(level(), getX() + 0.5F, getY() + 1.5F, getZ() + 0.5F, 4F);
            vnt.setBlockAllocator(new BlockAllocatorStandard());
            vnt.setBlockProcessor(new BlockProcessorStandard());
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 100));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
            vnt.explode();
        }
        if (type == 1) {
            ExplosionVNT vnt =
                    new ExplosionVNT(level(), getX() + 0.5F, getY() + 1.5F, getZ() + 0.5F, 4F);
            vnt.setBlockAllocator(new BlockAllocatorStandard());
            vnt.setBlockProcessor(
                    new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 100));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(15, 5F, 1.75F));
            vnt.explode();
        }
        if (type == 2) {
            level().playSound(
                            null,
                            getX() + 0.5D,
                            getY() + 0.5D,
                            getZ() + 0.5D,
                            SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.PLAYERS,
                            5F,
                            2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
            EntityMist mist = new EntityMist(level()).setType(NTMFluids.CHLORINE).setArea(15, 7.5F);
            mist.setPos(
                    getX() - getDeltaMovement().x,
                    getY() - getDeltaMovement().y,
                    getZ() - getDeltaMovement().z);
            level().addFreshEntity(mist);
        }

        if (type == 4) {
            level().addFreshEntity(
                            EntityNukeExplosionMK5.statFac(
                                    level(),
                                    (int) (ExplosionData.FATMAN_RADIUS.get() * 1.5F),
                                    getX(),
                                    getY(),
                                    getZ()));
            if (level() instanceof ServerLevel server) {
                Services.NETWORK.sendToAllAround(
                        new MukePayload(
                                getX(), getY() + 0.5D, getZ(), false, random.nextInt(100) == 0),
                        new TargetPoint(server, getX(), getY(), getZ(), 250));
            }
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            ModSounds.GUN_MINI_NUKE_EXPLOSION.get(),
                            SoundSource.BLOCKS,
                            15F,
                            1F);
        }
        discard();
    }

    public void rotation() {
        Vec3 motion = getDeltaMovement();
        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180D / Math.PI);
        renderPitch = (float) (Math.atan2(motion.y, horizontal) * 180D / Math.PI) - 90F;
        while (renderPitch - renderPitchO < -180F) renderPitchO -= 360F;
        while (renderPitch - renderPitchO >= 180F) renderPitchO += 360F;
        while (yaw - yRotO < -180F) yRotO -= 360F;
        while (yaw - yRotO >= 180F) yRotO += 360F;
        setYRot(yaw);
    }
}
