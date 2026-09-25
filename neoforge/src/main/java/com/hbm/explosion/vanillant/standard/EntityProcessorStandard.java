// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm.explosion.vanillant.interfaces.IEntityRangeMutator;
import java.util.HashMap;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Deprecated
public class EntityProcessorStandard implements IEntityProcessor {

    protected IEntityRangeMutator range;
    protected ICustomDamageHandler damage;
    protected boolean allowSelfDamage = false;

    public static DamageSource setExplosionSource(ExplosionVNT explosion) {
        return explosion.world.damageSources().explosion(explosion.exploder, explosion.exploder);
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

        Vec3 center = new Vec3(x, y, z);

        for (int index = 0; index < list.size(); ++index) {

            Entity entity = list.get(index);
            double distanceScaled = entity.distanceToSqr(x, y, z);
            distanceScaled = Math.sqrt(distanceScaled) / size;

            if (distanceScaled <= 1.0D) {

                double deltaX = entity.getX() - x;
                double deltaY = entity.getY() + entity.getEyeHeight() - y;
                double deltaZ = entity.getZ() - z;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (distance != 0.0D) {

                    deltaX /= distance;
                    deltaY /= distance;
                    deltaZ /= distance;

                    double density = EntityProcessorCross.getBlockDensity(world, center, entity);
                    double knockback = (1.0D - distanceScaled) * density;

                    attackEntity(
                            entity,
                            explosion,
                            calculateDamage(distanceScaled, density, knockback, size));

                    double push = knockback * EntityProcessorCross.knockbackDamping(entity);
                    entity.setDeltaMovement(
                            entity.getDeltaMovement()
                                    .add(deltaX * push, deltaY * push, deltaZ * push));

                    if (entity instanceof Player) {
                        affectedPlayers.put(
                                (Player) entity,
                                new Vec3(
                                        deltaX * knockback,
                                        deltaY * knockback,
                                        deltaZ * knockback));
                    }

                    if (damage != null) {
                        damage.handleAttack(explosion, entity, distanceScaled);
                    }
                }
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

    public EntityProcessorStandard withRangeMod(float mod) {
        range = (explosion, r) -> r * mod;
        return this;
    }

    public EntityProcessorStandard withDamageMod(ICustomDamageHandler damage) {
        this.damage = damage;
        return this;
    }

    public EntityProcessorStandard allowSelfDamage() {
        this.allowSelfDamage = true;
        return this;
    }
}
