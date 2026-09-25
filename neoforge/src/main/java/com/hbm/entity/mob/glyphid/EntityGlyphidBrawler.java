// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.Reg;
import com.hbm.util.MobUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityGlyphidBrawler extends EntityGlyphid {

    public static Reg.@Nullable EntityHandle<EntityGlyphidBrawler> TYPE;

    public static void register(IRegistrar r) {
        TYPE =
                Reg.entity(
                        "entity_glyphid_brawler",
                        () ->
                                EntityType.Builder.<EntityGlyphidBrawler>of(
                                                EntityGlyphidBrawler::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(2F, 1.125F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid_brawler"))));
        r.registerLivingAttributes(TYPE, EntityGlyphidBrawler::createAttributes);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphid.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBrawler().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBrawler().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBrawler().damage);
    }

    public int timer = 0;
    protected @Nullable LivingEntity lastTarget;
    protected double lastX;
    protected double lastY;
    protected double lastZ;

    public EntityGlyphidBrawler(EntityType<? extends EntityGlyphidBrawler> type, Level level) {
        super(type, level);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = getTarget();
        if (target != null && isAlive()) {

            lastX = target.getX();
            lastY = target.getY();
            lastZ = target.getZ();

            if (--timer <= 0) {
                leap();
                timer = 80 + random.nextInt(30);
            }
        }
    }

    public void leap() {
        LivingEntity target = getTarget();
        if (!level().isClientSide() && target != null && distanceTo(target) < 20) {

            double velX = target.getX() - lastX;
            double velY = target.getY() - lastY;
            double velZ = target.getZ() - lastZ;

            if (lastTarget != target) {
                velX = velY = velZ = 0;
            }

            int prediction = 60;
            double dx = target.getX() - getX() + velX * prediction;
            double dy =
                    (target.getY() + target.getBbHeight() / 2) - (getY() + 1) + velY * prediction;
            double dz = target.getZ() - getZ() + velZ * prediction;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 3) return;
            double targetYaw = -Math.atan2(dx, dz);

            double x = Math.sqrt(dx * dx + dz * dz);
            double y = dy;
            double v0 = 1.5;
            double v02 = v0 * v0;
            double g = 0.01;
            double targetPitch =
                    Math.atan(
                            (v02 + Math.sqrt(v02 * v02 - g * (g * x * x + 2 * y * v02)) * 1)
                                    / (g * x));
            Vec3 fireVec = null;
            if (!Double.isNaN(targetPitch)) {

                fireVec = new Vec3(v0, 0, 0);
                fireVec = fireVec.zRot((float) (-targetPitch / 3.5));
                fireVec = fireVec.yRot((float) -(targetYaw + Math.PI * 0.5));
            }
            if (fireVec != null) {
                setThrowableHeading(
                        fireVec.x, fireVec.y, fireVec.z, (float) v0, random.nextFloat());
            }
        }
    }

    public void setThrowableHeading(
            double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
        float throwLen =
                (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= (double) throwLen;
        motionY /= (double) throwLen;
        motionZ /= (double) throwLen;
        motionX += this.random.nextGaussian() * 0.0075D * (double) inaccuracy;
        motionY += this.random.nextGaussian() * 0.0075D * (double) inaccuracy;
        motionZ += this.random.nextGaussian() * 0.0075D * (double) inaccuracy;
        motionX *= (double) velocity;
        motionY *= (double) velocity;
        motionZ *= (double) velocity;
        this.setDeltaMovement(motionX, motionY, motionZ);
        float hyp = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(motionY, (double) hyp) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;

        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.setXRot(pitch);
        this.xRotO = pitch;
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_brawler_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 1.25D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getBrawler().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBrawler().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getBrawler().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBrawler;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypes.FALL) && amount <= 10) return false;
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }
}
