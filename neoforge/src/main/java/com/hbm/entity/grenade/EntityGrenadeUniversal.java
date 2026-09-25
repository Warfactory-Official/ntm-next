// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.grenade;

import com.hbm.NuclearTech;
import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.items.weapon.grenade.*;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityGrenadeUniversal extends EntityThrowableInterp {

    public static final int TRAIL_TRIPLET = 1;

    private static final EntityDataAccessor<ItemStack> GRENADE =
            SynchedEntityData.defineId(
                    EntityGrenadeUniversal.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> BOUNCES =
            SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TRAIL =
            SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.INT);

    public double prevSpin;
    public double spin;

    public EntityGrenadeUniversal(EntityType<? extends EntityGrenadeUniversal> type, Level level) {
        super(type, level);
    }

    public EntityGrenadeUniversal(Level level, ItemStack grenade) {
        this(ModEntities.GRENADE_UNIVERSAL.get(), level);
        setGrenadeItem(grenade);
    }

    public EntityGrenadeUniversal(Level level, LivingEntity thrower, ItemStack grenade) {
        this(level, grenade);
        setThrower(thrower);
        double radians = Math.toRadians(-thrower.getYRot() + 180F);
        double offsetX = 0.25D * Math.cos(radians);
        double offsetZ = -0.25D * Math.sin(radians);
        setPos(
                thrower.getX() + offsetX,
                thrower.getY() + thrower.getEyeHeight() - 0.25D,
                thrower.getZ() + offsetZ);
        Vec3 yeet = thrower.getLookAngle().normalize();
        setThrowableHeading(yeet.x, yeet.y, yeet.z, (float) getShell().getYeetForce(), 0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GRENADE, ItemStack.EMPTY);
        builder.define(BOUNCES, 0);
        builder.define(TRAIL, 0);
    }

    public ItemStack getGrenadeItem() {
        return entityData.get(GRENADE);
    }

    private void setGrenadeItem(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        entityData.set(GRENADE, copy);
    }

    public int getBounces() {
        return entityData.get(BOUNCES);
    }

    public int getTrail() {
        return entityData.get(TRAIL);
    }

    public EntityGrenadeUniversal setTrail(int trail) {
        entityData.set(TRAIL, trail);
        return this;
    }

    public ItemGrenadeShell.EnumGrenadeShell getShell() {
        return ItemGrenadeUniversal.getData(getGrenadeItem()).shell();
    }

    public ItemGrenadeFilling.EnumGrenadeFilling getFilling() {
        return ItemGrenadeUniversal.getData(getGrenadeItem()).filling();
    }

    public ItemGrenadeFuze.EnumGrenadeFuze getFuze() {
        return ItemGrenadeUniversal.getData(getGrenadeItem()).fuze();
    }

    public ItemGrenadeExtra.EnumGrenadeExtra getExtra() {
        return ItemGrenadeUniversal.getData(getGrenadeItem()).extra();
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;

        ItemGrenadeFuze.updateTick(this);
        ItemGrenadeExtra.updateTick(this);

        if (level().isClientSide()) {
            prevSpin = spin;
            if (getBounces() <= 0) spin += 15D;
            else spin += Math.min(15D, new Vec3(xo - getX(), 0, zo - getZ()).length() * 50D);
            if (spin >= 360D) {
                prevSpin -= 360D;
                spin -= 360D;
            }
            if (getTrail() == TRAIL_TRIPLET) {
                level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0D, 0D, 0D);
            }
        }
    }

    @Override
    protected void onImpact(HitResult impact) {
        ItemGrenadeFuze.onImpact(this, impact);
        ItemGrenadeExtra.onImpact(this, impact);

        if (isRemoved() || !(impact instanceof BlockHitResult blockHit)) return;
        Direction direction = blockHit.getDirection();
        Vec3 hit = blockHit.getLocation();
        setPos(
                hit.x + direction.getStepX() * 0.05D,
                hit.y + direction.getStepY() * 0.05D,
                hit.z + direction.getStepZ() * 0.05D);
        Vec3 velocity = getDeltaMovement();
        if (velocity.length() > 0.2D) {
            level().playSound(
                            null,
                            getX(),
                            getY(),
                            getZ(),
                            ModSounds.GRENADE_BOUNCE.get(),
                            SoundSource.BLOCKS,
                            1F,
                            1F);
        }
        double bounce = getShell().getBounce();
        setDeltaMovement(
                direction.getStepX() != 0 ? -velocity.x * bounce : velocity.x * 0.8D,
                direction.getStepY() != 0 ? -velocity.y * bounce : velocity.y * 0.8D,
                direction.getStepZ() != 0 ? -velocity.z * bounce : velocity.z * 0.8D);
        sendTeleport();
        entityData.set(BOUNCES, getBounces() + 1);
    }

    public void explode() {
        discard();
        ItemGrenadeFilling.explode(this);
        ItemGrenadeExtra.onExplode(this);
        if (!level().isClientSide() && Services.CONFIG.runtime().extendedLogging()) {
            NuclearTech.LOGGER.info(
                    "[GREN] Set off grenade at {} / {} / {} by {}!",
                    (int) getX(),
                    (int) getY(),
                    (int) getZ(),
                    getThrower() instanceof Player player
                            ? player.getDisplayName().getString()
                            : "null");
        }
    }

    public int getTimer() {
        return ticksInAir + ticksInGround;
    }

    @Override
    public boolean fullBlockCollisions() {
        return true;
    }

    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("grenade", ItemStack.OPTIONAL_CODEC, getGrenadeItem());
        output.putInt("bounces", getBounces());
        output.putInt("trail", getTrail());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("grenade", ItemStack.OPTIONAL_CODEC).ifPresent(this::setGrenadeItem);
        entityData.set(BOUNCES, input.getIntOr("bounces", 0));
        entityData.set(TRAIL, input.getIntOr("trail", 0));
    }
}
