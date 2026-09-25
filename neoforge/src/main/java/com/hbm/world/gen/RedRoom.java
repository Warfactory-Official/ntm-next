// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.world.gen;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockLoot.TileEntityLoot;
import com.hbm.blocks.generic.BlockRedBrick;
import com.hbm.blocks.generic.BlockRedBrickKeyhole;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPools;
import com.hbm.items.ModItems;
import com.hbm.tileentity.BlockEntityPedestal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class RedRoom {
    private RedRoom() {}

    public static void open(
            ServerLevel level,
            BlockPos keyhole,
            Direction side,
            boolean black,
            RandomSource random) {
        BlockPos center = keyhole.relative(side, -4).below(2);
        if (black) black(level, center.getX(), center.getY(), center.getZ(), side, random);
        else red(level, center.getX(), center.getY(), center.getZ(), random);
        door(level, keyhole.below(), side.getOpposite());
    }

    private static void red(ServerLevel level, int x, int y, int z, RandomSource random) {
        int size = 9;
        int height = 5;
        int width = size / 2;

        for (int i = -width; i <= width; i++) {
            set(level, x + i, y, z + width, brick(6));
            set(level, x + i, y, z - width, brick(6));
            set(level, x + width, y, z + i, brick(6));
            set(level, x - width, y, z + i, brick(6));
            set(level, x + i, y + height - 1, z + width, brick(6));
            set(level, x + i, y + height - 1, z - width, brick(6));
            set(level, x + width, y + height - 1, z + i, brick(6));
            set(level, x - width, y + height - 1, z + i, brick(6));
        }

        for (int i = 1; i <= height - 2; i++) {

            set(level, x + width, y + i, z + width, brick(6));
            set(level, x + width, y + i, z - width, brick(6));
            set(level, x - width, y + i, z + width, brick(6));
            set(level, x - width, y + i, z - width, brick(6));

            for (int j = -width + 1; j <= width - 1; j++) {
                set(level, x + width, y + i, z + j, brick(4));
                set(level, x - width, y + i, z + j, brick(5));
                set(level, x + j, y + i, z + width, brick(2));
                set(level, x + j, y + i, z - width, brick(3));
            }
        }

        if (random.nextInt(1) == 0) {
            int r = random.nextInt(4);
            if (r == 0)
                set(
                        level,
                        x + width,
                        y + 2,
                        z,
                        ModBlocks.STONE_KEYHOLE_META
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        BlockRedBrickKeyhole.FACING, Direction.from3DDataValue(4)));
            if (r == 1)
                set(
                        level,
                        x - width,
                        y + 2,
                        z,
                        ModBlocks.STONE_KEYHOLE_META
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        BlockRedBrickKeyhole.FACING, Direction.from3DDataValue(5)));
            if (r == 2)
                set(
                        level,
                        x,
                        y + 2,
                        z + width,
                        ModBlocks.STONE_KEYHOLE_META
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        BlockRedBrickKeyhole.FACING, Direction.from3DDataValue(2)));
            if (r == 3)
                set(
                        level,
                        x,
                        y + 2,
                        z - width,
                        ModBlocks.STONE_KEYHOLE_META
                                .get()
                                .defaultBlockState()
                                .setValue(
                                        BlockRedBrickKeyhole.FACING, Direction.from3DDataValue(3)));
        }

        for (int i = -width + 1; i <= width - 1; i++) {
            for (int j = -width + 1; j <= width - 1; j++) {

                set(level, x + i, y, z + j, brick(1));
                set(level, x + i, y + height - 1, z + j, brick(0));

                for (int k = 1; k <= height - 2; k++) {
                    set(level, x + i, y + k, z + j, Blocks.AIR.defaultBlockState());
                }
            }
        }

        int torchDist = width - 1;
        int torchOff = torchDist - 1;
        torch(level, x + torchDist, y + 2, z + torchOff);
        torch(level, x + torchDist, y + 2, z - torchOff);
        torch(level, x - torchDist, y + 2, z + torchOff);
        torch(level, x - torchDist, y + 2, z - torchOff);
        torch(level, x + torchOff, y + 2, z + torchDist);
        torch(level, x - torchOff, y + 2, z + torchDist);
        torch(level, x + torchOff, y + 2, z - torchDist);
        torch(level, x - torchOff, y + 2, z - torchDist);

        if (random.nextInt(4) == 0) {
            for (int i = -width + 1; i <= width - 1; i++) {
                for (int j = -width + 1; j <= width - 1; j++) {
                    if (random.nextBoolean())
                        set(level, x + i, y + height - 2, z + j, Blocks.COBWEB.defaultBlockState());
                }
            }
        }

        if (random.nextInt(4) == 0) {
            for (int i = 1; i <= height - 2; i++) {
                set(
                        level,
                        x + width - 2,
                        y + i,
                        z + width - 2,
                        ModBlocks.CONCRETE_RED.get().defaultBlockState());
                set(
                        level,
                        x + width - 2,
                        y + i,
                        z - width + 2,
                        ModBlocks.CONCRETE_RED.get().defaultBlockState());
                set(
                        level,
                        x - width + 2,
                        y + i,
                        z + width - 2,
                        ModBlocks.CONCRETE_RED.get().defaultBlockState());
                set(
                        level,
                        x - width + 2,
                        y + i,
                        z - width + 2,
                        ModBlocks.CONCRETE_RED.get().defaultBlockState());
            }
        }

        if (random.nextInt(4) == 0) {
            set(level, x + width - 1, y, z + width - 1, Blocks.NETHERRACK.defaultBlockState());
            set(level, x + width - 1, y, z - width + 1, Blocks.NETHERRACK.defaultBlockState());
            set(level, x - width + 1, y, z + width - 1, Blocks.NETHERRACK.defaultBlockState());
            set(level, x - width + 1, y, z - width + 1, Blocks.NETHERRACK.defaultBlockState());
            set(level, x + width - 1, y + 1, z + width - 1, Blocks.FIRE.defaultBlockState());
            set(level, x + width - 1, y + 1, z - width + 1, Blocks.FIRE.defaultBlockState());
            set(level, x - width + 1, y + 1, z + width - 1, Blocks.FIRE.defaultBlockState());
            set(level, x - width + 1, y + 1, z - width + 1, Blocks.FIRE.defaultBlockState());
        }

        if (random.nextInt(4) == 0) {
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0)
                        set(
                                level,
                                x + i,
                                y,
                                z + j,
                                ModBlocks.CONCRETE_RED.get().defaultBlockState());
                }
            }
        }

        if (random.nextInt(4) == 0) {
            set(level, x + width - 2, y, z + width - 1, Blocks.LAVA.defaultBlockState());
            set(level, x + width - 3, y, z + width - 1, Blocks.LAVA.defaultBlockState());
            set(level, x - width + 2, y, z + width - 1, Blocks.LAVA.defaultBlockState());
            set(level, x - width + 3, y, z + width - 1, Blocks.LAVA.defaultBlockState());
            set(level, x + width - 2, y, z - width + 1, Blocks.LAVA.defaultBlockState());
            set(level, x + width - 3, y, z - width + 1, Blocks.LAVA.defaultBlockState());
            set(level, x - width + 2, y, z - width + 1, Blocks.LAVA.defaultBlockState());
            set(level, x - width + 3, y, z - width + 1, Blocks.LAVA.defaultBlockState());
            set(level, x + width - 1, y, z + width - 2, Blocks.LAVA.defaultBlockState());
            set(level, x + width - 1, y, z + width - 3, Blocks.LAVA.defaultBlockState());
            set(level, x + width - 1, y, z - width + 2, Blocks.LAVA.defaultBlockState());
            set(level, x + width - 1, y, z - width + 3, Blocks.LAVA.defaultBlockState());
            set(level, x - width + 1, y, z + width - 2, Blocks.LAVA.defaultBlockState());
            set(level, x - width + 1, y, z + width - 3, Blocks.LAVA.defaultBlockState());
            set(level, x - width + 1, y, z - width + 2, Blocks.LAVA.defaultBlockState());
            set(level, x - width + 1, y, z - width + 3, Blocks.LAVA.defaultBlockState());
        }

        int rand = random.nextInt(20);

        if (rand == 0) {
            set(level, x, y + 1, z, ModBlocks.LOOT.get().defaultBlockState());
            TileEntityLoot loot = (TileEntityLoot) level.getBlockEntity(new BlockPos(x, y + 1, z));

            if (random.nextInt(5) == 0) {
                loot.addItem(new ItemStack(ModItems.NCRPA_HELMET.get()), 0, 0, 0);
                loot.addItem(new ItemStack(ModItems.NCRPA_PLATE.get()), 0, 0, 0);
                loot.addItem(new ItemStack(ModItems.NCRPA_LEGS.get()), 0, 0, 0);
                loot.addItem(new ItemStack(ModItems.NCRPA_BOOTS.get()), 0, 0, 0);
            } else {
                loot.addItem(new ItemStack(ModItems.TRENCHMASTER_HELMET.get()), 0, 0, 0);
                loot.addItem(new ItemStack(ModItems.TRENCHMASTER_PLATE.get()), 0, 0, 0);
                loot.addItem(new ItemStack(ModItems.TRENCHMASTER_LEGS.get()), 0, 0, 0);
                loot.addItem(new ItemStack(ModItems.TRENCHMASTER_BOOTS.get()), 0, 0, 0);
            }
        } else {
            pedestal(level, x, y + 1, z, ItemPool.getUnit(ItemPools.POOL_RED_PEDESTAL, random));
        }

        clearItems(level, x, y, z);
    }

    private static void black(
            ServerLevel level, int x, int y, int z, Direction dir, RandomSource random) {
        int size = 9;
        int height = 5;
        int width = size / 2;

        for (int i = -width; i <= width; i++) {
            set(level, x + i, y, z + width, brick(6));
            set(level, x + i, y, z - width, brick(6));
            set(level, x + width, y, z + i, brick(6));
            set(level, x - width, y, z + i, brick(6));
            set(level, x + i, y + height - 1, z + width, brick(6));
            set(level, x + i, y + height - 1, z - width, brick(6));
            set(level, x + width, y + height - 1, z + i, brick(6));
            set(level, x - width, y + height - 1, z + i, brick(6));
        }

        for (int i = 1; i <= height - 2; i++) {

            set(level, x + width, y + i, z + width, brick(6));
            set(level, x + width, y + i, z - width, brick(6));
            set(level, x - width, y + i, z + width, brick(6));
            set(level, x - width, y + i, z - width, brick(6));

            for (int j = -width + 1; j <= width - 1; j++) {
                if (dir != Direction.EAST) set(level, x + width, y + i, z + j, brick(6));
                if (dir != Direction.WEST) set(level, x - width, y + i, z + j, brick(6));
                if (dir != Direction.SOUTH) set(level, x + j, y + i, z + width, brick(6));
                if (dir != Direction.NORTH) set(level, x + j, y + i, z - width, brick(6));
            }
        }

        for (int i = -width + 1; i <= width - 1; i++) {
            for (int j = -width + 1; j <= width - 1; j++) {

                set(level, x + i, y, z + j, brick(6));
                set(level, x + i, y + height - 1, z + j, brick(6));

                for (int k = 1; k <= height - 2; k++) {
                    set(level, x + i, y + k, z + j, Blocks.AIR.defaultBlockState());
                }
            }
        }

        pedestal(level, x, y + 1, z, ItemPool.getStack(ItemPools.POOL_BLACK_SLAB, random));
        if (random.nextBoolean())
            pedestal(level, x + 2, y + 1, z, ItemPool.getStack(ItemPools.POOL_BLACK_PART, random));
        if (random.nextBoolean())
            pedestal(level, x - 2, y + 1, z, ItemPool.getStack(ItemPools.POOL_BLACK_PART, random));
        if (random.nextBoolean())
            pedestal(level, x, y + 1, z + 2, ItemPool.getStack(ItemPools.POOL_BLACK_PART, random));
        if (random.nextBoolean())
            pedestal(level, x, y + 1, z - 2, ItemPool.getStack(ItemPools.POOL_BLACK_PART, random));

        clearItems(level, x, y, z);
    }

    private static BlockState brick(int face) {
        return ModBlocks.BRICK_RED.get().defaultBlockState().setValue(BlockRedBrick.FACE, face);
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
    }

    private static void torch(ServerLevel level, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);

        set(
                level,
                x,
                y,
                z,
                Blocks.WALL_TORCH.getStateForPlacement(
                        new DirectionalPlaceContext(
                                level, pos, Direction.DOWN, ItemStack.EMPTY, Direction.UP)));
    }

    private static void pedestal(ServerLevel level, int x, int y, int z, ItemStack stack) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = ModBlocks.PEDESTAL.get().defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_ALL);
        BlockEntityPedestal pedestal = (BlockEntityPedestal) level.getBlockEntity(pos);
        pedestal.item = stack;
        pedestal.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private static void clearItems(ServerLevel level, int x, int y, int z) {
        for (ItemEntity item :
                level.getEntitiesOfClass(
                        ItemEntity.class, new AABB(x - 4, y, z - 4, x + 5, y + 5, z + 5)))
            item.discard();
    }

    private static void door(ServerLevel level, BlockPos pos, Direction facing) {
        DoorBlock block = ModBlocks.DOOR_RED.get();

        level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        BlockState state =
                block.getStateForPlacement(
                        new DirectionalPlaceContext(
                                level, pos, facing, ItemStack.EMPTY, Direction.UP));
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
        level.updateNeighborsAt(pos, block);
    }
}
