// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.mob.ai.EntityAIEatBread;
import com.hbm.entity.mob.ai.EntityAIStartFlying;
import com.hbm.entity.mob.ai.EntityAIStopFlying;
import com.hbm.entity.mob.ai.EntityAISwimmingConditional;
import com.hbm.entity.mob.ai.EntityAIWanderConditional;
import com.hbm.util.FertilizerUtil;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityPigeon extends PathfinderMob implements IFlyingCreature {

    private static final EntityDataAccessor<Byte> FLYING_STATE =
            SynchedEntityData.defineId(EntityPigeon.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> FAT =
            SynchedEntityData.defineId(EntityPigeon.class, EntityDataSerializers.BYTE);

    public float fallTime;
    public float dest;
    public float prevDest;
    public float prevFallTime;
    public float offGroundTimer = 1.0F;

    public EntityPigeon(EntityType<? extends EntityPigeon> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes();
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.5F, 1.0F);
    }

    @Override
    protected void registerGoals() {
        Predicate<PathfinderMob> grounded =
                p -> ((EntityPigeon) p).getFlyingState() == STATE_WALKING;
        goalSelector.addGoal(0, new EntityAIStartFlying(this, this));
        goalSelector.addGoal(0, new EntityAIStopFlying(this, this));
        goalSelector.addGoal(
                1, new EntityAISwimmingConditional(this, m -> grounded.test((PathfinderMob) m)));
        goalSelector.addGoal(2, new EntityAIEatBread(this, 0.4D));
        goalSelector.addGoal(5, new EntityAIWanderConditional(this, 0.2D, grounded));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLYING_STATE, (byte) STATE_WALKING);
        builder.define(FAT, (byte) 0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (amount >= getMaxHealth() * 2) {
            discard();

            for (int i = 0; i < 10; i++) {
                Vec3 vec =
                        new Vec3(
                                        random.nextGaussian(),
                                        random.nextGaussian(),
                                        random.nextGaussian())
                                .normalize();

                ItemEntity feather =
                        new ItemEntity(
                                level,
                                getX() + vec.x,
                                getY() + getBbHeight() / 2D + vec.y,
                                getZ() + vec.z,
                                new ItemStack(Items.FEATHER));
                feather.setDeltaMovement(vec.scale(0.5D));
                level.addFreshEntity(feather);
            }

            return true;
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.CHICKEN_STEP.value(), 0.15F, 1.0F);
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        int looting = 0;
        if (source.getEntity() instanceof LivingEntity attacker) {
            Holder<Enchantment> lootingEnchant =
                    level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING);
            looting = EnchantmentHelper.getEnchantmentLevel(lootingEnchant, attacker);
        }

        int feathers = random.nextInt(3) + random.nextInt(1 + looting);
        for (int i = 0; i < feathers; i++) spawnAtLocation(level, new ItemStack(Items.FEATHER));

        spawnAtLocation(
                level,
                new ItemStack(isOnFire() ? Items.COOKED_CHICKEN : Items.CHICKEN, isFat() ? 3 : 1));
    }

    @Override
    public int getFlyingState() {
        return entityData.get(FLYING_STATE);
    }

    @Override
    public void setFlyingState(int state) {
        entityData.set(FLYING_STATE, (byte) state);
    }

    public boolean isFat() {
        return entityData.get(FAT) == 1;
    }

    public void setFat(boolean fat) {
        entityData.set(FAT, (byte) (fat ? 1 : 0));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (getFlyingState() == STATE_FLYING) {
            int ground =
                    level.getHeight(
                            Heightmap.Types.MOTION_BLOCKING,
                            (int) Math.floor(getX()),
                            (int) Math.floor(getZ()));
            boolean ceil = getY() - ground > 10;

            double motionY =
                    getRandom().nextGaussian() * 0.05 + (ceil ? 0 : 0.04) + (isInWater() ? 0.2 : 0);
            if (onGround()) motionY = Math.abs(motionY) + 0.1D;
            setDeltaMovement(getDeltaMovement().x, motionY, getDeltaMovement().z);

            zza = 1.5F;
            if (getRandom().nextInt(20) == 0)
                setYRot(getYRot() + (float) (getRandom().nextGaussian() * 30));

            if (isFat() && getRandom().nextInt(50) == 0) {
                level.sendParticles(
                        new BlockParticleOption(
                                ParticleTypes.BLOCK, Blocks.WOOL.white().defaultBlockState()),
                        getX(),
                        getY() + getBbHeight() / 2D,
                        getZ(),
                        3,
                        0.2D,
                        0.2D,
                        0.2D,
                        0D);

                BlockPos below = blockPosition();
                for (int i = 0; i < 25; i++) {
                    BlockPos target = below.below(i);
                    if (FertilizerUtil.fertilize(level, target, null, ItemStack.EMPTY, true)) {
                        FertilizerUtil.growthParticles(level, target);
                        break;
                    }
                }

                if (getRandom().nextInt(10) == 0) setFat(false);
            }

        } else if (!onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1D, 0.8D, 1D));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();

        prevFallTime = fallTime;
        prevDest = dest;
        dest += (onGround() ? -1 : 4) * 0.3F;

        if (dest < 0.0F) dest = 0.0F;
        if (dest > 1.0F) dest = 1.0F;

        if (!onGround() && offGroundTimer < 1.0F) offGroundTimer = 1.0F;
        offGroundTimer *= 0.9F;

        if (!onGround() && getDeltaMovement().y < 0.0D) {
            setDeltaMovement(getDeltaMovement().multiply(1D, 0.6D, 1D));
        }

        fallTime += offGroundTimer * 2.0F;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}
}
