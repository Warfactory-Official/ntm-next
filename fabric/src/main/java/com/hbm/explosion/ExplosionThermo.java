// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.ArmorUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ExplosionThermo {

    private static final int CAGE_WEST = -2, CAGE_EAST = 1;
    private static final int CAGE_UP = 3;
    private static final int CAGE_NORTH = -1, CAGE_SOUTH = 2;

    private ExplosionThermo() {}

    public static void freeze(Level level, BlockPos center, int bombStartStrength) {
        sphere(level, center, bombStartStrength, true);
    }

    public static void scorch(Level level, BlockPos center, int bombStartStrength) {
        sphere(level, center, bombStartStrength, false);
    }

    private static void sphere(
            Level level, BlockPos center, int bombStartStrength, boolean freezing) {
        RandomSource random = level.getRandom();
        int r = bombStartStrength * 2;
        int r2 = r * r;
        int r22 = r2 / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -r; xx < r; xx++) {
            int xSq = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int ySq = xSq + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int zSq = ySq + zz * zz;
                    if (zSq >= r22 + random.nextInt(r22 / 2)) continue;
                    pos.set(center.getX() + xx, center.getY() + yy, center.getZ() + zz);
                    if (freezing) {
                        freezeDest(level, pos);
                    } else {
                        scorchDest(level, pos);
                    }
                }
            }
        }
    }

    public static void freezeDest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block become = null;

        if (state.is(ModBlocks.VOLCANIC_LAVA_BLOCK.get())) become = Blocks.COBBLESTONE;
        else if (state.is(Blocks.GRASS_BLOCK)) become = ModBlocks.FROZEN_GRASS.get();
        else if (state.is(Blocks.DIRT)) become = ModBlocks.FROZEN_DIRT.get();
        else if (state.is(BlockTags.LOGS) || state.is(ModBlocks.WASTE_LOG.get())) {
            become = ModBlocks.FROZEN_LOG.get();
        } else if (state.is(BlockTags.PLANKS) || state.is(ModBlocks.WASTE_PLANKS.get())) {
            become = ModBlocks.FROZEN_PLANKS.get();
        } else if (state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)) {
            become = Blocks.PACKED_ICE;
        } else if (state.is(BlockTags.LEAVES)) become = Blocks.SNOW_BLOCK;
        else if (state.is(Blocks.LAVA)) become = Blocks.OBSIDIAN;
        else if (state.is(Blocks.WATER)) become = Blocks.ICE;

        if (become != null) level.setBlockAndUpdate(pos, become.defaultBlockState());
    }

    public static void scorchDest(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block become = null;

        if (state.is(Blocks.GRASS_BLOCK)
                || state.is(ModBlocks.FROZEN_GRASS.get())
                || state.is(ModBlocks.FROZEN_DIRT.get())) {
            become = Blocks.DIRT;
        } else if (state.is(Blocks.DIRT)) become = Blocks.NETHERRACK;
        else if (state.is(BlockTags.LOGS) || state.is(ModBlocks.FROZEN_LOG.get())) {
            become = ModBlocks.WASTE_LOG.get();
        } else if (state.is(BlockTags.PLANKS) || state.is(ModBlocks.FROZEN_PLANKS.get())) {
            become = ModBlocks.WASTE_PLANKS.get();
        } else if (state.is(Blocks.NETHERRACK)
                || state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.OBSIDIAN)) {
            become = Blocks.LAVA;
        } else if (state.is(BlockTags.LEAVES) || state.is(Blocks.WATER) || state.is(Blocks.ICE)) {
            become = Blocks.AIR;
        } else if (state.is(Blocks.PACKED_ICE)) become = Blocks.WATER;

        if (become != null) level.setBlockAndUpdate(pos, become.defaultBlockState());
    }

    public static void freezer(Level level, BlockPos center, int bombStartStrength) {
        double reach = bombStartStrength;
        for (Entity entity : near(level, center, reach)) {
            if (!inside(entity, center, reach, bombStartStrength * 2)) continue;

            if (entity instanceof Ocelot || entity instanceof Cat) continue;
            if (!(entity instanceof LivingEntity living)) continue;

            int x = (int) entity.getX();
            int y = (int) entity.getY();
            int z = (int) entity.getZ();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int a = x + CAGE_WEST; a < x + CAGE_EAST; a++) {
                for (int b = y; b < y + CAGE_UP; b++) {
                    for (int c = z + CAGE_NORTH; c < z + CAGE_SOUTH; c++) {
                        level.setBlockAndUpdate(pos.set(a, b, c), Blocks.ICE.defaultBlockState());
                    }
                }
            }

            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 2 * 60 * 20, 4));
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 90 * 20, 2));
            living.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 3 * 60 * 20, 2));
        }
    }

    public static void setEntitiesOnFire(Level level, BlockPos center, int radius) {
        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        AABB box = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (Entity entity : level.getEntities((Entity) null, box, e -> true)) {
            if (entity.distanceToSqr(x, y, z) > (double) radius * radius) continue;

            if (entity instanceof Player player && ArmorUtil.checkForAsbestos(player)) continue;
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 15 * 20, 4));
            }
            entity.igniteForSeconds(10);
        }
    }

    private static List<Entity> near(Level level, BlockPos center, double reach) {
        AABB box =
                new AABB(
                        Math.floor(center.getX() - reach - 1D),
                        Math.floor(center.getY() - reach - 1D),
                        Math.floor(center.getZ() - reach - 1D),
                        Math.floor(center.getX() + reach + 1D),
                        Math.floor(center.getY() + reach + 1D),
                        Math.floor(center.getZ() + reach + 1D));
        return level.getEntities((Entity) null, box, e -> true);
    }

    private static boolean inside(Entity entity, BlockPos center, double reach, int doubled) {
        if (entity.distanceToSqr(center.getX(), center.getY(), center.getZ())
                > (double) doubled * doubled) {
            return false;
        }
        double dx = entity.getX() - center.getX();
        double dy = entity.getY() + entity.getEyeHeight() - center.getY();
        double dz = entity.getZ() - center.getZ();
        return dx * dx + dy * dy + dz * dz < reach * reach;
    }
}
