// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.data.MobData;
import com.hbm.entity.mob.ai.EntityAIBreaking;
import com.hbm.items.ModItems;
import com.hbm.platform.Services;
import com.hbm.tags.HbmBlockTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityFBI extends Monster implements RangedAttackMob {

    public EntityFBI(EntityType<? extends EntityFBI> type, Level level) {
        super(type, level);
        getNavigation().setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ARMOR, 20D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new EntityAIBreaking(this));

        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 20, 25, 15.0F));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {

        if (source.getDirectEntity() != null
                && source.getEntity() instanceof EntityFBI
                && source.getDirectEntity() != source.getEntity()) {
            return false;
        }

        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        equipRaidLoadout();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    public void equipRaidLoadout() {
        setItemSlot(
                EquipmentSlot.MAINHAND,
                random.nextBoolean()
                        ? ModItems.GUN_HEAVY_REVOLVER.get().getDefaultInstance()
                        : ModItems.GUN_SPAS12.get().getDefaultInstance());

        if (random.nextInt(5) == 0) {
            setItemSlot(EquipmentSlot.HEAD, ModItems.SECURITY_HELMET.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.CHEST, ModItems.SECURITY_PLATE.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.LEGS, ModItems.SECURITY_LEGS.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.FEET, ModItems.SECURITY_BOOTS.get().getDefaultInstance());
        }

        if (level().dimension() != Level.OVERWORLD) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GLASS));
            setItemSlot(EquipmentSlot.CHEST, ModItems.PAA_PLATE.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.LEGS, ModItems.PAA_LEGS.get().getDefaultInstance());
            setItemSlot(EquipmentSlot.FEET, ModItems.PAA_BOOTS.get().getDefaultInstance());
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {}

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            setItemSlot(EquipmentSlot.HEAD, ModItems.GAS_MASK_M65.get().getDefaultInstance());
        }
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = getTarget();

        if (target == null) {
            target =
                    level.getNearestPlayer(
                            getX(),
                            getY(),
                            getZ(),
                            128.0D,
                            entity -> !((Player) entity).getAbilities().invulnerable);
            if (target != null) setTarget(target);
        }

        if (target != null) {
            Vec3 bearing =
                    new Vec3(target.getX() - getX(), target.getY() - getY(), target.getZ() - getZ())
                            .normalize()
                            .scale(16.0D);
            int x = Mth.floor(getX() + bearing.x);
            int y = Mth.floor(getY() + bearing.y);
            int z = Mth.floor(getZ() + bearing.z);

            y = snapToGround(level, x, y, z, true);
            if (y == Integer.MIN_VALUE)
                y = snapToGround(level, x, Mth.floor(getY() + bearing.y), z, false);
            if (y != Integer.MIN_VALUE) getNavigation().moveTo(x + 0.5D, y, z + 0.5D, 1.0D);
        }
    }

    private static int snapToGround(ServerLevel level, int x, int y, int z, boolean down) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int start = down ? y : y + 10;
        int end = down ? y - 10 : y;

        for (int i = start; i > end; i--) {
            pos.set(x, i, z);
            if (!level.getBlockState(pos).isSolidRender()
                    && level.getBlockState(pos.move(0, -1, 0)).isSolidRender()) {
                return i;
            }
        }

        return Integer.MIN_VALUE;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide() || getHealth() <= 0) return;

        if (tickCount % MobData.RAID_ATTACK_DELAY.get() == 0) {
            breakNearbyMachine();
        }

        double range = 1.5;
        List<ItemEntity> items =
                level().getEntitiesOfClass(
                                ItemEntity.class, getBoundingBox().inflate(range, range, range));

        for (ItemEntity item : items) item.igniteForSeconds(10);
    }

    private void breakNearbyMachine() {
        Vec3 reach =
                new Vec3(MobData.RAID_ATTACK_REACH.get(), 0, 0)
                        .yRot((float) (Math.PI * 2) * random.nextFloat());
        Vec3 from = new Vec3(getX(), getY() + 0.5 + random.nextFloat(), getZ());
        Vec3 to = from.add(reach);

        BlockHitResult hit =
                level().clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this));

        if (hit.getType() != HitResult.Type.BLOCK || !(level() instanceof ServerLevel server))
            return;
        BlockState state = server.getBlockState(hit.getBlockPos());
        if (state.is(HbmBlockTags.FBI_BREAKABLE)
                && Services.PLATFORM.canEntityDestroyBlock(
                        server, hit.getBlockPos(), state, this)) {
            level().destroyBlock(hit.getBlockPos(), false);
        }
    }
}
