// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import com.hbm.entity.projectile.EntityRubble;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.SmokeShockPayload;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.platform.Services;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ExplosionLarge {

    private ExplosionLarge() {}

    public static void explode(
            Level level,
            double x,
            double y,
            double z,
            float strength,
            boolean cloud,
            boolean rubble,
            boolean shrapnel) {
        explode(level, null, x, y, z, strength, false, cloud, rubble, shrapnel);
    }

    public static void explodeFire(
            Level level,
            double x,
            double y,
            double z,
            float strength,
            boolean cloud,
            boolean rubble,
            boolean shrapnel) {
        explode(level, null, x, y, z, strength, true, cloud, rubble, shrapnel);
    }

    private static void explode(
            Level level,
            Entity detonator,
            double x,
            double y,
            double z,
            float strength,
            boolean fire,
            boolean cloud,
            boolean rubble,
            boolean shrapnel) {
        level.explode(
                detonator, null, null, x, y, z, strength, fire, Level.ExplosionInteraction.BLOCK);

        if (cloud) ExplosionCreator.composeEffectStandard(level, x, y + 2, z);
        if (rubble) spawnRubble(level, x, y + 2, z, rubbleCount(strength));
        if (shrapnel) spawnShrapnels(level, x, y + 2, z, shrapnelCount(strength));
    }

    private static int rubbleCount(float strength) {
        return (int) (strength / 10F);
    }

    private static int shrapnelCount(float strength) {
        return (int) (strength / 3F);
    }

    public static void spawnShock(
            Level level, double x, double y, double z, int count, double strength) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(
                new SmokeShockPayload(x, y + 0.5, z, count, strength, SmokeShockPayload.MODE_SHOCK),
                new TargetPoint(server, x, y, z, 250));
    }

    public static void spawnBurst(
            Level level, double x, double y, double z, int count, double strength) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(
                new SmokeShockPayload(x, y, z, count, strength, SmokeShockPayload.MODE_BURST),
                new TargetPoint(server, x, y, z, 250));
    }

    public static void spawnParticlesRadial(Level level, double x, double y, double z, int count) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(
                new SmokeShockPayload(x, y, z, count, 0, SmokeShockPayload.MODE_RADIAL),
                new TargetPoint(server, x, y, z, 250));
    }

    public static void spawnFoam(Level level, double x, double y, double z, int count) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(
                new SmokeShockPayload(x, y, z, count, 0, SmokeShockPayload.MODE_FOAM),
                new TargetPoint(server, x, y, z, 250));
    }

    public static void spawnParticles(Level level, double x, double y, double z, int count) {
        if (!(level instanceof ServerLevel server)) return;
        Services.NETWORK.sendToAllAround(
                new SmokeShockPayload(x, y, z, count, 0, SmokeShockPayload.MODE_CLOUD),
                new TargetPoint(server, x, y, z, 250));
    }

    public static void spawnRubble(Level level, double x, double y, double z, int count) {
        RandomSource rand = level.getRandom();
        for (int i = 0; i < count; i++) {
            EntityRubble rubble = new EntityRubble(level, x, y, z);
            rubble.setBlockState(Blocks.STONE.defaultBlockState());
            double motionY = 0.75D * (1 + (count + rand.nextInt(count * 5)) / 25);
            rubble.setDeltaMovement(
                    rand.nextGaussian() * 0.75D * (1 + count / 50),
                    motionY,
                    rand.nextGaussian() * 0.75D * (1 + count / 50));
            level.addFreshEntity(rubble);
        }
    }

    public static void spawnShrapnels(Level level, double x, double y, double z, int count) {
        RandomSource rand = level.getRandom();
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(level, x, y, z);
            double motionY =
                    ((rand.nextFloat() * 0.5D) + 0.5D) * (1 + count / (15 + rand.nextInt(21)))
                            + (rand.nextFloat() / 50F * count);
            shrapnel.setDeltaMovement(
                    rand.nextGaussian() * (1 + count / 50),
                    motionY,
                    rand.nextGaussian() * (1 + count / 50));
            shrapnel.setTrail(rand.nextInt(3) == 0 ? 1 : 0);
            level.addFreshEntity(shrapnel);
        }
    }

    public static void spawnTracers(Level level, double x, double y, double z, int count) {
        RandomSource rand = level.getRandom();
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(level, x, y, z);
            double motionY =
                    ((rand.nextFloat() * 0.5) + 0.5) * (1 + (count / (15 + rand.nextInt(21))))
                            + (rand.nextFloat() / 50 * count) * 0.25F;
            double motionX = rand.nextGaussian() * (1 + (count / 50)) * 0.25F;
            double motionZ = rand.nextGaussian() * (1 + (count / 50)) * 0.25F;
            shrapnel.setDeltaMovement(motionX, motionY, motionZ);
            shrapnel.setTrail(1);
            level.addFreshEntity(shrapnel);
        }
    }

    public static void spawnShrapnelShower(
            Level level,
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            int count,
            double deviation) {
        RandomSource rand = level.getRandom();
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(level, x, y, z);
            shrapnel.setDeltaMovement(
                    mX + rand.nextGaussian() * deviation,
                    mY + rand.nextGaussian() * deviation,
                    mZ + rand.nextGaussian() * deviation);
            shrapnel.setTrail(rand.nextInt(3) == 0 ? 1 : 0);
            level.addFreshEntity(shrapnel);
        }
    }

    public static void spawnMissileDebris(
            Level level,
            double x,
            double y,
            double z,
            double mX,
            double mY,
            double mZ,
            double deviation,
            List<ItemStack> debris,
            ItemStack rareDrop) {
        RandomSource rand = level.getRandom();
        for (ItemStack stack : debris) {
            if (stack.isEmpty()) continue;
            int drops = rand.nextInt(stack.getCount() + 1);
            for (int i = 0; i < drops; i++) {
                double dvx = (mX + rand.nextGaussian() * deviation) * 0.85;
                double dvy = (mY + rand.nextGaussian() * deviation) * 0.85;
                double dvz = (mZ + rand.nextGaussian() * deviation) * 0.85;
                ItemEntity item =
                        new ItemEntity(level, x + dvx * 2, y + dvy * 2, z + dvz * 2, stack.copy());
                item.setDeltaMovement(dvx, dvy, dvz);
                level.addFreshEntity(item);
            }
        }
        if (!rareDrop.isEmpty() && rand.nextInt(10) == 0) {
            ItemEntity item = new ItemEntity(level, x, y, z, rareDrop.copy());
            item.setDeltaMovement(
                    mX + rand.nextGaussian() * deviation * 0.1,
                    mY + rand.nextGaussian() * deviation * 0.1,
                    mZ + rand.nextGaussian() * deviation * 0.1);
            level.addFreshEntity(item);
        }
    }

    public static void buster(
            Level level, double x, double y, double z, Vec3 vector, float strength, int depth) {
        Vec3 dir = vector.normalize();
        for (int i = 0; i < depth; i += 2) {
            level.explode(
                    null,
                    null,
                    null,
                    x + dir.x * i,
                    y + dir.y * i,
                    z + dir.z * i,
                    strength,
                    false,
                    Level.ExplosionInteraction.BLOCK);
        }
    }

    public static void jolt(
            Level level, double x, double y, double z, int strength, int count, double vel) {
        RandomSource rand = level.getRandom();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int n = 0; n < count; n++) {
            double phi = rand.nextDouble() * Math.PI * 2;
            double cosTheta = rand.nextDouble() * 2 - 1;
            double sinTheta = Math.sqrt(1 - cosTheta * cosTheta);
            double dx = sinTheta * Math.cos(phi);
            double dy = cosTheta;
            double dz = sinTheta * Math.sin(phi);
            for (int i = 0; i < strength; i++) {
                int bx = (int) Math.floor(x + dx * i);
                int by = (int) Math.floor(y + dy * i);
                int bz = (int) Math.floor(z + dz * i);
                pos.set(bx, by, bz);
                BlockState state = level.getBlockState(pos);
                if (state.liquid()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    continue;
                }
                if (state.isAir()) continue;
                if (state.getBlock().getExplosionResistance() > 70F) continue;
                EntityRubble rubble = new EntityRubble(level, bx + 0.5, by + 0.5, bz + 0.5);
                rubble.setBlockState(state);

                Vec3 back = new Vec3(x - (bx + 0.5), y - (by + 0.5), z - (bz + 0.5)).scale(vel);
                rubble.setDeltaMovement(back);
                level.addFreshEntity(rubble);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                break;
            }
        }
    }
}
