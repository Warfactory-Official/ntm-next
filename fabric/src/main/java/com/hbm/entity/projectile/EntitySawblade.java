// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntitySawblade extends EntityThrowableInterp {

    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(EntitySawblade.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> META =
            SynchedEntityData.defineId(EntitySawblade.class, EntityDataSerializers.INT);

    public EntitySawblade(EntityType<? extends EntitySawblade> type, Level level) {
        super(type, level);
    }

    public EntitySawblade(Level level, double x, double y, double z) {
        this(ModEntities.STRAY_SAW.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ORIENTATION, 0);
        builder.define(META, 0);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(1F, 1F);
    }

    public EntitySawblade setOrientation(int rot) {
        entityData.set(ORIENTATION, rot);
        return this;
    }

    public int getOrientation() {
        return entityData.get(ORIENTATION);
    }

    public int getMeta() {
        return entityData.get(META);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {

        if (!level().isClientSide()) {
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.SAWBLADE));
            this.discard();
        }

        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (mop instanceof EntityHitResult hit && hit.getEntity().isAlive()) {
            Entity e = hit.getEntity();
            if (level() instanceof ServerLevel server) {

                e.hurtServer(server, damageSources().source(ModDamageTypes.RUBBLE), 1000F);

                if (!e.isAlive() && e instanceof LivingEntity living) {
                    ParticleCreators.giblets(server, living, ParticleGiblet.TYPE_MEAT, 5);

                    level().playSound(
                                    null,
                                    living.getX(),
                                    living.getY(),
                                    living.getZ(),
                                    SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                                    SoundSource.HOSTILE,
                                    2.0F,
                                    0.95F + level().getRandom().nextFloat() * 0.2F);
                }
            }
        }

        if (this.tickCount > 1 && mop instanceof BlockHitResult hit) {

            int orientation = getOrientation();

            if (orientation < 6) {

                if (getDeltaMovement().length() < 0.75D) {
                    orientation += 6;
                    setOrientation(orientation);
                } else {

                    Direction side = hit.getDirection();
                    Vec3 motion = getDeltaMovement();
                    setDeltaMovement(
                            motion.x * (1 - Math.abs(side.getStepX()) * 2),
                            motion.y * (1 - Math.abs(side.getStepY()) * 2),
                            motion.z * (1 - Math.abs(side.getStepZ()) * 2));

                    level().explode(
                                    this,
                                    getX(),
                                    getY(),
                                    getZ(),
                                    3F,
                                    Level.ExplosionInteraction.NONE);

                    BlockPos pos = hit.getBlockPos();

                    if (level().getBlockState(pos).getBlock().getExplosionResistance() < 50) {
                        if (level() instanceof ServerLevel server) {
                            server.destroyBlock(pos, false);
                        }
                    }
                }
            }

            if (orientation >= 6) {
                setDeltaMovement(Vec3.ZERO);
                this.inGround = true;
            }
        }
    }

    @Override
    public void tick() {

        if (!level().isClientSide()) {
            int orientation = this.getOrientation();
            if (orientation >= 6 && !this.inGround) {
                this.setOrientation(orientation - 6);
            }
        }

        super.tick();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return true;
    }

    @Override
    public double getGravityVelocity() {
        return this.inGround ? 0 : 0.03D;
    }

    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("rot", this.getOrientation());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setOrientation(input.getIntOr("rot", 0));
    }
}
