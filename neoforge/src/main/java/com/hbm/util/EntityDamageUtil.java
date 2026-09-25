// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.util;

import com.hbm.api.entity.IResistanceProvider;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.PlayerShield;
import com.hbm.interfaces.NtmDamageContext;
import com.hbm.items.armor.ArmorDamageHandler;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.lib.ModDamageTypes;
import com.hbm.platform.Services;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class EntityDamageUtil {

    private EntityDamageUtil() {}

    @Deprecated
    public static boolean attackEntityFromIgnoreIFrame(
            Entity victim, DamageSource src, float damage) {
        if (!(victim.level() instanceof ServerLevel serverLevel)) return false;

        if (!victim.hurtServer(serverLevel, src, damage)) {

            if (victim instanceof LivingEntity living) {
                if (living.invulnerableTime > 10) {
                    damage += living.lastHurt;
                }
            }
            return victim.hurtServer(serverLevel, src, damage);
        } else {
            return true;
        }
    }

    public static boolean attackEntityFromNT(
            LivingEntity living,
            DamageSource source,
            float amount,
            boolean ignoreIFrame,
            boolean allowSpecialCancel,
            double knockbackMultiplier,
            float pierceDT,
            float pierce) {
        if (!(living.level() instanceof ServerLevel serverLevel)) return false;
        NtmDamageContext context = (NtmDamageContext) living;
        float previousDT = context.hbm$pierceDT();
        float previousDR = context.hbm$pierceDR();
        double previousKnockback = context.hbm$knockbackMultiplier();
        boolean previousCancellation = context.hbm$ignoreEarlyCancellation();
        context.hbm$setPiercing(pierceDT, pierce);
        context.hbm$setKnockbackMultiplier(knockbackMultiplier);

        context.hbm$setIgnoreEarlyCancellation(
                !allowSpecialCancel && !Services.CONFIG.runtime().damageCompatibilityMode());
        try {
            if (ignoreIFrame) living.invulnerableTime = 0;
            return living.hurtServer(serverLevel, source, amount);
        } finally {
            context.hbm$setPiercing(previousDT, previousDR);
            context.hbm$setKnockbackMultiplier(previousKnockback);
            context.hbm$setIgnoreEarlyCancellation(previousCancellation);
        }
    }

    public static boolean allowsAttack(LivingEntity entity, DamageSource source, float amount) {
        if (ArmorDamageHandler.filterAttack(entity, source, amount) <= 0F) return false;
        if (source.is(DamageTypeTags.BYPASSES_EFFECTS)) return true;
        NtmDamageContext context = (NtmDamageContext) entity;
        float[] values =
                getDTDR(entity, source, amount, context.hbm$pierceDT(), context.hbm$pierceDR());
        float dt = values[0] - context.hbm$pierceDT();
        float dr = values[1] - context.hbm$pierceDR();

        return !((dt > 0F && dt >= amount) || dr >= 1F);
    }

    public static float modifyAcceptedDamage(
            LivingEntity entity, DamageSource source, float amount) {

        if (entity instanceof Player player) amount = PlayerShield.absorb(player, amount);
        HbmLivingProps props = HbmLivingProps.peek(entity);
        if (props != null && props.contagion > 0 && amount < 100F) amount *= 2F;
        amount = ArmorModHandler.modifyDamage(entity, source, amount);
        amount = ArmorDamageHandler.filterHurt(entity, source, amount);

        if (source.is(ModDamageTypes.ELECTRIC)
                && entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem armor
                && armor.maxPower() > 0) {
            amount *= 5F;
        }
        NtmDamageContext context = (NtmDamageContext) entity;
        return Math.max(
                0F,
                calculateDamageAndNotify(
                        entity, source, amount, context.hbm$pierceDT(), context.hbm$pierceDR()));
    }

    public static float calculateDamageAndNotify(
            LivingEntity entity, DamageSource damage, float amount, float pierceDT, float pierce) {
        float reduced = calculateDamage(entity, damage, amount, pierceDT, pierce);
        if (entity instanceof IResistanceProvider irp) irp.onDamageDealt(damage, reduced);
        return reduced;
    }

    public static float calculateDamage(
            LivingEntity entity, DamageSource damage, float amount, float pierceDT, float pierce) {

        if (damage.is(DamageTypeTags.BYPASSES_EFFECTS)) return amount;

        float[] vals = getDTDR(entity, damage, amount, pierceDT, pierce);
        float dt = vals[0];
        float dr = vals[1];

        dt = Math.max(0F, dt - pierceDT);
        if (dt >= amount) return 0F;
        amount -= dt;

        dr *= Mth.clamp(1F - pierce, 0F, 2F);

        return amount * (1F - dr);
    }

    public static float[] getDTDR(
            LivingEntity entity, DamageSource damage, float amount, float pierceDT, float pierce) {
        return DamageResistanceHandler.getDTDR(entity, damage, amount, pierceDT, pierce);
    }

    public static HitResult getMouseOver(Player attacker, double reach) {
        return getMouseOver(attacker, reach, 0D);
    }

    public static HitResult getMouseOver(Player attacker, double reach, double threshold) {
        Vec3 from = attacker.getEyePosition();
        HitResult blockHit = attacker.pick(reach, 1F, false);
        double maxDistanceSqr = reach * reach;
        double blockDistanceSqr = Double.MAX_VALUE;
        double rayLength = reach;
        if (blockHit.getType() != HitResult.Type.MISS) {
            blockDistanceSqr = blockHit.getLocation().distanceToSqr(from);
            maxDistanceSqr = blockDistanceSqr;
            rayLength = Math.sqrt(blockDistanceSqr);
        }

        Vec3 look = attacker.getViewVector(1F);
        Vec3 to = from.add(look.scale(rayLength));
        AABB search = attacker.getBoundingBox().expandTowards(look.scale(rayLength)).inflate(1D);
        EntityHitResult entityHit =
                threshold == 0D
                        ? ProjectileUtil.getEntityHitResult(
                                attacker,
                                from,
                                to,
                                search,
                                EntitySelector.CAN_BE_PICKED,
                                maxDistanceSqr)
                        : getEntityHitResult(attacker, from, to, search, threshold, maxDistanceSqr);
        if (entityHit != null && entityHit.getLocation().distanceToSqr(from) < blockDistanceSqr)
            return entityHit;
        return blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
    }

    private static EntityHitResult getEntityHitResult(
            Player attacker,
            Vec3 from,
            Vec3 to,
            AABB search,
            double threshold,
            double maxDistanceSqr) {
        double nearest = maxDistanceSqr;
        Entity hovered = null;
        Vec3 hoveredPos = null;
        for (Entity entity :
                attacker.level().getEntities(attacker, search, EntitySelector.CAN_BE_PICKED)) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + threshold);
            Vec3 clip = box.clip(from, to).orElse(null);
            if (box.contains(from)) {
                if (nearest >= 0D && entity.canBePickedFromInside()) {
                    hovered = entity;
                    hoveredPos = clip == null ? from : clip;
                    nearest = 0D;
                }
            } else if (clip != null) {
                double distanceSqr = from.distanceToSqr(clip);
                if (distanceSqr < nearest || nearest == 0D) {
                    if (entity.getRootVehicle() == attacker.getRootVehicle()) {
                        if (nearest == 0D) {
                            hovered = entity;
                            hoveredPos = clip;
                        }
                    } else {
                        hovered = entity;
                        hoveredPos = clip;
                        nearest = distanceSqr;
                    }
                }
            }
        }
        return hovered == null ? null : new EntityHitResult(hovered, hoveredPos);
    }
}
