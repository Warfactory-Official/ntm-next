// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon.sedna;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockDetonatable;
import com.hbm.blocks.generic.BlockDecoCRT;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.items.EnumCasingType;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.lib.ModDamageTypes;
import com.hbm.particle.SpentCasing;
import com.hbm.sound.ModSounds;
import com.hbm.util.BobMathUtil;
import com.hbm.util.DamageClass;
import com.hbm.util.EntityDamageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BulletConfig implements Cloneable {

    public static List<BulletConfig> configs = new ArrayList<>();
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_RICOCHET =
            (bullet, mop) -> {
                if (mop instanceof BlockHitResult blockHit) {

                    BlockPos pos = blockHit.getBlockPos();
                    BlockState state = bullet.level().getBlockState(pos);

                    if (state.instrument() == NoteBlockInstrument.HAT
                            || state.is(Blocks.GLOWSTONE)) {
                        bullet.level().destroyBlock(pos, false);
                        bullet.setPos(mop.getLocation());
                        return;
                    }
                    if (state.getBlock() instanceof BlockDetonatable det) {
                        det.onShot(bullet.level(), pos);
                    }
                    if (state.getBlock() instanceof BlockDecoCRT
                            && !state.is(ModBlocks.DECO_CRT_BROKEN.get())) {
                        bullet.level()
                                .setBlock(
                                        pos,
                                        ModBlocks.DECO_CRT_BROKEN
                                                .get()
                                                .defaultBlockState()
                                                .setValue(
                                                        BlockDecoCRT.ROTATION,
                                                        state.getValue(BlockDecoCRT.ROTATION)),
                                        3);
                    }

                    Direction dir = blockHit.getDirection();
                    Vec3 face = new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ());
                    Vec3 vel = bullet.getDeltaMovement().normalize();

                    double angle = Math.abs(BobMathUtil.getCrossAngle(vel, face) - 90);

                    if (angle <= bullet.config.ricochetAngle) {

                        bullet.ricochets++;
                        if (bullet.ricochets > bullet.config.maxRicochetCount) {
                            bullet.setPos(mop.getLocation());
                            bullet.discard();
                        }

                        Vec3 motion = bullet.getDeltaMovement();
                        switch (dir.getAxis()) {
                            case Y:
                                bullet.setDeltaMovement(motion.x, -motion.y, motion.z);
                                break;
                            case Z:
                                bullet.setDeltaMovement(motion.x, motion.y, -motion.z);
                                break;
                            case X:
                                bullet.setDeltaMovement(-motion.x, motion.y, motion.z);
                                break;
                        }
                        bullet.level()
                                .playSound(
                                        null,
                                        bullet.getX(),
                                        bullet.getY(),
                                        bullet.getZ(),
                                        ModSounds.WEAPON_RICOCHET.get(),
                                        SoundSource.PLAYERS,
                                        0.25F,
                                        1.0F);
                        bullet.setPos(mop.getLocation());
                        bullet.sendTeleport();

                    } else {
                        bullet.setPos(mop.getLocation());
                        bullet.discard();
                    }
                }
            };
    public static BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_ENTITY_HIT =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit) {
                    Entity entity = entityHit.getEntity();

                    if (entity == bullet.getThrower()
                            && bullet.tickCount < bullet.selfDamageDelay()) return;
                    if (entity instanceof LivingEntity && ((LivingEntity) entity).getHealth() <= 0)
                        return;

                    DamageSource source =
                            getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);
                    float intendedDamage = bullet.damage;

                    if (!(entity instanceof LivingEntity)) {
                        EntityDamageUtil.attackEntityFromIgnoreIFrame(
                                entity, source, bullet.damage);
                        return;
                    } else if (bullet.config.headshotMult > 1F) {

                        LivingEntity living = (LivingEntity) entity;
                        double head = living.getBbHeight() - living.getEyeHeight();

                        if (living.isAlive()
                                && mop.getLocation() != null
                                && mop.getLocation().y
                                        > (living.getY() + living.getBbHeight() - head * 2)) {
                            intendedDamage *= bullet.config.headshotMult;
                        }
                    }

                    LivingEntity living = (LivingEntity) entity;
                    float prevHealth = living.getHealth();

                    EntityDamageUtil.attackEntityFromNT(
                            living,
                            source,
                            intendedDamage,
                            true,
                            true,
                            bullet.config.knockbackMult,
                            bullet.config.armorThresholdNegation,
                            bullet.config.armorPiercingPercent);

                    float newHealth = living.getHealth();

                    if (bullet.config.damageFalloffByPen)
                        bullet.damage -= Math.max(prevHealth - newHealth, 0) * 0.5;
                    if (!bullet.doesPenetrate() || bullet.damage < 0) {
                        bullet.setPos(mop.getLocation());
                        bullet.discard();
                    }

                    if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
                }
            };
    public static BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_STANDARD_BEAM_HIT =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit) {
                    Entity entity = entityHit.getEntity();

                    if (entity instanceof LivingEntity && ((LivingEntity) entity).getHealth() <= 0)
                        return;

                    DamageSource source =
                            getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);

                    if (!(entity instanceof LivingEntity living)) {
                        EntityDamageUtil.attackEntityFromIgnoreIFrame(
                                entity, source, bullet.damage);
                        return;
                    }

                    EntityDamageUtil.attackEntityFromNT(
                            living,
                            source,
                            bullet.damage,
                            true,
                            true,
                            bullet.config.knockbackMult,
                            bullet.config.armorThresholdNegation,
                            bullet.config.armorPiercingPercent);
                    if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
                }
            };
    public static BiConsumer<EntityBulletBeamBase, HitResult> LAMBDA_BEAM_HIT =
            (beam, mop) -> {
                if (mop instanceof EntityHitResult entityHit) {
                    Entity entity = entityHit.getEntity();

                    if (entity instanceof LivingEntity && ((LivingEntity) entity).getHealth() <= 0)
                        return;

                    DamageSource source = getDamage(beam, beam.thrower, beam.config.dmgClass);

                    if (!(entity instanceof LivingEntity living)) {
                        EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, beam.damage);
                        return;
                    }

                    EntityDamageUtil.attackEntityFromNT(
                            living,
                            source,
                            beam.damage,
                            true,
                            false,
                            beam.config.knockbackMult,
                            beam.config.armorThresholdNegation,
                            beam.config.armorPiercingPercent);
                }
            };
    public int id;
    public Supplier<? extends Item> ammoItem;
    public Supplier<ItemStack> casingItem;
    public int casingAmount;
    public int ammoReloadCount = 1;
    public float velocity = 10F;
    public float spread = 0F;
    public float wear = 1F;
    public int projectilesMin = 1;
    public int projectilesMax = 1;
    public ProjectileType pType = ProjectileType.BULLET;
    public float damageMult = 1.0F;
    public float armorThresholdNegation = 0.0F;
    public float armorPiercingPercent = 0.0F;
    public float knockbackMult = 0.1F;
    public float headshotMult = 1.25F;
    public DamageClass dmgClass = DamageClass.PHYSICAL;
    public float ricochetAngle = 5F;
    public int maxRicochetCount = 2;
    public boolean damageFalloffByPen = true;
    public Consumer<Entity> onUpdate;
    public BiConsumer<EntityBulletBaseMK4, HitResult> onImpact;
    public BiConsumer<EntityBulletBaseMK4, HitResult> onRicochet = LAMBDA_STANDARD_RICOCHET;
    public BiConsumer<EntityBulletBaseMK4, HitResult> onEntityHit = LAMBDA_STANDARD_ENTITY_HIT;
    public double gravity = 0;
    public int expires = 30;
    public boolean impactsEntities = true;
    public boolean doesPenetrate = false;
    public boolean isSpectral = false;
    public int selfDamageDelay = 2;
    public boolean blackPowder = false;
    public boolean renderRotations = true;
    public SpentCasing casing;
    public BiConsumer<EntityBulletBeamBase, HitResult> onImpactBeam;

    public BulletConfig() {
        this.id = configs.size();
        configs.add(this);
    }

    public static DamageSource getDamage(
            Entity projectile, LivingEntity shooter, DamageClass dmgClass) {
        var type =
                projectile
                        .level()
                        .registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ModDamageTypes.forClass(dmgClass));
        if (shooter != null) return new DamageSourceSedna(type, projectile, shooter);
        return new DamageSourceSedna(type);
    }

    public BulletConfig forceReRegister() {
        this.id = configs.size();
        configs.add(this);
        return this;
    }

    public BulletConfig setBeam() {
        this.pType = ProjectileType.BEAM;
        return this;
    }

    public BulletConfig setChunkloading() {
        this.pType = ProjectileType.BULLET_CHUNKLOADING;
        return this;
    }

    public BulletConfig setItem(Supplier<? extends Item> ammo) {
        this.ammoItem = ammo;
        return this;
    }

    public BulletConfig setItem(GunFactory.EnumAmmo ammo) {
        return setItem(() -> ModItems.AMMO_STANDARD.get(ammo));
    }

    public BulletConfig setItem(GunFactory.EnumAmmoSecret ammo) {
        return setItem(() -> ModItems.AMMO_SECRET.get(ammo));
    }

    public BulletConfig setCasing(Supplier<ItemStack> item, int amount) {
        this.casingItem = item;
        this.casingAmount = amount;
        return this;
    }

    public BulletConfig setCasing(EnumCasingType item, int amount) {
        this.casingItem = () -> ModItems.CASING.stack(item);
        this.casingAmount = amount;
        return this;
    }

    public BulletConfig setReloadCount(int ammoReloadCount) {
        this.ammoReloadCount = ammoReloadCount;
        return this;
    }

    public BulletConfig setVel(float velocity) {
        this.velocity = velocity;
        return this;
    }

    public BulletConfig setSpread(float spread) {
        this.spread = spread;
        return this;
    }

    public BulletConfig setWear(float wear) {
        this.wear = wear;
        return this;
    }

    public BulletConfig setProjectiles(int amount) {
        this.projectilesMin = this.projectilesMax = amount;
        return this;
    }

    public BulletConfig setProjectiles(int min, int max) {
        this.projectilesMin = min;
        this.projectilesMax = max;
        return this;
    }

    public BulletConfig setDamage(float damageMult) {
        this.damageMult = damageMult;
        return this;
    }

    public BulletConfig setThresholdNegation(float armorThresholdNegation) {
        this.armorThresholdNegation = armorThresholdNegation;
        return this;
    }

    public BulletConfig setArmorPiercing(float armorPiercingPercent) {
        this.armorPiercingPercent = armorPiercingPercent;
        return this;
    }

    public BulletConfig setKnockback(float knockbackMult) {
        this.knockbackMult = knockbackMult;
        return this;
    }

    public BulletConfig setHeadshot(float headshotMult) {
        this.headshotMult = headshotMult;
        return this;
    }

    public BulletConfig setupDamageClass(DamageClass clazz) {
        this.dmgClass = clazz;
        return this;
    }

    public BulletConfig setRicochetAngle(float angle) {
        this.ricochetAngle = angle;
        return this;
    }

    public BulletConfig setRicochetCount(int count) {
        this.maxRicochetCount = count;
        return this;
    }

    public BulletConfig setDamageFalloffByPen(boolean falloff) {
        this.damageFalloffByPen = falloff;
        return this;
    }

    public BulletConfig setGrav(double gravity) {
        this.gravity = gravity;
        return this;
    }

    public BulletConfig setLife(int expires) {
        this.expires = expires;
        return this;
    }

    public BulletConfig setImpactsEntities(boolean impact) {
        this.impactsEntities = impact;
        return this;
    }

    public BulletConfig setDoesPenetrate(boolean pen) {
        this.doesPenetrate = pen;
        return this;
    }

    public BulletConfig setSpectral(boolean spectral) {
        this.isSpectral = spectral;
        return this;
    }

    public BulletConfig setSelfDamageDelay(int delay) {
        this.selfDamageDelay = delay;
        return this;
    }

    public BulletConfig setBlackPowder(boolean bp) {
        this.blackPowder = bp;
        return this;
    }

    public BulletConfig setRenderRotations(boolean rot) {
        this.renderRotations = rot;
        return this;
    }

    public BulletConfig setCasing(SpentCasing casing) {
        this.casing = casing;
        return this;
    }

    public BulletConfig setOnUpdate(Consumer<Entity> lambda) {
        this.onUpdate = lambda;
        return this;
    }

    public BulletConfig setOnRicochet(BiConsumer<EntityBulletBaseMK4, HitResult> lambda) {
        this.onRicochet = lambda;
        return this;
    }

    public BulletConfig setOnImpact(BiConsumer<EntityBulletBaseMK4, HitResult> lambda) {
        this.onImpact = lambda;
        return this;
    }

    public BulletConfig setOnEntityHit(BiConsumer<EntityBulletBaseMK4, HitResult> lambda) {
        this.onEntityHit = lambda;
        return this;
    }

    public BulletConfig setOnBeamImpact(BiConsumer<EntityBulletBeamBase, HitResult> lambda) {
        this.onImpactBeam = lambda;
        return this;
    }

    public boolean matchesAmmo(ItemStack slot) {
        return !slot.isEmpty() && slot.getItem() == ammoItem.get();
    }

    public ItemStack ammoStack() {
        return new ItemStack(ammoItem.get());
    }

    @Override
    public BulletConfig clone() {
        try {
            BulletConfig clone = (BulletConfig) super.clone();
            clone.forceReRegister();
            return clone;
        } catch (CloneNotSupportedException e) {
        }
        return null;
    }

    public enum ProjectileType {
        BULLET,
        BULLET_CHUNKLOADING,
        BEAM
    }
}
