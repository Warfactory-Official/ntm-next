// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.api.entity.IResistanceProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.data.MobData;
import com.hbm.entity.logic.EntityWaypoint;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.mob.glyphid.ai.GlyphidAttackGoal;
import com.hbm.entity.mob.glyphid.ai.GlyphidTargetGoal;
import com.hbm.entity.mob.glyphid.ai.GlyphidWanderGoal;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorGlyphidDig;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.main.ResourceManager;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.platform.Services;
import com.hbm.util.MobUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

public class EntityGlyphid extends Monster implements IResistanceProvider {

    public static final int TASK_IDLE = 0;
    public static final int TASK_RETREAT_FOR_REINFORCEMENTS = 1;
    public static final int TASK_BUILD_HIVE = 2;
    public static final int TASK_INITIATE_RETREAT = 3;
    public static final int TASK_FOLLOW = 4;
    public static final int TASK_TERRAFORM = 5;
    public static final int TASK_DIG = 6;

    public static final byte TYPE_NORMAL = 0;
    public static final byte TYPE_INFECTED = 1;
    public static final byte TYPE_RADIOACTIVE = 2;

    private static final EntityDataAccessor<Boolean> DW_WALL =
            SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DW_ARMOR =
            SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DW_SUBTYPE =
            SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BYTE);

    public boolean hasHome = false;
    public int homeX;
    public int homeY;
    public int homeZ;
    protected int currentTask = TASK_IDLE;

    protected int previousTask;
    protected @Nullable EntityWaypoint previousWaypoint;
    public int taskX;
    public int taskY;
    public int taskZ;

    public int blastSize = Math.min((int) (3 * getGlyphidScale()) / 2, 5);
    public int blastResToDig = Math.min((int) (50 * (getGlyphidScale() * 2)), 150);

    protected boolean hasWaypoint = false;
    protected @Nullable EntityWaypoint taskWaypoint = null;

    public EntityGlyphid(EntityType<? extends EntityGlyphid> type, Level level) {
        super(type, level);
        applyEntityAttributes();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getGrunt().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getGrunt().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getGrunt().damage);
    }

    public Identifier getSkin() {
        return ResourceManager.glyphid_tex;
    }

    public double getGlyphidScale() {
        return 1.0D;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DW_WALL, false);
        builder.define(DW_ARMOR, (byte) 0b11111);
        builder.define(DW_SUBTYPE, TYPE_NORMAL);
    }

    protected void applyEntityAttributes() {
        byte variant = subtype();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(GlyphidStats.getStats().getGrunt().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(
                        MobUtil.oldAiWalkSpeed(
                                GlyphidStats.getStats().getGrunt().speed
                                        * (variant == TYPE_RADIOACTIVE ? 2D : 1D)));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(
                        GlyphidStats.getStats().getGrunt().damage
                                * (variant == TYPE_RADIOACTIVE ? 5D : 1D));
    }

    public StatBundle getStats() {
        return GlyphidStats.getStats().statsGrunt;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new GlyphidAttackGoal(this));
        goalSelector.addGoal(4, new GlyphidWanderGoal(this, 1.0D));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new GlyphidTargetGoal(this));
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public float[] getCurrentDTDR(DamageSource damage, float amount, float pierceDT, float pierce) {
        if (damage.is(DamageTypeTags.BYPASSES_ARMOR)) return new float[] {0F, 0F};
        StatBundle stats = this.getStats();
        float threshold = stats.thresholdMultForArmor * getGlyphidArmor() / 5F;

        if (damage.is(ModDamageTypes.NUCLEAR_BLAST)) return new float[] {threshold * 0.25F, 0F};
        if (damage.is(ModDamageTypes.LASER))
            return new float[] {threshold * 0.5F, stats.resistanceMult * 0.5F};
        if (damage.is(ModDamageTypes.ELECTRIC))
            return new float[] {threshold * 0.25F, stats.resistanceMult * 0.25F};
        if (damage.is(ModDamageTypes.SUBATOMIC))
            return new float[] {0F, stats.resistanceMult * 0.1F};

        if (isFireDamage(damage)) return new float[] {0F, stats.resistanceMult * 0.2F};
        if (isExplosionDamage(damage))
            return new float[] {threshold * 0.5F, stats.resistanceMult * 0.35F};

        return new float[] {threshold, stats.resistanceMult};
    }

    @Override
    public void onDamageDealt(DamageSource damage, float amount) {
        if (isArmorBroken(amount)) breakOffArmor();
    }

    static boolean isFireDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE) || source.is(ModDamageTypes.FIRE);
    }

    static boolean isExplosionDamage(DamageSource source) {
        return source.is(DamageTypeTags.IS_EXPLOSION) || source.is(ModDamageTypes.EXPLOSIVE);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (GlyphidPathDebug.hasWatchers()
                && tickCount % GlyphidPathDebug.BROADCAST_INTERVAL == 0) {
            GlyphidPathDebug.broadcast(this);
        }

        if (!hasHome) {
            homeX = (int) getX();
            homeY = (int) getY();
            homeZ = (int) getZ();
            hasHome = true;
        }

        if (hasEffect(MobEffects.BLINDNESS)) {
            onBlinded();
        }

        if (getCurrentTask() == TASK_FOLLOW) {

            if (isAtDestination() && !hasWaypoint) {
                setCurrentTask(TASK_IDLE, null);
            }
        } else if (getCurrentTask() == TASK_DIG && tickCount % 20 == 0 && isAtDestination()) {
            swing(InteractionHand.MAIN_HAND);

            ExplosionVNT vnt = new ExplosionVNT(level, taskX, taskY + 2, taskZ, blastSize, this);
            vnt.setBlockAllocator(
                    new BlockAllocatorGlyphidDig(blastResToDig, EntityGlyphid::isSpawnerBlock));
            vnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop());
            vnt.setEntityProcessor(null);
            vnt.setPlayerProcessor(null);
            vnt.explode();

            setCurrentTask(previousTask, previousWaypoint);
        }

        setBesideClimbableBlock(horizontalCollision);

        if (tickCount % 100 == 0) {
            swing(InteractionHand.MAIN_HAND);
        }

        if (!hasEffect(MobEffects.BLINDNESS)
                && getNavigation().isDone()
                && getCurrentTask() != TASK_IDLE
                && !isAtDestination()) {

            if (taskWaypoint != null) {

                taskX = (int) taskWaypoint.getX();
                taskY = (int) taskWaypoint.getY();
                taskZ = (int) taskWaypoint.getZ();

                if (taskWaypoint.highPriority) setTarget(null);
            }

            if (hasWaypoint) {

                BlockHitResult obstacle = canDig() ? findWaypointObstruction() : null;

                if (getGlyphidScale() >= 1 && getCurrentTask() != TASK_DIG && obstacle != null) {
                    digToWaypoint(level, obstacle);
                } else {
                    pathToTask();
                }
            }
        }
    }

    private void pathToTask() {
        int maxDist = (int) (Math.sqrt(distanceToSqr(taskX, taskY, taskZ)) * 1.2D);
        Path path = getNavigation().createPath(new BlockPos(taskX, taskY, taskZ), 1, maxDist);
        if (path != null) getNavigation().moveTo(path, 1.0D);
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

        Item drop = isOnFire() ? ModItems.GLYPHID_MEAT_GRILLED.get() : ModItems.GLYPHID_MEAT.get();
        if (random.nextInt(2) == 0) {
            spawnAtLocation(level, new ItemStack(drop, ((int) getGlyphidScale() * 2) + looting));
        }
    }

    public @Nullable Player findTargetCandidate() {
        if (hasEffect(MobEffects.BLINDNESS)) return null;
        double radius = useExtendedTargeting() ? 128D : 16D;
        return level().getNearestPlayer(getX(), getY(), getZ(), radius, true);
    }

    public boolean hasHighPriorityWaypoint() {
        return taskWaypoint != null && taskWaypoint.highPriority;
    }

    public boolean useExtendedTargeting() {
        return MobData.rampantExtendedTargetting()
                || PollutionHandler.getPollution(level(), blockPosition(), PollutionType.SOOT)
                        >= MobData.TARGETING_THRESHOLD.get();
    }

    protected boolean canDig() {
        return MobData.rampantDig();
    }

    public void onBlinded() {
        setTarget(null);
        getNavigation().stop();

        if (getGlyphidScale() >= 1.25) {
            if (tickCount % 20 == 0) {
                for (int i = 0; i < 16; i++) {
                    float angle = (float) Math.toRadians(360D / 16 * i);
                    Vec3 rot = new Vec3(0, 0, 4).yRot(angle);
                    Vec3 from = new Vec3(getX(), getY() + 1, getZ());
                    Vec3 to = new Vec3(getX() + rot.x, getY() + 1, getZ() + rot.z);

                    ClipContext context =
                            new ClipContext(
                                    from,
                                    to,
                                    ClipContext.Block.COLLIDER,
                                    ClipContext.Fluid.NONE,
                                    CollisionContext.empty());
                    BlockHitResult hit = level().clip(context);

                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockPos pos = hit.getBlockPos();
                        Block block = level().getBlockState(pos).getBlock();

                        if (isLanternBlock(block)
                                && level() instanceof ServerLevel server
                                && Services.PLATFORM.canEntityDestroyBlock(
                                        server, pos, server.getBlockState(pos), this)) {

                            setYRot(360F / 16 * i);
                            yBodyRot = getYRot();
                            swing(InteractionHand.MAIN_HAND);
                            level().destroyBlock(pos, false, this);
                        }
                    }
                }
            }
        }
    }

    public boolean isLanternBlock(Block block) {
        return BuiltInRegistries.BLOCK
                .getOptional(Library.id("lantern"))
                .map(lantern -> lantern == block)
                .orElse(false);
    }

    public static boolean isSpawnerBlock(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.GLYPHID_SPAWNER.get()
                || block == ModBlocks.GLYPHID_SPAWNER_INFESTED.get()
                || block == ModBlocks.GLYPHID_SPAWNER_RAD.get();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return getTarget() == null && getCurrentTask() == TASK_IDLE && tickCount > 100;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);

        if (!level().isClientSide() && doesInfectedSpawnMaggots() && subtype() == TYPE_INFECTED) {
            onInfectedDeath();
        }
    }

    protected void onInfectedDeath() {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        int count = 2 + random.nextInt(3);
        for (int k = 0; k < count; k++) {
            float f = ((float) (k % 2) - 0.5F) * 0.5F;
            float f1 = ((float) (k / 2) - 0.5F) * 0.5F;
            if (EntityParasiteMaggot.TYPE != null) {
                EntityParasiteMaggot maggot =
                        new EntityParasiteMaggot(EntityParasiteMaggot.TYPE.get(), serverLevel);
                maggot.snapTo(
                        getX() + f, getY() + 0.5D, getZ() + f1, random.nextFloat() * 360.0F, 0F);
                maggot.setDeltaMovement(f, 0D, f1);
                maggot.hurtMarked = true;
                serverLevel.addFreshEntity(maggot);
            }
        }

        playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 2.0F, 0.95F + random.nextFloat() * 0.2F);

        ParticleCreators.giblets(serverLevel, this, ParticleGiblet.TYPE_MEAT, 0);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof EntityGlyphid) return false;
        return GlyphidStats.getStats().handleAttack(this, source, amount);
    }

    public boolean attackSuperclass(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel serverLevel)) return false;
        return super.hurtServer(serverLevel, source, amount);
    }

    public boolean doesInfectedSpawnMaggots() {
        return true;
    }

    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.6, 2), 100);
    }

    public void breakOffArmor() {
        byte armorValue = armor();
        List<Integer> indices = new ArrayList<>(List.of(0, 1, 2, 3, 4));
        Collections.shuffle(indices);

        for (int i : indices) {
            byte bit = (byte) (1 << i);
            if ((armorValue & bit) > 0) {
                armorValue &= ~bit;
                armorValue = (byte) (armorValue & 0b11111);
                entityData.set(DW_ARMOR, armorValue);
                playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.0F, 1.25F);
                break;
            }
        }
    }

    public int getGlyphidArmor() {
        int total = 0;
        byte armorValue = armor();
        for (int i = 0; i < 5; i++) {
            total += (armorValue & (1 << i)) != 0 ? 1 : 0;
        }
        return total;
    }

    public byte armor() {
        return entityData.get(DW_ARMOR);
    }

    public byte subtype() {
        return entityData.get(DW_SUBTYPE);
    }

    public void setSubtype(byte subtype) {
        entityData.set(DW_SUBTYPE, subtype);
    }

    @Override
    protected void updateSwingTime() {
        int duration = swingDuration();

        if (swinging) {
            swingTime++;

            if (swingTime >= duration) {
                swingTime = 0;
                swinging = false;
            }
        } else {
            swingTime = 0;
        }

        attackAnim = (float) swingTime / (float) duration;
    }

    public int swingDuration() {
        return 15;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {}

    @Override
    public boolean onClimbable() {
        return isBesideClimbableBlock();
    }

    public boolean isBesideClimbableBlock() {
        return entityData.get(DW_WALL);
    }

    public void setBesideClimbableBlock(boolean climbable) {
        entityData.set(DW_WALL, climbable);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {

        if (swinging) return false;
        swing(InteractionHand.MAIN_HAND);

        if (subtype() == TYPE_INFECTED && target instanceof LivingEntity livingTarget) {
            livingTarget.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
            livingTarget.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));
        }

        return super.doHurtTarget(level, target);
    }

    public int getCurrentTask() {
        return currentTask;
    }

    public @Nullable EntityWaypoint getWaypoint() {
        return taskWaypoint;
    }

    public void setCurrentTask(int task, @Nullable EntityWaypoint waypoint) {
        this.currentTask = task;
        this.taskWaypoint = waypoint;
        this.hasWaypoint = waypoint != null;
        if (taskWaypoint != null) {

            taskX = (int) taskWaypoint.getX();
            taskY = (int) taskWaypoint.getY();
            taskZ = (int) taskWaypoint.getZ();

            if (taskWaypoint.highPriority) {
                setTarget(null);
                getNavigation().stop();
            }
        }
        carryOutTask();
    }

    public void carryOutTask() {
        switch (getCurrentTask()) {
            case TASK_RETREAT_FOR_REINFORCEMENTS -> {
                if (taskWaypoint != null) {
                    communicate(TASK_FOLLOW, taskWaypoint);
                    setCurrentTask(TASK_FOLLOW, taskWaypoint);
                }
            }

            case TASK_INITIATE_RETREAT -> {
                if (level() instanceof ServerLevel serverLevel
                        && taskWaypoint == null
                        && EntityWaypoint.TYPE != null) {

                    EntityType<EntityWaypoint> type = EntityWaypoint.TYPE.get();

                    EntityWaypoint additional = new EntityWaypoint(type, serverLevel);
                    additional.snapTo(getX(), getY(), getZ(), 0F, 0F);

                    EntityWaypoint home = new EntityWaypoint(type, serverLevel);
                    home.setWaypointType(TASK_RETREAT_FOR_REINFORCEMENTS);
                    home.setAdditionalWaypoint(additional);
                    home.setHighPriority();
                    home.snapTo(homeX, homeY, homeZ, 0F, 0F);
                    serverLevel.addFreshEntity(home);

                    this.taskWaypoint = home;
                    communicate(TASK_FOLLOW, home);
                    setCurrentTask(TASK_FOLLOW, taskWaypoint);
                }
            }

            default -> {}
        }
    }

    public void communicate(int task, @Nullable EntityWaypoint waypoint) {
        int radius = waypoint != null ? waypoint.radius : 4;
        AABB bb = new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(radius);

        List<Entity> bugs = level().getEntities(this, bb);
        for (Entity e : bugs) {
            if (e instanceof EntityGlyphid bug && !bug.isScoutType()) {
                if (bug.getCurrentTask() != task) {
                    bug.setCurrentTask(task, waypoint);
                }
            }
        }
    }

    public boolean expandHive() {
        return false;
    }

    public boolean isAtDestination() {
        int destinationRadius = taskWaypoint != null ? (int) Math.pow(taskWaypoint.radius, 2) : 25;
        return distanceToSqr(taskX, taskY, taskZ) <= destinationRadius;
    }

    public boolean isScoutType() {
        return false;
    }

    public boolean isNuclearType() {
        return false;
    }

    public @Nullable BlockHitResult findWaypointObstruction() {
        Vec3 from = new Vec3(getX(), getY() + getEyeHeight(), getZ());
        Vec3 to = new Vec3(taskX, taskY, taskZ);

        BlockHitResult obstruction =
                level().clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        CollisionContext.empty()));

        if (obstruction.getType() == HitResult.Type.MISS) return null;

        BlockState hit = level().getBlockState(obstruction.getBlockPos());
        return hit.getBlock().getExplosionResistance() <= blastResToDig ? obstruction : null;
    }

    public void digToWaypoint(ServerLevel level, BlockHitResult obstacle) {
        if (EntityWaypoint.TYPE == null) return;

        BlockPos pos = obstacle.getBlockPos();
        EntityWaypoint target = new EntityWaypoint(EntityWaypoint.TYPE.get(), level);
        target.snapTo(pos.getX(), pos.getY(), pos.getZ(), 0F, 0F);
        target.radius = 5;
        level.addFreshEntity(target);

        previousTask = getCurrentTask();
        previousWaypoint = getWaypoint();

        setCurrentTask(TASK_DIG, target);

        pathToTask();

        communicate(TASK_DIG, target);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putByte("armor", armor());
        output.putByte("subtype", subtype());

        output.putBoolean("hasHome", hasHome);
        output.putInt("homeX", homeX);
        output.putInt("homeY", homeY);
        output.putInt("homeZ", homeZ);

        output.putBoolean("hasWaypoint", hasWaypoint);
        output.putInt("taskX", taskX);
        output.putInt("taskY", taskY);
        output.putInt("taskZ", taskZ);

        output.putInt("task", currentTask);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        entityData.set(DW_ARMOR, input.getByteOr("armor", (byte) 0b11111));
        entityData.set(DW_SUBTYPE, input.getByteOr("subtype", TYPE_NORMAL));

        hasHome = input.getBooleanOr("hasHome", false);
        homeX = input.getIntOr("homeX", 0);
        homeY = input.getIntOr("homeY", 0);
        homeZ = input.getIntOr("homeZ", 0);

        hasWaypoint = input.getBooleanOr("hasWaypoint", false);
        taskX = input.getIntOr("taskX", 0);
        taskY = input.getIntOr("taskY", 0);
        taskZ = input.getIntOr("taskZ", 0);

        currentTask = input.getIntOr("task", TASK_IDLE);
    }
}
