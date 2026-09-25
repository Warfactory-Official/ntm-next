// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.advancement.BossKilledTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.mob.ai.EntityAIMaskmanCasualApproach;
import com.hbm.entity.mob.ai.EntityAIMaskmanLasergun;
import com.hbm.entity.mob.ai.EntityAIMaskmanMinigun;
import com.hbm.handler.ArmorUtil;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class EntityMaskMan extends Monster implements IRadiationImmune {

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(
                    getUUID(),
                    getDisplayName(),
                    BossEvent.BossBarColor.RED,
                    BossEvent.BossBarOverlay.PROGRESS);

    private float lastHealth;

    public EntityMaskMan(EntityType<? extends EntityMaskMan> type, Level level) {
        super(type, level);
        this.xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new EntityAIMaskmanCasualApproach(this, 1.0D, false));
        this.goalSelector.addGoal(2, new EntityAIMaskmanMinigun(this, 3));
        this.goalSelector.addGoal(3, new EntityAIMaskmanLasergun(this));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (source.getDirectEntity() instanceof ThrownEgg && this.random.nextInt(10) == 0) {
            this.xpReward = 0;
            setHealth(0F);
            return true;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) amount = 0F;
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) amount = 0F;
        if (source.is(DamageTypeTags.IS_PROJECTILE)) amount *= 0.5F;
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;

        if (amount > 50F) amount = 50F + (amount - 50F) * 0.5F;

        return super.hurtServer(level, source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (level() instanceof ServerLevel server) {

            if (this.lastHealth >= getMaxHealth() / 2F
                    && getHealth() < getMaxHealth() / 2F
                    && isAlive()) {
                this.lastHealth = getHealth();
                server.explode(
                        this, getX(), getY() + 4, getZ(), 2.5F, Level.ExplosionInteraction.BLOCK);
            }

            this.lastHealth = getHealth();

            bossEvent.setProgress(getHealth() / getMaxHealth());
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = getTarget();
        if (target == null) return;

        getLookControl().setLookAt(target, 30F, 30F);
        setYRot(getYHeadRot());
        this.yBodyRot = getYRot();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        HbmCriteria.bossKilled(this, BossKilledTrigger.Kind.MASKMAN, 100D);
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
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        ItemStack mask = new ItemStack(ModItems.GAS_MASK_M65.get());
        ArmorUtil.installGasMaskFilter(mask, new ItemStack(ModItems.GAS_MASK_FILTER_COMBO.get()));

        spawnAtLocation(level, mask);
        spawnAtLocation(level, new ItemStack(ModItems.COIN_MASKMAN.get()));
        spawnAtLocation(level, new ItemStack(ModItems.BOTTLED_CLOUD.get()));
        spawnAtLocation(level, new ItemStack(Items.SKELETON_SKULL));
    }
}
