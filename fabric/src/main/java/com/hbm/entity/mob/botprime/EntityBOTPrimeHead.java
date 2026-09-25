// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.botprime;

import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.ai.EntityAINearestAttackableTargetNT;
import com.hbm.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityBOTPrimeHead extends EntityBOTPrimeBase {

    private static final int BODY_SEGMENTS = 74;

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    getUUID(),
                    getDisplayName(),
                    BossEvent.BossBarColor.PURPLE,
                    BossEvent.BossBarOverlay.PROGRESS);

    public EntityBOTPrimeHead(EntityType<? extends EntityBOTPrimeHead> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
        this.wasNearGround = false;
        this.attackRange = 150D;
        this.maxSpeed = 1D;
        this.fallSpeed = 0.006D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityBOTPrimeBase.createAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.15D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(
                2,
                new EntityAINearestAttackableTargetNT<>(this, Player.class, 0, false, false, null));
    }

    @Override
    public boolean getIsHead() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (!super.hurtServer(level, source, amount)) return false;
        this.dmgCooldown = 10;
        return true;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {

        setHeadID(getId());

        BlockPos at = blockPosition();

        for (int i = 0; i < BODY_SEGMENTS; i++) {
            EntityBOTPrimeBody segment =
                    ModEntities.BALLS_O_TRON_SEG.get().create(level.getLevel(), reason);
            if (segment == null) continue;
            segment.setPartNumber(i);
            segment.setPos(at.getX(), at.getY(), at.getZ());
            segment.setHeadID(getId());
            level.addFreshEntity(segment);
        }

        setPos(at.getX(), at.getY(), at.getZ());
        this.spawnPoint = at;

        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {

        updateActionState();
        super.customServerAiStep(level);
        updateHeadMovement();

        if (getHealth() < getMaxHealth() && this.tickCount % 6 == 0) {
            if (this.targetedEntity != null) {
                heal(1F);
            } else if (getLastHurtByPlayer() == null) {
                heal(4F);
            }
        }

        if (this.targetedEntity != null
                && this.targetedEntity.distanceToSqr(this) < this.attackRange * this.attackRange
                && canSeeThroughNonSolids(this.targetedEntity)) {

            this.attackCounter++;

            if (this.attackCounter == 30) {
                laserAttack(this.targetedEntity, true);
                this.attackCounter = 0;
            }

        } else {
            this.attackCounter = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 motion = getDeltaMovement();
        float hyp = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float yaw = (float) (Math.atan2(motion.x, motion.z) * 180D / Math.PI);
        float pitch = (float) (Math.atan2(motion.y, hyp) * 180D / Math.PI);
        setYRot(yaw);
        this.yRotO = yaw;
        setXRot(pitch);
        this.xRotO = pitch;

        if (!level().isClientSide()) bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.WORM, 200D);

        if (level() instanceof ServerLevel server) {
            for (Player player : server.getPlayers(p -> p.distanceToSqr(this) <= 200D * 200D)) {
                player.getInventory()
                        .placeItemBackInInventory(new ItemStack(ModItems.COIN_WORM.get()));
            }
        }
    }

    @Override
    public float getAttackStrength(Entity target) {
        return 1000F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("spawnX", this.spawnPoint.getX());
        output.putInt("spawnY", this.spawnPoint.getY());
        output.putInt("spawnZ", this.spawnPoint.getZ());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.spawnPoint =
                new BlockPos(
                        input.getIntOr("spawnX", 0),
                        input.getIntOr("spawnY", 0),
                        input.getIntOr("spawnZ", 0));
    }

    protected void updateHeadMovement() {

        double dx = this.waypointX - getX();
        double dy = this.waypointY - getY();
        double dz = this.waypointZ - getZ();
        double distSq = dx * dx + dy * dy + dz * dz;

        if (this.courseChangeCooldown-- <= 0) {

            this.courseChangeCooldown += getRandom().nextInt(5) + 2;
            double dist = Math.sqrt(distSq);

            if (getDeltaMovement().lengthSqr() < this.maxSpeed) {

                if (!isCourseTraversable()) dist *= 8D;

                double speed = getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
                setDeltaMovement(
                        getDeltaMovement()
                                .add(dx / dist * speed, dy / dist * speed, dz / dist * speed));
            }
        }

        if (!isCourseTraversable()) {
            setDeltaMovement(getDeltaMovement().subtract(0D, this.fallSpeed, 0D));
        }

        if (this.dmgCooldown > 0) this.dmgCooldown--;
        this.aggroCooldown--;

        if (getTarget() != null) {

            if (this.aggroCooldown <= 0) {
                this.targetedEntity = getTarget();
                this.aggroCooldown = 20;
            }

        } else if (this.targetedEntity == null) {
            this.waypointX = this.spawnPoint.getX() - 50 + getRandom().nextInt(100);
            this.waypointY = this.spawnPoint.getY() - 30 + getRandom().nextInt(60);
            this.waypointZ = this.spawnPoint.getZ() - 50 + getRandom().nextInt(100);
        }

        if (this.targetedEntity == null) return;
        if (this.targetedEntity.distanceToSqr(this) >= this.attackRange * this.attackRange) return;

        if (this.wasNearGround || this.canFly) {

            this.waypointX = this.targetedEntity.getX();
            this.waypointY = this.targetedEntity.getY();
            this.waypointZ = this.targetedEntity.getZ();

            if (getRandom().nextInt(80) == 0 && getY() > this.surfaceY && !isCourseTraversable()) {
                this.wasNearGround = false;
            }

        } else {
            this.waypointX = this.targetedEntity.getX();
            this.waypointY = 10D;
            this.waypointZ = this.targetedEntity.getZ();

            if (getY() < 15D) this.wasNearGround = true;
        }
    }
}
