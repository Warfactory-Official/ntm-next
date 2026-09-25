// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.botprime;

import com.hbm.entity.mob.ai.EntityAINearestAttackableTargetNT;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class EntityBOTPrimeBody extends EntityBOTPrimeBase {

    public EntityBOTPrimeBody(EntityType<? extends EntityBOTPrimeBody> type, Level level) {
        super(type, level);
        this.rangeForParts = 70D;
        this.segmentDistance = 3.5D;
        this.maxBodySpeed = 1.4D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityBOTPrimeBase.createAttributes().add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(
                1,
                new EntityAINearestAttackableTargetNT<Player>(
                        this,
                        Player.class,
                        0,
                        false,
                        false,
                        (entity, level) -> this.notMyWorm.test(entity)));
    }

    @Override
    public float getAttackStrength(Entity target) {
        if (target instanceof LivingEntity living) return living.getHealth() * 0.75F;
        return 100F;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {

        updateActionState();
        updateMovement();

        if (this.didCheck) {

            if (this.targetedEntity == null || !this.targetedEntity.isAlive()) {
                setHealth(getHealth() - 1999F);
            }

            if ((this.followed == null || !this.followed.isAlive())
                    && this.random.nextInt(60) == 0) {
                level.explode(this, getX(), getY(), getZ(), 2F, Level.ExplosionInteraction.NONE);
            }
        }

        if (this.followed != null && this.followed.isAlive() && getTarget() != null) {

            if (canSeeThroughNonSolids(getTarget())) {
                this.attackCounter++;

                if (this.attackCounter == 10) {
                    laserAttack(getTarget(), false);
                    this.attackCounter = -20;
                }

            } else if (this.attackCounter > 0) {
                this.attackCounter--;
            }

        } else if (this.attackCounter > 0) {
            this.attackCounter--;
        }

        faceFollowed();
    }

    @Override
    public void tick() {
        super.tick();
        faceFollowed();
    }

    private void faceFollowed() {

        if (this.targetedEntity == null) return;

        double dx = this.targetedEntity.getX() - getX();
        double dy = this.targetedEntity.getY() - getY();
        double dz = this.targetedEntity.getZ() - getZ();
        float hyp = (float) Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.atan2(dx, dz) * 180D / Math.PI);
        float pitch = (float) (Math.atan2(dy, hyp) * 180D / Math.PI);

        setYRot(yaw);
        this.yRotO = yaw;
        setXRot(pitch);
        this.xRotO = pitch;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("partID", getPartNumber());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setPartNumber(input.getIntOr("partID", 0));
    }
}
