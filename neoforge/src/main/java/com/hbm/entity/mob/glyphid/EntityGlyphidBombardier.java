// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.projectile.EntityAcidBomb;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityGlyphidBombardier extends EntityGlyphid {

    public static Reg.@Nullable EntityHandle<EntityGlyphidBombardier> TYPE;

    public static void register(IRegistrar r) {
        TYPE =
                Reg.entity(
                        "entity_glyphid_bombardier",
                        () ->
                                EntityType.Builder.<EntityGlyphidBombardier>of(
                                                EntityGlyphidBombardier::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(1.75F, 1F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid_bombardier"))));
        r.registerLivingAttributes(TYPE, EntityGlyphidBombardier::createAttributes);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphid.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getBombardier().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBombardier().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getBombardier().damage);
    }

    protected @Nullable LivingEntity lastTarget;
    protected double lastX;
    protected double lastY;
    protected double lastZ;

    public EntityGlyphidBombardier(
            EntityType<? extends EntityGlyphidBombardier> type, Level level) {
        super(type, level);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_bombardier_tex;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getBombardier().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getBombardier().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getBombardier().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBombardier;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = getTarget();
        if (target != null) {

            if (tickCount % 20 == 0) {
                lastTarget = target;
                lastX = target.getX();
                lastY = target.getY();
                lastZ = target.getZ();
            }

            if (tickCount % 60 == 1) {

                boolean topAttack = false;

                double velX = target.getX() - lastX;
                double velY = target.getY() - lastY;
                double velZ = target.getZ() - lastZ;

                if (lastTarget != target
                        || Math.sqrt(velX * velX + velY * velY + velZ * velZ) > 30) {
                    velX = velY = velZ = 0;
                }

                if (distanceTo(target) > 20) {
                    topAttack = true;
                }

                int prediction = topAttack ? 60 : 20;
                double dx = target.getX() - getX() + velX * prediction;
                double dy =
                        (target.getY() + target.getBbHeight() / 2)
                                - (getY() + 1)
                                + velY * prediction;
                double dz = target.getZ() - getZ() + velZ * prediction;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (len < 3) return;
                double targetYaw = -Math.atan2(dx, dz);

                double x = Math.sqrt(dx * dx + dz * dz);
                double y = dy;
                double v0 = getV0();
                double v02 = v0 * v0;
                double g = 0.04D;
                double upperLower = topAttack ? 1 : -1;
                double targetPitch =
                        Math.atan(
                                (v02
                                                + Math.sqrt(
                                                                v02 * v02
                                                                        - g
                                                                                * (g * x * x
                                                                                        + 2 * y
                                                                                                * v02))
                                                        * upperLower)
                                        / (g * x));

                if (!Double.isNaN(targetPitch)) {

                    Vec3 fireVec = new Vec3(v0, 0, 0);
                    fireVec = fireVec.zRot((float) -targetPitch);
                    fireVec = fireVec.yRot((float) -(targetYaw + Math.PI * 0.5));

                    if (EntityAcidBomb.TYPE != null) {
                        for (int i = 0; i < getBombCount(); i++) {
                            EntityAcidBomb bomb =
                                    new EntityAcidBomb(level, getX(), getY() + 1, getZ());
                            bomb.setThrower(this);
                            bomb.setThrowableHeading(
                                    fireVec.x,
                                    fireVec.y,
                                    fireVec.z,
                                    (float) v0,
                                    i * getSpreadMult());
                            bomb.damage = getBombDamage();
                            level.addFreshEntity(bomb);
                        }
                    }

                    swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    public float getBombDamage() {
        return 5F;
    }

    public int getBombCount() {
        return 5;
    }

    public float getSpreadMult() {
        return 1F;
    }

    public double getV0() {
        return 1D;
    }
}
