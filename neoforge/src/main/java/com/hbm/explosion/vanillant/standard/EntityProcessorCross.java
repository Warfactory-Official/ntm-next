// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityRangeMutator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityProcessorCross implements IEntityProcessor {

    protected double nodeDist = 2D;
    protected IEntityRangeMutator range;
    protected ICustomDamageHandler damage;
    protected double knockbackMult = 1D;
    protected boolean allowSelfDamage = false;

    public EntityProcessorCross() {
        this(0);
    }

    public EntityProcessorCross(double nodeDist) {
        this.nodeDist = nodeDist;
    }

    public static boolean shouldDealKnockback(Entity entity) {
        return !(entity instanceof EntityBulletBaseMK4 || entity instanceof EntityGrenadeUniversal);
    }

    public static DamageSource setExplosionSource(ExplosionVNT explosion) {
        return explosion.world.damageSources().explosion(explosion.exploder, explosion.exploder);
    }

    public static double knockbackDamping(Entity entity) {
        return entity instanceof LivingEntity living
                ? 1.0D - living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
                : 1.0D;
    }

    public static double getBlockDensity(Level world, Vec3 source, Entity entity) {
        AABB bb = entity.getBoundingBox();
        double xs = 1.0D / ((bb.maxX - bb.minX) * 2.0D + 1.0D);
        double ys = 1.0D / ((bb.maxY - bb.minY) * 2.0D + 1.0D);
        double zs = 1.0D / ((bb.maxZ - bb.minZ) * 2.0D + 1.0D);
        double xOffset = (1.0D - Math.floor(1.0D / xs) * xs) / 2.0D;
        double zOffset = (1.0D - Math.floor(1.0D / zs) * zs) / 2.0D;

        if (xs < 0.0D || ys < 0.0D || zs < 0.0D) return 0.0D;

        int hits = 0;
        int count = 0;

        for (double xx = 0.0D; xx <= 1.0D; xx += xs) {
            for (double yy = 0.0D; yy <= 1.0D; yy += ys) {
                for (double zz = 0.0D; zz <= 1.0D; zz += zs) {
                    double px = bb.minX + (bb.maxX - bb.minX) * xx;
                    double py = bb.minY + (bb.maxY - bb.minY) * yy;
                    double pz = bb.minZ + (bb.maxZ - bb.minZ) * zz;
                    Vec3 from = new Vec3(px + xOffset, py, pz + zOffset);
                    if (world.clip(
                                            new ClipContext(
                                                    from,
                                                    source,
                                                    ClipContext.Block.COLLIDER,
                                                    ClipContext.Fluid.NONE,
                                                    entity))
                                    .getType()
                            == HitResult.Type.MISS) {
                        hits++;
                    }
                    count++;
                }
            }
        }

        return (double) hits / count;
    }

    public EntityProcessorCross setAllowSelfDamage() {
        this.allowSelfDamage = true;
        return this;
    }

    public EntityProcessorCross setKnockback(double mult) {
        this.knockbackMult = mult;
        return this;
    }

    @Override
    public HashMap<Player, Vec3> process(
            ExplosionVNT explosion, Level world, double x, double y, double z, float size) {

        HashMap<Player, Vec3> affectedPlayers = new HashMap<>();

        size *= 2.0F;

        if (range != null) {
            size = range.mutateRange(explosion, size);
        }

        double minX = x - (double) size - 1.0D;
        double maxX = x + (double) size + 1.0D;
        double minY = y - (double) size - 1.0D;
        double maxY = y + (double) size + 1.0D;
        double minZ = z - (double) size - 1.0D;
        double maxZ = z + (double) size + 1.0D;

        List<Entity> list =
                world.getEntities(
                        allowSelfDamage ? null : explosion.exploder,
                        new AABB(minX, minY, minZ, maxX, maxY, maxZ));

        Vec3[] nodes;

        if (this.nodeDist > 0) {
            Direction[] dirs = Direction.VALUES;
            nodes = new Vec3[7];
            for (int i = 0; i < 6; i++) {
                Direction dir = dirs[i];
                nodes[i] =
                        new Vec3(
                                x + dir.getStepX() * nodeDist,
                                y + dir.getStepY() * nodeDist,
                                z + dir.getStepZ() * nodeDist);
            }
            nodes[6] = new Vec3(x, y, z);
        } else {
            nodes = new Vec3[1];
            nodes[0] = new Vec3(x, y, z);
        }

        HashMap<Entity, Float> damageMap = new HashMap<>();

        for (int index = 0; index < list.size(); ++index) {

            Entity entity = list.get(index);
            AABB box = entity.getBoundingBox();
            double xDist =
                    (box.minX <= x && box.maxX >= x)
                            ? 0
                            : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
            double yDist =
                    (box.minY <= y && box.maxY >= y)
                            ? 0
                            : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
            double zDist =
                    (box.minZ <= z && box.maxZ >= z)
                            ? 0
                            : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
            double distanceScaled = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) / size;

            if (distanceScaled <= 1.0D) {

                double deltaX = entity.getX() - x;
                double deltaY = entity.getY() + entity.getEyeHeight() - y;
                double deltaZ = entity.getZ() - z;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (distance != 0.0D) {

                    deltaX /= distance;
                    deltaY /= distance;
                    deltaZ /= distance;

                    double density = 0;

                    for (Vec3 vec : nodes) {
                        double d = getBlockDensity(world, vec, entity);
                        if (d > density) {
                            density = d;
                        }
                    }

                    double knockback = (1.0D - distanceScaled) * density;

                    float dmg = calculateDamage(distanceScaled, density, knockback, size);
                    Float prev = damageMap.get(entity);
                    if (prev == null || prev < dmg) damageMap.put(entity, dmg);

                    if (shouldDealKnockback(entity)) {

                        double push = knockback * knockbackDamping(entity) * knockbackMult;
                        entity.setDeltaMovement(
                                entity.getDeltaMovement()
                                        .add(deltaX * push, deltaY * push, deltaZ * push));
                    }

                    if (entity instanceof Player) {
                        affectedPlayers.put(
                                (Player) entity,
                                new Vec3(
                                        deltaX * knockback * knockbackMult,
                                        deltaY * knockback * knockbackMult,
                                        deltaZ * knockback * knockbackMult));
                    }
                }
            }
        }

        for (Map.Entry<Entity, Float> entry : damageMap.entrySet()) {

            Entity entity = entry.getKey();
            attackEntity(entity, explosion, entry.getValue());

            if (damage != null) {
                AABB box = entity.getBoundingBox();
                double xDist =
                        (box.minX <= x && box.maxX >= x)
                                ? 0
                                : Math.min(Math.abs(box.minX - x), Math.abs(box.maxX - x));
                double yDist =
                        (box.minY <= y && box.maxY >= y)
                                ? 0
                                : Math.min(Math.abs(box.minY - y), Math.abs(box.maxY - y));
                double zDist =
                        (box.minZ <= z && box.maxZ >= z)
                                ? 0
                                : Math.min(Math.abs(box.minZ - z), Math.abs(box.maxZ - z));
                double distanceScaled =
                        Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist) / size;
                damage.handleAttack(explosion, entity, distanceScaled);
            }
        }

        return affectedPlayers;
    }

    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            entity.hurtServer(serverLevel, setExplosionSource(source), amount);
        }
    }

    public float calculateDamage(
            double distanceScaled, double density, double knockback, float size) {
        return (float) ((int) ((knockback * knockback + knockback) / 2.0D * 8.0D * size + 1.0D));
    }

    public EntityProcessorCross withRangeMod(float mod) {
        range = (explosion, r) -> r * mod;
        return this;
    }

    public EntityProcessorCross withDamageMod(ICustomDamageHandler damage) {
        this.damage = damage;
        return this;
    }
}
