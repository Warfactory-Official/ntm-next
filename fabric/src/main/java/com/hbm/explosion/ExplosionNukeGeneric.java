// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockGlyphidSpawner;
import com.hbm.config.VersatileConfig;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.projectile.EntityB92Beam;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.platform.IEnergyHandlerView;
import com.hbm.platform.Services;
import com.hbm.util.EntityDamageUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ExplosionNukeGeneric {

    private static Block oreUranium;
    private static Block oreSchrabidium;
    private static Block oreUraniumScorched;
    private static Block oreNetherUranium;
    private static Block oreNetherSchrabidium;
    private static Block oreNetherUraniumScorched;
    private static Block oreGneissUranium;
    private static Block oreGneissSchrabidium;
    private static Block oreGneissUraniumScorched;
    private static Block wastePlanks;

    private ExplosionNukeGeneric() {}

    private static void resolveBlocks() {
        if (oreUranium != null) return;
        oreUranium = ModBlocks.ORE_URANIUM.get();
        oreSchrabidium = ModBlocks.ORE_SCHRABIDIUM.get();
        oreUraniumScorched = ModBlocks.ORE_URANIUM_SCORCHED.get();
        oreNetherUranium = ModBlocks.ORE_NETHER_URANIUM.get();
        oreNetherSchrabidium = ModBlocks.ORE_NETHER_SCHRABIDIUM.get();
        oreNetherUraniumScorched = ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get();
        oreGneissUranium = ModBlocks.ORE_GNEISS_URANIUM.get();
        oreGneissSchrabidium = ModBlocks.ORE_GNEISS_SCHRABIDIUM.get();
        oreGneissUraniumScorched = ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get();
        wastePlanks = ModBlocks.WASTE_PLANKS.get();
    }

    public static void wasteDest(Level world, BlockPos pos, RandomSource random) {
        if (world.isClientSide()) return;
        resolveBlocks();

        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();

        if (state.is(BlockTags.DOORS)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            world.setBlock(pos, ModBlocks.WASTE_EARTH.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.MYCELIUM)) {
            world.setBlock(
                    pos, ModBlocks.WASTE_MYCELIUM.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
            if (random.nextInt(20) == 1) {
                Block waste =
                        state.is(Blocks.SAND)
                                ? ModBlocks.WASTE_TRINITITE.get()
                                : ModBlocks.WASTE_TRINITITE_RED.get();
                world.setBlock(pos, waste.defaultBlockState(), Block.UPDATE_ALL);
            }
        } else if (state.is(Blocks.CLAY)) {
            world.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.MOSSY_COBBLESTONE)) {
            world.setBlock(pos, Blocks.COAL_ORE.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.COAL_ORE)) {
            int roll = random.nextInt(10);
            if (roll >= 1 && roll <= 3) {
                world.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
            } else if (roll == 9) {
                world.setBlock(pos, Blocks.EMERALD_ORE.defaultBlockState(), Block.UPDATE_ALL);
            }
        } else if (state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM)) {

            world.setBlock(pos, ModBlocks.WASTE_LOG.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE)
                && state.isSolidRender()
                && b != ModBlocks.WASTE_LOG.get()) {

            world.setBlock(pos, wastePlanks.defaultBlockState(), Block.UPDATE_ALL);
        } else if (b == oreUranium) {
            transmuteUraniumOre(world, pos, random, oreSchrabidium, oreUraniumScorched);
        } else if (b == oreNetherUranium) {
            transmuteUraniumOre(world, pos, random, oreNetherSchrabidium, oreNetherUraniumScorched);
        } else if (b == oreGneissUranium) {
            transmuteUraniumOre(world, pos, random, oreGneissSchrabidium, oreGneissUraniumScorched);
        }
    }

    private static void transmuteUraniumOre(
            Level world, BlockPos pos, RandomSource random, Block schrabidium, Block scorched) {
        Block result =
                random.nextInt(VersatileConfig.getSchrabOreChance()) == 1 ? schrabidium : scorched;
        world.setBlock(pos, result.defaultBlockState(), Block.UPDATE_ALL);
    }

    public static void waste(Level world, BlockPos center, int radius, RandomSource random) {
        if (world.isClientSide()) return;
        int r = radius;
        int r2 = r * r;
        int r22 = r2 / 2;
        int bound = r22 / 5;
        if (bound == 0) return;
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22 + random.nextInt(bound)) {
                        pos.set(X, Y, Z);
                        if (!world.getBlockState(pos).isAir())
                            wasteDest(world, pos.immutable(), random);
                    }
                }
            }
        }
    }

    public static void wasteDestNoSchrab(Level world, BlockPos pos, RandomSource random) {
        if (world.isClientSide()) return;
        resolveBlocks();

        BlockState state = world.getBlockState(pos);
        if (state.is(Blocks.GLASS)
                || state.getBlock() instanceof StainedGlassBlock
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.LEAVES)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            world.setBlock(pos, ModBlocks.WASTE_EARTH.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.MYCELIUM)) {
            world.setBlock(
                    pos, ModBlocks.WASTE_MYCELIUM.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)) {
            if (random.nextInt(20) == 1) {
                Block waste =
                        state.is(Blocks.SAND)
                                ? ModBlocks.WASTE_TRINITITE.get()
                                : ModBlocks.WASTE_TRINITITE_RED.get();
                world.setBlock(pos, waste.defaultBlockState(), Block.UPDATE_ALL);
            }
        } else if (state.is(Blocks.CLAY)) {
            world.setBlock(pos, Blocks.TERRACOTTA.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.MOSSY_COBBLESTONE)) {
            world.setBlock(pos, Blocks.COAL_ORE.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.COAL_ORE)) {
            int roll = random.nextInt(30);
            if (roll >= 1 && roll <= 3) {
                world.setBlock(pos, Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
            } else if (roll == 29) {
                world.setBlock(pos, Blocks.EMERALD_ORE.defaultBlockState(), Block.UPDATE_ALL);
            }
        } else if (state.is(BlockTags.LOGS) || state.is(Blocks.MUSHROOM_STEM)) {

            world.setBlock(pos, ModBlocks.WASTE_LOG.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(BlockTags.PLANKS)) {
            world.setBlock(pos, wastePlanks.defaultBlockState(), Block.UPDATE_ALL);
        } else if (state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK)) {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void wasteNoSchrab(
            Level world, BlockPos center, int radius, RandomSource random) {
        if (world.isClientSide()) return;
        int r = radius;
        int r22 = r * r / 2;
        int bound = r22 / 5;
        if (bound == 0) return;
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -r; xx < r; xx++) {
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    if (YY + zz * zz < r22 + random.nextInt(bound)) {
                        pos.set(xx + x, yy + y, zz + z);
                        if (!world.getBlockState(pos).isAir()) {
                            wasteDestNoSchrab(world, pos.immutable(), random);
                        }
                    }
                }
            }
        }
    }

    public static void empBlast(Level world, BlockPos center, int bombStartStrength) {
        if (world.isClientSide()) return;
        int radius = bombStartStrength;
        int threshold = radius * radius / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xx = -radius; xx < radius; xx++)
            for (int yy = -radius; yy < radius; yy++)
                for (int zz = -radius; zz < radius; zz++) {
                    if (xx * xx + yy * yy + zz * zz >= threshold) continue;
                    pos.set(center.getX() + xx, center.getY() + yy, center.getZ() + zz);
                    emp(world, pos);
                }
    }

    public static void emp(Level world, BlockPos pos) {
        if (world.isClientSide()) return;

        BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof IEnergyHandlerMK2 power) {
            power.setPower(0);
            if (world.getRandom().nextInt(5) < 1) {
                world.setBlock(
                        pos,
                        ModBlocks.BLOCK_ELECTRICAL_SCRAP.get().defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }

        if (drainForeignEnergy(world, pos) && world.getRandom().nextInt(5) <= 1) {
            world.setBlock(
                    pos,
                    ModBlocks.BLOCK_ELECTRICAL_SCRAP.get().defaultBlockState(),
                    Block.UPDATE_ALL);
        }
    }

    public static boolean drainForeignEnergy(Level world, BlockPos pos) {
        boolean drained = false;
        for (Direction side : Direction.values()) {
            IEnergyHandlerView view = Services.CAPS.findEnergyHandler(world, pos, side);
            if (view == null) continue;
            long stored = view.amount();
            if (stored > 0 && view.extract(stored, 1L, false) > 0) drained = true;
        }
        return drained;
    }

    public static boolean hasDrainableEnergy(Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof IEnergyHandlerMK2) return true;
        for (Direction side : Direction.values()) {
            if (Services.CAPS.findEnergyHandler(world, pos, side) != null) return true;
        }
        return false;
    }

    public static void solinium(Level world, BlockPos pos) {
        if (world.isClientSide()) return;
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || block == ModBlocks.WASTE_EARTH.get()
                || block == ModBlocks.WASTE_MYCELIUM.get()) {
            world.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }

        if (state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.LEAVES)
                || block instanceof VegetationBlock
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.SPONGE)
                || state.is(Blocks.WET_SPONGE)
                || state.is(Blocks.SUGAR_CANE)
                || block == ModBlocks.PLANT_REEDS.get()
                || block == ModBlocks.LEAVES_LAYER.get()
                || isGlyphidHive(block)) {
            world.removeBlock(pos, false);
        }
    }

    private static boolean isGlyphidHive(Block block) {
        return block == ModBlocks.GLYPHID_BASE.get()
                || block == ModBlocks.GLYPHID_BASE_INFESTED.get()
                || block == ModBlocks.GLYPHID_BASE_RAD.get()
                || block instanceof BlockGlyphidSpawner;
    }

    public static void dealDamage(Level world, double x, double y, double z, double radius) {
        dealDamage(world, x, y, z, radius, 250F);
    }

    public static void dealDamage(
            Level world, double x, double y, double z, double radius, float maxDamage) {
        AABB box = new AABB(x, y, z, x, y, z).inflate(radius);

        List<Entity> list = world.getEntities(null, box);
        DamageSource source = world.damageSources().source(ModDamageTypes.NUCLEAR_BLAST);

        for (Entity e : list) {
            double dist = Math.sqrt(e.distanceToSqr(x, y, z));
            if (dist > radius) continue;

            double entX = e.getX();
            double entY = e.getY() + e.getEyeHeight();
            double entZ = e.getZ();

            if (isExplosionExempt(e) || isObstructed(world, e, x, y, z, entX, entY, entZ)) continue;

            boolean doKnockback = true;
            double damage = maxDamage * (radius - dist) / radius;
            if (e instanceof LivingEntity living && living.isAlive()) {
                doKnockback =
                        EntityDamageUtil.attackEntityFromNT(
                                living, source, (float) damage, true, true, 0, 100F, 0);

                if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
            } else if (world instanceof ServerLevel server) {
                e.hurtServer(server, source, (float) damage);
            }

            e.igniteForSeconds(5F);

            if (doKnockback) {
                double knockX = e.getX() - x;
                double knockY = e.getY() + e.getEyeHeight() - y;
                double knockZ = e.getZ() - z;
                double len = Math.sqrt(knockX * knockX + knockY * knockY + knockZ * knockZ);
                if (len != 0D) {
                    knockX /= len;
                    knockY /= len;
                    knockZ /= len;
                    e.setDeltaMovement(
                            e.getDeltaMovement().add(knockX * 0.2D, knockY * 0.2D, knockZ * 0.2D));
                }
            }
        }
    }

    private static boolean isExplosionExempt(Entity e) {

        if (e instanceof Ocelot
                || e instanceof EntityB92Beam
                || e instanceof EntityBulletBaseMK4
                || e instanceof EntityGrenadeUniversal) {
            return true;
        }

        return e instanceof Player player && player.isCreative();
    }

    private static boolean isObstructed(
            Level world, Entity e, double x, double y, double z, double a, double b, double c) {
        ClipContext ctx =
                new ClipContext(
                        new Vec3(x, y, z),
                        new Vec3(a, b, c),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        e);
        return world.clip(ctx).getType() != HitResult.Type.MISS;
    }
}
