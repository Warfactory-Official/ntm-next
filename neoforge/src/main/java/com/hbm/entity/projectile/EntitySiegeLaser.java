// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.lib.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntitySiegeLaser extends EntityThrowableNT {

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(EntitySiegeLaser.class, EntityDataSerializers.INT);

    private float damage = 2;
    private float explosive = 0F;
    private float breakChance = 0F;
    private boolean incendiary = false;

    public EntitySiegeLaser(EntityType<? extends EntitySiegeLaser> type, Level level) {
        super(type, level);
    }

    public EntitySiegeLaser(
            EntityType<? extends EntitySiegeLaser> type, Level level, LivingEntity thrower) {
        super(type, level);
        initThrower(thrower);
    }

    public EntitySiegeLaser(
            EntityType<? extends EntitySiegeLaser> type,
            Level level,
            double x,
            double y,
            double z) {
        super(type, level);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, 0xffffff);
    }

    public EntitySiegeLaser setDamage(float f) {
        this.damage = f;
        return this;
    }

    public EntitySiegeLaser setExplosive(float f) {
        this.explosive = f;
        return this;
    }

    public EntitySiegeLaser setBreakChance(float f) {
        this.breakChance = f;
        return this;
    }

    public EntitySiegeLaser setIncendiary() {
        this.incendiary = true;
        return this;
    }

    public EntitySiegeLaser setColor(int color) {
        this.entityData.set(COLOR, color);
        return this;
    }

    public int getColor() {
        return this.entityData.get(COLOR);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > 60) this.discard();
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (!(level() instanceof ServerLevel server)) return;

        Vec3 hitVec = mop.getLocation();

        if (mop instanceof EntityHitResult entityHit) {
            DamageSource dmg = laserDamageSource();
            Entity hitEntity = entityHit.getEntity();

            if (hitEntity.hurtServer(server, dmg, this.damage)) {
                this.discard();

                if (this.incendiary) hitEntity.igniteForSeconds(3);

                if (this.explosive > 0)
                    server.explode(
                            this,
                            hitVec.x,
                            hitVec.y,
                            hitVec.z,
                            this.explosive,
                            this.incendiary,
                            Level.ExplosionInteraction.NONE);
            }

        } else if (mop instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();

            if (this.explosive > 0) {
                server.explode(
                        this,
                        hitVec.x,
                        hitVec.y,
                        hitVec.z,
                        this.explosive,
                        this.incendiary,
                        Level.ExplosionInteraction.NONE);

            } else if (this.incendiary) {

                BlockPos target = pos.relative(blockHit.getDirection());

                if (server.getBlockState(target).canBeReplaced()) {
                    server.setBlockAndUpdate(target, Blocks.FIRE.defaultBlockState());
                }
            }

            if (this.random.nextFloat() < this.breakChance) {
                server.destroyBlock(pos, false);
            }

            this.discard();
        }
    }

    private DamageSource laserDamageSource() {
        Holder<DamageType> holder =
                level().registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ModDamageTypes.LASER);
        LivingEntity thrower = getThrower();
        return thrower != null ? new DamageSource(holder, this, thrower) : new DamageSource(holder);
    }

    @Override
    public double getGravityVelocity() {
        return 0D;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putFloat("damage", this.damage);
        output.putFloat("explosive", this.explosive);
        output.putFloat("breakChance", this.breakChance);
        output.putBoolean("incendiary", this.incendiary);
        output.putInt("color", this.getColor());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.damage = input.getFloatOr("damage", 2F);
        this.explosive = input.getFloatOr("explosive", 0F);
        this.breakChance = input.getFloatOr("breakChance", 0F);
        this.incendiary = input.getBooleanOr("incendiary", false);
        this.setColor(input.getIntOr("color", 0xffffff));
    }
}
