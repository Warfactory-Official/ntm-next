// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.particle.EntityCloudFX;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.particle.EntityPinkCloudFX;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardClass;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.lib.ModDamageTypes;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ArmorRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ExplosionChaos {

    private ExplosionChaos() {}

    public static void hardenVirus(Level level, BlockPos center, int radius) {
        int threshold = radius * radius / 2;
        Block virus = ModBlocks.CRYSTAL_VIRUS.get();
        BlockState hardened = ModBlocks.CRYSTAL_HARDENED.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -radius; xx < radius; xx++) {
            int xxSq = xx * xx;
            for (int yy = -radius; yy < radius; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -radius; zz < radius; zz++) {
                    if (yySq + zz * zz >= threshold) continue;
                    pos.set(center.getX() + xx, center.getY() + yy, center.getZ() + zz);
                    if (level.getBlockState(pos).is(virus)) level.setBlockAndUpdate(pos, hardened);
                }
            }
        }
    }

    public static void floater(Level level, BlockPos center, int radius, int height) {
        int r2 = radius * radius;
        int threshold = r2 / 2;
        BlockPos.MutableBlockPos from = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos to = new BlockPos.MutableBlockPos();
        for (int xx = -radius; xx < radius; xx++) {
            int xxSq = xx * xx;
            for (int yy = -radius; yy < radius; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -radius; zz < radius; zz++) {
                    if (yySq + zz * zz >= threshold) continue;
                    from.set(center.getX() + xx, center.getY() + yy, center.getZ() + zz);
                    BlockState save = level.getBlockState(from);
                    level.setBlockAndUpdate(from, Blocks.AIR.defaultBlockState());
                    if (save.isAir()) continue;
                    to.set(from.getX(), from.getY() + height, from.getZ());
                    level.setBlockAndUpdate(to, save);
                }
            }
        }
    }

    public static void move(Level level, BlockPos center, int radius, int a, int b, int c) {
        double reach = radius;
        int doubled = radius * 2;
        AABB box =
                new AABB(
                        Math.floor(center.getX() - reach - 1D),
                        Math.floor(center.getY() - reach - 1D),
                        Math.floor(center.getZ() - reach - 1D),
                        Math.floor(center.getX() + reach + 1D),
                        Math.floor(center.getY() + reach + 1D),
                        Math.floor(center.getZ() + reach + 1D));

        for (Entity entity : level.getEntities((Entity) null, box, e -> true)) {
            if (entity.distanceToSqr(center.getX(), center.getY(), center.getZ())
                    > (double) doubled * doubled) continue;

            if (entity instanceof Sheep sheep) {
                sheep.setCustomName(Component.literal("jeb_"));
            } else if (entity instanceof Mob mob) {
                mob.setCustomName(
                        Component.literal(
                                level.getRandom().nextInt(2) == 0 ? "Dinnerbone" : "Grumm"));
            }

            double dx = entity.getX() - center.getX();
            double dy = entity.getY() + entity.getEyeHeight() - center.getY();
            double dz = entity.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz < reach * reach) {
                entity.setPos(entity.getX() + a, entity.getY() + b, entity.getZ() + c);
            }
        }
    }

    public static void igniteFlammableBlocks(ServerLevel level, int x, int y, int z, int bound) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int threshold = bound * bound / 2;
        for (int xx = -bound; xx < bound; xx++) {
            int xxSq = xx * xx;
            for (int yy = -bound; yy < bound; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -bound; zz < bound; zz++) {
                    if (yySq + zz * zz >= threshold) continue;
                    pos.set(x + xx, y + yy, z + zz);

                    if (!Services.PLATFORM.isFlammable(
                            level, pos, level.getBlockState(pos), Direction.UP)) continue;
                    BlockPos above = pos.above();
                    if (level.getBlockState(above).isAir()) {
                        level.setBlock(above, BaseFireBlock.getState(level, above), 3);
                    }
                }
            }
        }
    }

    public static void spawnPoisonCloud(
            Level level, double x, double y, double z, int count, double speed, int type) {
        for (int i = 0; i < count; i++) {
            Entity fx =
                    switch (type) {
                        case 1 -> new EntityCloudFX(level, x, y, z, 0D, 0D, 0D);
                        case 2 -> new EntityPinkCloudFX(level, x, y, z, 0D, 0D, 0D);
                        default -> new EntityOrangeFX(level, x, y, z, 0D, 0D, 0D);
                    };
            fx.setDeltaMovement(
                    level.getRandom().nextGaussian() * speed,
                    level.getRandom().nextGaussian() * speed,
                    level.getRandom().nextGaussian() * speed);
            level.addFreshEntity(fx);
        }
    }

    public static void cluster(
            Level level,
            double x,
            double y,
            double z,
            int count,
            float yaw,
            float pitch,
            float yawRand,
            float pitchRand,
            float speed) {

        for (int i = 0; i < count; i++) {
            EntityBulletBaseMK4 bullet =
                    new EntityBulletBaseMK4(
                            level,
                            XFactoryCatapult.cluster_submunition,
                            50F,
                            0F,
                            yaw + (float) (yawRand * level.getRandom().nextGaussian()),
                            pitch + (float) (pitchRand * level.getRandom().nextGaussian()));
            bullet.setPos(x, y, z);
            bullet.setDeltaMovement(bullet.getDeltaMovement().scale(speed));
            level.addFreshEntity(bullet);
        }
    }

    public static void pinkCloud(Level level, double x, double y, double z, double range) {
        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(
                                x - range, y - range, z - range, x + range, y + range,
                                z + range))) {
            if (entity.distanceToSqr(x, y, z) > range * range) continue;

            ArmorUtil.damageWholeSuit(entity, 25);

            if (level instanceof ServerLevel server) {
                entity.hurtServer(
                        server, level.damageSources().source(ModDamageTypes.PINK_CLOUD), 5F);
            }
        }
    }

    public static void chlorine(Level level, double x, double y, double z, double range) {
        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(
                                x - range, y - range, z - range, x + range, y + range,
                                z + range))) {
            if (entity.distanceToSqr(x, y, z) > range * range) continue;

            ArmorUtil.damageWholeSuit(entity, 25);

            if (ArmorUtil.checkForHazmat(entity)) continue;

            if (entity.hasEffect(HbmPotion.taint())) {
                entity.removeEffect(HbmPotion.taint());
                entity.addEffect(
                        new MobEffectInstance(HbmPotion.mutation(), 60 * 60 * 20, 0, false, false));
            }

            if (level instanceof ServerLevel server) {
                entity.hurtServer(server, level.damageSources().source(ModDamageTypes.CLOUD), 5F);
            }
        }
    }

    public static void poison(Level level, double x, double y, double z, double range) {
        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(
                                x - range, y - range, z - range, x + range, y + range,
                                z + range))) {
            if (entity.distanceToSqr(x, y, z) > range * range) continue;
            boolean protectedFromGas =
                    ArmorRegistry.hasProtection(entity, EquipmentSlot.HEAD, HazardClass.GAS_LUNG)
                            || ArmorRegistry.hasProtection(
                                    entity, EquipmentSlot.HEAD, HazardClass.GAS_BLISTERING);

            if (protectedFromGas) {
                ArmorUtil.damageGasMaskFilter(entity, 1);
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30 * 20, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 30 * 20, 2));
        }
    }

    public static void igniteAllBlocks(ServerLevel level, int x, int y, int z, int bound) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int threshold = bound * bound / 2;
        for (int xx = -bound; xx < bound; xx++) {
            int xxSq = xx * xx;
            for (int yy = -bound; yy < bound; yy++) {
                int yySq = xxSq + yy * yy;
                for (int zz = -bound; zz < bound; zz++) {
                    if (yySq + zz * zz >= threshold) continue;
                    pos.set(x + xx, y + yy, z + zz);
                    if (level.getBlockState(pos).isAir()) continue;
                    BlockPos above = pos.above();
                    BlockState aboveState = level.getBlockState(above);
                    if (aboveState.isAir() || aboveState.is(Blocks.SNOW)) {
                        level.setBlock(above, BaseFireBlock.getState(level, above), 3);
                    }
                }
            }
        }
    }
}
