// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.items.EnumGearType;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityCog extends EntityThrowableInterp {

    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(EntityCog.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> META =
            SynchedEntityData.defineId(EntityCog.class, EntityDataSerializers.INT);

    private static final double SETTLE_SPEED = 0.75D;
    private static final int LANDED = 6;

    private static final float BREAKABLE_RESISTANCE = 50F;

    public EntityCog(EntityType<? extends EntityCog> type, Level level) {
        super(type, level);
    }

    public EntityCog(Level level, double x, double y, double z) {
        this(ModEntities.COG.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ORIENTATION, 0);
        builder.define(META, 0);
    }

    public EntityCog setOrientation(int rot) {
        entityData.set(ORIENTATION, rot);
        return this;
    }

    public EntityCog setMeta(int meta) {
        entityData.set(META, meta);
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
            player.getInventory()
                    .placeItemBackInInventory(
                            ModItems.GEAR_LARGE.stack(EnumGearType.values()[getMeta()]));
            discard();
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (level() instanceof ServerLevel server
                && mop instanceof EntityHitResult hit
                && hit.getEntity().isAlive()) {
            Entity e = hit.getEntity();
            e.hurtServer(server, damageSources().source(ModDamageTypes.RUBBLE), 1000F);
            if (!e.isAlive() && e instanceof LivingEntity living) {
                ParticleCreators.giblets(server, living, ParticleGiblet.TYPE_MEAT, 5);
                server.playSound(
                        null,
                        living.getX(),
                        living.getY(),
                        living.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.HOSTILE,
                        2.0F,
                        0.95F + random.nextFloat() * 0.2F);
            }
        }

        if (tickCount > 1
                && level() instanceof ServerLevel server
                && mop instanceof BlockHitResult hit) {

            int orientation = entityData.get(ORIENTATION);

            if (orientation < LANDED) {

                if (getDeltaMovement().length() < SETTLE_SPEED) {
                    entityData.set(ORIENTATION, orientation + LANDED);
                    orientation += LANDED;
                } else {
                    Direction side = hit.getDirection();
                    Vec3 motion = getDeltaMovement();
                    setDeltaMovement(
                            motion.x * (1 - Math.abs(side.getStepX()) * 2),
                            motion.y * (1 - Math.abs(side.getStepY()) * 2),
                            motion.z * (1 - Math.abs(side.getStepZ()) * 2));
                    server.explode(
                            this,
                            null,
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            3F,
                            false,
                            Level.ExplosionInteraction.NONE);

                    if (server.getBlockState(hit.getBlockPos()).getBlock().getExplosionResistance()
                            < BREAKABLE_RESISTANCE) {
                        server.destroyBlock(hit.getBlockPos(), false);
                    }
                }
            }

            if (orientation >= LANDED) {
                setDeltaMovement(Vec3.ZERO);
                inGround = true;
            }
        }
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            int orientation = entityData.get(ORIENTATION);
            if (orientation >= LANDED && !inGround)
                entityData.set(ORIENTATION, orientation - LANDED);
        }
        super.tick();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public double getGravityVelocity() {
        return inGround ? 0 : 0.03D;
    }

    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("rot", getOrientation());
        output.putInt("meta", getMeta());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setOrientation(input.getIntOr("rot", 0));
        setMeta(input.getIntOr("meta", 0));
    }
}
