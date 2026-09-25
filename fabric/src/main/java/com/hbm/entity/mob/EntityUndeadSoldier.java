// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class EntityUndeadSoldier extends Monster {

    public static final byte TYPE_ZOMBIE = 0;
    public static final byte TYPE_SKELETON = 1;
    private static final EntityDataAccessor<Byte> TYPE =
            SynchedEntityData.defineId(EntityUndeadSoldier.class, EntityDataSerializers.BYTE);

    public EntityUndeadSoldier(EntityType<? extends EntityUndeadSoldier> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 40D)
                .add(Attributes.MOVEMENT_SPEED, .25D)
                .add(Attributes.ATTACK_DAMAGE, 5D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(4, new RandomStrollGoal(this, 1D));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(
                2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
        targetSelector.addGoal(
                3, new NearestAttackableTargetGoal<>(this, Villager.class, 0, true, false, null));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, TYPE_ZOMBIE);
    }

    public byte soldierType() {
        return entityData.get(TYPE);
    }

    public boolean canSpawnAtCurrentPosition() {
        return level().getDifficulty() != Difficulty.PEACEFUL
                && level().noCollision(this, getBoundingBox())
                && !level().containsAnyLiquid(getBoundingBox());
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        equipTaurunLoadout();
        entityData.set(TYPE, random.nextBoolean() ? TYPE_ZOMBIE : TYPE_SKELETON);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    public void equipTaurunLoadout() {
        setItemSlot(EquipmentSlot.HEAD, ModItems.TAURUN_HELMET.get().getDefaultInstance());
        setItemSlot(EquipmentSlot.CHEST, ModItems.TAURUN_PLATE.get().getDefaultInstance());
        setItemSlot(EquipmentSlot.LEGS, ModItems.TAURUN_LEGS.get().getDefaultInstance());
        setItemSlot(EquipmentSlot.FEET, ModItems.TAURUN_BOOTS.get().getDefaultInstance());
        switch (random.nextInt(5)) {
            case 0 ->
                    setItemSlot(
                            EquipmentSlot.MAINHAND,
                            ModItems.GUN_HEAVY_REVOLVER.get().getDefaultInstance());
            case 1 ->
                    setItemSlot(
                            EquipmentSlot.MAINHAND,
                            ModItems.GUN_LIGHT_REVOLVER.get().getDefaultInstance());
            case 2 ->
                    setItemSlot(
                            EquipmentSlot.MAINHAND,
                            ModItems.GUN_CARBINE.get().getDefaultInstance());
            case 3 ->
                    setItemSlot(
                            EquipmentSlot.MAINHAND,
                            ModItems.GUN_MARESLEG.get().getDefaultInstance());
            case 4 ->
                    setItemSlot(
                            EquipmentSlot.MAINHAND,
                            ModItems.GUN_GREASEGUN.get().getDefaultInstance());
            default -> throw new IllegalStateException();
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0F);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(
                TYPE,
                input.getByteOr("type", TYPE_ZOMBIE) == TYPE_SKELETON
                        ? TYPE_SKELETON
                        : TYPE_ZOMBIE);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("type", soldierType());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return soldierType() == TYPE_SKELETON
                ? SoundEvents.SKELETON_AMBIENT
                : SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return soldierType() == TYPE_SKELETON ? SoundEvents.SKELETON_HURT : SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return soldierType() == TYPE_SKELETON
                ? SoundEvents.SKELETON_DEATH
                : SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(
                soldierType() == TYPE_SKELETON
                        ? SoundEvents.SKELETON_STEP
                        : SoundEvents.ZOMBIE_STEP,
                .15F,
                1F);
    }
}
