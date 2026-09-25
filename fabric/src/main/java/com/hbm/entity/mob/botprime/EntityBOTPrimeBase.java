// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.botprime;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.items.weapon.sedna.factory.XFactoryNPC;
import com.hbm.sound.ModSounds;
import java.util.function.Predicate;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class EntityBOTPrimeBase extends EntityWormBaseNT implements IRadiationImmune {

    public int attackCounter;

    protected final Predicate<LivingEntity> notMyWorm =
            entity -> !(entity instanceof EntityWormBaseNT worm) || worm.getHeadID() != getHeadID();

    protected EntityBOTPrimeBase(EntityType<? extends EntityBOTPrimeBase> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.dragInAir = 0.995F;
        this.dragInGround = 0.98F;
        this.knockbackDivider = 1.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityWormBaseNT.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    public boolean canSeeThroughNonSolids(Entity target) {
        Vec3 from = new Vec3(getX(), getY() + getEyeHeight(), getZ());
        Vec3 to = new Vec3(target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
        return level().clip(
                                new ClipContext(
                                        from,
                                        to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        this))
                        .getType()
                == HitResult.Type.MISS;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return ModSounds.BOMB_DET.get();
    }

    protected void laserAttack(Entity target, boolean head) {

        if (!(target instanceof LivingEntity)) return;

        if (head) {

            for (int i = 0; i < 5; i++) {
                EntityBulletBaseMK4 bullet =
                        XFactoryNPC.aimed(
                                this,
                                XFactoryNPC.worm_laser,
                                XFactoryNPC.WORM_LASER_DAMAGE,
                                target,
                                i * 0.05F);
                if (bullet != null) level().addFreshEntity(bullet);
            }

            playLaser(0.75F);

        } else {
            EntityBulletBaseMK4 bullet =
                    XFactoryNPC.aimed(
                            this,
                            XFactoryNPC.worm_bolt,
                            XFactoryNPC.WORM_BOLT_DAMAGE,
                            target,
                            0.125F);
            if (bullet != null) level().addFreshEntity(bullet);
            playLaser(1.0F);
        }
    }

    private void playLaser(float pitch) {
        level().playSound(
                        null,
                        getX(),
                        getY(),
                        getZ(),
                        ModSounds.BALLS_LASER.get(),
                        SoundSource.HOSTILE,
                        5F,
                        pitch);
    }
}
