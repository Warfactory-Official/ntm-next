// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockTaint;
import com.hbm.data.WorldData;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.ModItems;
import com.hbm.lib.ModDamageTypes;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tags.HbmBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class Meteorite {

    public static final Meteorite INSTANCE = new Meteorite();

    private static final int METEOR = 0;
    private static final int MOLTEN = 1;
    private static final int COBBLE = 2;
    private static final int BROKEN = 3;
    private static final int TREASURE = 4;
    private static final int TOXIC = 5;
    private static final int RANDOM_ORE = 6;
    private static final int BROKEN_99_TREASURE = 7;
    private static final int MOLTEN_OR_BROKEN = 8;
    private static final int COBBLE_OR_BROKEN = 9;
    private static final int METEOR_OR_TREASURE = 10;
    private static final int ORE_OR_BROKEN_EQUAL = 11;
    private static final int ORE_OR_TWO_BROKEN = 12;
    private static final int TWO_TREASURE_OR_BROKEN = 13;
    private static final int TREASURE_OR_BROKEN = 14;

    private static BlockState bs(RegistryHandle<? extends Block> handle) {
        return handle.get().defaultBlockState();
    }

    private static BlockState pick(int palette, RandomSource rand) {
        return switch (palette) {
            case METEOR -> Palette.METEOR;
            case MOLTEN -> Palette.MOLTEN;
            case COBBLE -> Palette.COBBLE;
            case BROKEN -> Palette.BROKEN;
            case TREASURE -> Palette.TREASURE;
            case TOXIC -> Palette.TOXIC;
            case RANDOM_ORE -> randomOre(rand);
            case BROKEN_99_TREASURE -> rand.nextInt(100) == 99 ? Palette.TREASURE : Palette.BROKEN;
            case MOLTEN_OR_BROKEN -> rand.nextBoolean() ? Palette.MOLTEN : Palette.BROKEN;
            case COBBLE_OR_BROKEN -> rand.nextBoolean() ? Palette.COBBLE : Palette.BROKEN;
            case METEOR_OR_TREASURE -> rand.nextBoolean() ? Palette.METEOR : Palette.TREASURE;
            case ORE_OR_BROKEN_EQUAL -> rand.nextBoolean() ? Palette.BROKEN : randomOre(rand);
            case ORE_OR_TWO_BROKEN -> rand.nextInt(7) < 2 ? Palette.BROKEN : randomOre(rand);
            case TWO_TREASURE_OR_BROKEN -> rand.nextInt(3) < 2 ? Palette.TREASURE : Palette.BROKEN;
            case TREASURE_OR_BROKEN -> rand.nextBoolean() ? Palette.TREASURE : Palette.BROKEN;
            default -> throw new IllegalArgumentException("unknown meteor palette: " + palette);
        };
    }

    private static BlockState randomOre(RandomSource rand) {
        return switch (rand.nextInt(5)) {
            case 0 -> Palette.ORE_IRON;
            case 1 -> Palette.ORE_COPPER;
            case 2 -> Palette.ORE_ALUMINIUM;
            case 3 -> Palette.ORE_RAREEARTH;
            default -> Palette.ORE_COBALT;
        };
    }

    public void generate(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos pos,
            boolean safe,
            boolean allowSpecials,
            boolean damagingImpact) {
        BlockPos.MutableBlockPos work = new BlockPos.MutableBlockPos();

        if (damagingImpact) {
            ServerLevel server = level.getLevel();
            AABB box =
                    new AABB(
                            pos.getX() - 7.5D,
                            pos.getY() - 7.5D,
                            pos.getZ() - 7.5D,
                            pos.getX() + 7.5D,
                            pos.getY() + 7.5D,
                            pos.getZ() + 7.5D);
            for (Entity entity : server.getEntities(null, box)) {
                entity.hurtServer(
                        server, server.damageSources().source(ModDamageTypes.METEORITE), 1000F);
            }
        }

        if (WorldData.ENABLE_SPECIAL_METEORS.get() && allowSpecials) {
            switch (rand.nextInt(300)) {
                case 0 -> {
                    generateBox(level, rand, pos, work, METEOR, safe);
                    return;
                }
                case 1 -> {
                    generateSphere7x7(level, rand, pos, work, ORE_OR_BROKEN_EQUAL, safe);
                    return;
                }
                case 2 -> {
                    generateSphere5x5(level, rand, pos, work, ORE_OR_TWO_BROKEN, safe);
                    return;
                }
                case 3 -> {
                    generateBox(level, rand, pos, work, RANDOM_ORE, safe);
                    return;
                }
                case 4 -> {
                    explode(level, pos, 15F, safe);
                    ExplosionLarge.spawnRubble(
                            level.getLevel(), pos.getX(), pos.getY(), pos.getZ(), 25);
                    return;
                }
                case 5 -> {
                    generateSphere7x7(level, rand, pos, work, TREASURE_OR_BROKEN, safe);
                    return;
                }
                case 6 -> {
                    generateSphere5x5(level, rand, pos, work, TWO_TREASURE_OR_BROKEN, safe);
                    return;
                }
                case 7 -> {
                    generateBox(level, rand, pos, work, TREASURE, safe);
                    return;
                }
                case 8 -> {
                    generateSphere7x7(level, rand, pos, work, TREASURE, safe);
                    generateSphere5x5(level, rand, pos, work, TOXIC, safe);
                    return;
                }
                case 9 -> {
                    generateSphere9x9(level, rand, pos, work, BROKEN, safe);
                    generateSphere7x7(level, rand, pos, work, RANDOM_ORE, safe);
                    return;
                }
                case 10 -> {
                    generateSphere5x5(level, rand, pos, work, BROKEN, safe);
                    setBlock(level, pos, work, 0, 0, 0, Palette.TAINT_AGE_9, safe);
                    return;
                }

                case 12 -> {
                    explode(level, pos, 10F, safe);
                    ItemStack blaster = new ItemStack(ModItems.GUN_B92.get());
                    blaster.set(
                            DataComponents.CUSTOM_NAME,
                            Component.translatable("desc.item.gunB92.starBlaster"));
                    level.addFreshEntity(
                            new ItemEntity(
                                    level.getLevel(),
                                    pos.getX() + 0.5D,
                                    pos.getY() + 0.5D,
                                    pos.getZ() + 0.5D,
                                    blaster));
                    return;
                }
                default -> {}
            }
        }

        switch (rand.nextInt(3)) {
            case 0 -> generateLarge(level, rand, pos, work, safe);
            case 1 -> generateMedium(level, rand, pos, work, safe);
            default -> generateSmall(level, rand, pos, work, safe);
        }
    }

    private void explode(WorldGenLevel level, BlockPos pos, float strength, boolean safeMode) {
        level.getLevel()
                .explode(
                        null,
                        null,
                        null,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        strength,
                        false,
                        safeMode
                                ? Level.ExplosionInteraction.NONE
                                : Level.ExplosionInteraction.BLOCK);
    }

    private void generateLarge(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos origin,
            BlockPos.MutableBlockPos work,
            boolean safeMode) {
        int hull = rand.nextInt(4);
        int outerPadding = 0;
        if (hull == 2) outerPadding = 1 + rand.nextInt(2);
        else if (hull == 3) outerPadding = 2;
        int innerPadding = rand.nextInt(hull == 0 ? 3 : 2);
        int core = rand.nextInt(2);
        if (innerPadding > 0) core = 2;

        int hullPalette = hullPalette(hull);
        int outerPalette = outerPaddingPalette(outerPadding);
        int innerPalette = innerPaddingPalette(innerPadding);
        int corePalette = corePalette(core);

        switch (rand.nextInt(5)) {
            case 0 -> {
                generateSphere7x7(level, rand, origin, work, hullPalette, safeMode);
                generateStar5x5(level, rand, origin, work, outerPalette, safeMode);
                generateStar3x3(level, rand, origin, work, innerPalette, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
            case 1 -> {
                generateSphere7x7(level, rand, origin, work, hullPalette, safeMode);
                generateSphere5x5(level, rand, origin, work, outerPalette, safeMode);
                generateStar3x3(level, rand, origin, work, innerPalette, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
            case 2 -> {
                generateSphere7x7(level, rand, origin, work, hullPalette, safeMode);
                generateSphere5x5(level, rand, origin, work, outerPalette, safeMode);
                generateBox(level, rand, origin, work, innerPalette, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
            case 3 -> {
                generateSphere7x7(level, rand, origin, work, hullPalette, safeMode);
                generateSphere5x5(level, rand, origin, work, outerPalette, safeMode);
                generateBox(level, rand, origin, work, innerPalette, safeMode);
                generateStar3x3(level, rand, origin, work, RANDOM_ORE, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
            default -> {
                generateSphere7x7(level, rand, origin, work, hullPalette, safeMode);
                generateSphere5x5(level, rand, origin, work, outerPalette, safeMode);
                generateStar5x5(level, rand, origin, work, innerPalette, safeMode);
                generateStar3x3(level, rand, origin, work, RANDOM_ORE, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
        }
    }

    private void generateMedium(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos origin,
            BlockPos.MutableBlockPos work,
            boolean safeMode) {
        int hull = rand.nextInt(4);
        int outerPadding = 0;
        if (hull == 2) outerPadding = 1 + rand.nextInt(2);
        else if (hull == 3) outerPadding = 2;
        int innerPadding = rand.nextInt(hull == 0 ? 3 : 2);
        int core = rand.nextInt(2);
        if (innerPadding > 0) core = 2;

        int hullPalette = hullPalette(hull);
        int outerPalette = outerPaddingPalette(outerPadding);
        int innerPalette = innerPaddingPalette(innerPadding);
        int corePalette = corePalette(core);

        int smallCorePalette = smallCorePalette(core);

        switch (rand.nextInt(6)) {
            case 0 -> {
                generateSphere5x5(level, rand, origin, work, hullPalette, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(smallCorePalette, rand), safeMode);
            }
            case 1 -> {
                generateSphere5x5(level, rand, origin, work, hullPalette, safeMode);
                generateStar3x3(level, rand, origin, work, outerPalette, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
            case 2 -> {
                generateSphere5x5(level, rand, origin, work, hullPalette, safeMode);
                generateBox(level, rand, origin, work, outerPalette, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
            case 3 -> {
                generateSphere5x5(level, rand, origin, work, hullPalette, safeMode);
                generateBox(level, rand, origin, work, outerPalette, safeMode);
                generateStar3x3(level, rand, origin, work, innerPalette, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
            case 4 -> {
                generateSphere5x5(level, rand, origin, work, hullPalette, safeMode);
                generateBox(level, rand, origin, work, innerPalette, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
            default -> {
                generateSphere5x5(level, rand, origin, work, hullPalette, safeMode);
                generateBox(level, rand, origin, work, innerPalette, safeMode);
                generateStar3x3(level, rand, origin, work, RANDOM_ORE, safeMode);
                setBlock(level, origin, work, 0, 0, 0, pick(corePalette, rand), safeMode);
            }
        }
    }

    private void generateSmall(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos origin,
            BlockPos.MutableBlockPos work,
            boolean safeMode) {
        int hull = rand.nextInt(4);
        int core = rand.nextInt(3);

        generateBox(level, rand, origin, work, hullPalette(hull), safeMode);
        setBlock(level, origin, work, 0, 0, 0, pick(smallCorePalette(core), rand), safeMode);
    }

    private static int hullPalette(int hull) {
        return switch (hull) {
            case 0 -> MOLTEN;
            case 1 -> COBBLE;
            case 2 -> BROKEN_99_TREASURE;
            case 3 -> MOLTEN_OR_BROKEN;
            default -> throw new IllegalArgumentException("unknown meteor hull: " + hull);
        };
    }

    private static int outerPaddingPalette(int outerPadding) {
        return switch (outerPadding) {
            case 0 -> COBBLE;
            case 1 -> BROKEN_99_TREASURE;
            case 2 -> COBBLE_OR_BROKEN;
            default ->
                    throw new IllegalArgumentException(
                            "unknown meteor outer padding: " + outerPadding);
        };
    }

    private static int innerPaddingPalette(int innerPadding) {
        return switch (innerPadding) {
            case 0 -> BROKEN_99_TREASURE;
            case 1 -> BROKEN;
            default -> COBBLE;
        };
    }

    private static int corePalette(int core) {
        return switch (core) {
            case 0 -> METEOR;
            case 1 -> TREASURE;
            default -> RANDOM_ORE;
        };
    }

    private static int smallCorePalette(int core) {
        return switch (core) {
            case 0 -> METEOR;
            case 1 -> TREASURE;
            default -> METEOR_OR_TREASURE;
        };
    }

    private void generateSphere9x9(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos o,
            BlockPos.MutableBlockPos work,
            int palette,
            boolean safeMode) {
        for (int a = -4; a < 5; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -1; a < 2; a++)
            for (int b = -4; b < 5; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -1; a < 2; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -4; c < 5; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -1; a < 2; a++)
            for (int b = -3; b < 4; b++)
                for (int c = -3; c < 4; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -3; a < 4; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -3; c < 4; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -3; a < 4; a++)
            for (int b = -3; b < 4; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -3; a < 4; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -2; c < 3; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -2; a < 3; a++)
            for (int b = -3; b < 4; b++)
                for (int c = -2; c < 3; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -2; a < 3; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -3; c < 4; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
    }

    private void generateSphere7x7(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos o,
            BlockPos.MutableBlockPos work,
            int palette,
            boolean safeMode) {
        for (int a = -3; a < 4; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -1; a < 2; a++)
            for (int b = -3; b < 4; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -1; a < 2; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -3; c < 4; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -2; a < 3; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -1; a < 2; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -2; c < 3; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -2; a < 3; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -2; c < 3; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
    }

    private void generateSphere5x5(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos o,
            BlockPos.MutableBlockPos work,
            int palette,
            boolean safeMode) {
        for (int a = -2; a < 3; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -1; a < 2; a++)
            for (int b = -2; b < 3; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
        for (int a = -1; a < 2; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -2; c < 3; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
    }

    private void generateBox(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos o,
            BlockPos.MutableBlockPos work,
            int palette,
            boolean safeMode) {
        for (int a = -1; a < 2; a++)
            for (int b = -1; b < 2; b++)
                for (int c = -1; c < 2; c++)
                    setBlock(level, o, work, a, b, c, pick(palette, rand), safeMode);
    }

    private void generateStar5x5(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos o,
            BlockPos.MutableBlockPos work,
            int palette,
            boolean safeMode) {
        generateBox(level, rand, o, work, palette, safeMode);
        setBlock(level, o, work, 2, 0, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, -2, 0, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, 0, 2, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, 0, -2, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, 0, 0, 2, pick(palette, rand), safeMode);
        setBlock(level, o, work, 0, 0, -2, pick(palette, rand), safeMode);
    }

    private void generateStar3x3(
            WorldGenLevel level,
            RandomSource rand,
            BlockPos o,
            BlockPos.MutableBlockPos work,
            int palette,
            boolean safeMode) {
        setBlock(level, o, work, 0, 0, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, 1, 0, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, -1, 0, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, 0, 1, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, 0, -1, 0, pick(palette, rand), safeMode);
        setBlock(level, o, work, 0, 0, 1, pick(palette, rand), safeMode);
        setBlock(level, o, work, 0, 0, -1, pick(palette, rand), safeMode);
    }

    private void setBlock(
            WorldGenLevel level,
            BlockPos origin,
            BlockPos.MutableBlockPos pos,
            int dx,
            int dy,
            int dz,
            BlockState state,
            boolean safeMode) {
        pos.setWithOffset(origin, dx, dy, dz);
        BlockState target = level.getBlockState(pos);

        if (safeMode && !target.canBeReplaced() && !target.is(HbmBlockTags.METEORITE_REPLACEABLE))
            return;

        float hardness = target.getDestroySpeed(level, pos);
        if (hardness != -1F && hardness < 10_000F) level.setBlock(pos, state, 2);
    }

    private static final class Palette {
        private static final BlockState METEOR = bs(ModBlocks.BLOCK_METEOR);
        private static final BlockState MOLTEN = bs(ModBlocks.BLOCK_METEOR_MOLTEN);
        private static final BlockState COBBLE = bs(ModBlocks.BLOCK_METEOR_COBBLE);
        private static final BlockState BROKEN = bs(ModBlocks.BLOCK_METEOR_BROKEN);
        private static final BlockState TREASURE = bs(ModBlocks.BLOCK_METEOR_TREASURE);
        private static final BlockState TOXIC = bs(ModBlocks.TOXIC_BLOCK);
        private static final BlockState TAINT = bs(ModBlocks.TAINT);
        private static final BlockState TAINT_AGE_9 = TAINT.setValue(BlockTaint.AGE, 9);
        private static final BlockState ORE_IRON = bs(ModBlocks.ORE_METEOR_IRON);
        private static final BlockState ORE_COPPER = bs(ModBlocks.ORE_METEOR_COPPER);
        private static final BlockState ORE_ALUMINIUM = bs(ModBlocks.ORE_METEOR_ALUMINIUM);
        private static final BlockState ORE_RAREEARTH = bs(ModBlocks.ORE_METEOR_RAREEARTH);
        private static final BlockState ORE_COBALT = bs(ModBlocks.ORE_METEOR_COBALT);
    }
}
