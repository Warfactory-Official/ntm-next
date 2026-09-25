// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ExplosionBalefire {

    public int posX;
    public int posY;
    public int posZ;
    public int lastposX = 0;
    public int lastposZ = 0;
    public int radius;
    public int radius2;
    public Level level;
    private int n = 1;
    private int nlimit;
    private int shell;
    private int leg;
    private int element;

    public ExplosionBalefire(int x, int y, int z, Level level, int rad) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;

        this.level = level;

        this.radius = rad;
        this.radius2 = this.radius * this.radius;

        this.nlimit = this.radius2 * 4;
    }

    public void saveToNbt(ValueOutput output, String name) {
        output.putInt(name + "posX", posX);
        output.putInt(name + "posY", posY);
        output.putInt(name + "posZ", posZ);
        output.putInt(name + "lastposX", lastposX);
        output.putInt(name + "lastposZ", lastposZ);
        output.putInt(name + "radius", radius);
        output.putInt(name + "radius2", radius2);
        output.putInt(name + "n", n);
        output.putInt(name + "nlimit", nlimit);
        output.putInt(name + "shell", shell);
        output.putInt(name + "leg", leg);
        output.putInt(name + "element", element);
    }

    public void readFromNbt(ValueInput input, String name) {
        posX = input.getIntOr(name + "posX", 0);
        posY = input.getIntOr(name + "posY", 0);
        posZ = input.getIntOr(name + "posZ", 0);
        lastposX = input.getIntOr(name + "lastposX", 0);
        lastposZ = input.getIntOr(name + "lastposZ", 0);
        radius = input.getIntOr(name + "radius", 0);
        radius2 = input.getIntOr(name + "radius2", 0);
        n = Math.max(input.getIntOr(name + "n", 1), 1);
        nlimit = input.getIntOr(name + "nlimit", 0);
        shell = input.getIntOr(name + "shell", 0);
        leg = input.getIntOr(name + "leg", 0);
        element = input.getIntOr(name + "element", 0);
    }

    public boolean update() {

        if (n == 0) return true;

        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
        int shell2 = this.shell * 2;

        if (shell2 == 0) return true;

        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / (double) shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX =
                this.leg == 0
                        ? this.shell
                        : this.leg == 1
                                ? -this.element
                                : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ =
                this.leg == 0
                        ? this.element
                        : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;
        return this.n > this.nlimit;
    }

    private void breakColumn(int x, int z) {
        int dist = (int) (radius - Math.sqrt(x * x + z * z));

        if (dist <= 0) return;

        int pX = posX + x;
        int pZ = posZ + z;

        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, pX, pZ);
        int maxdepth = (int) (10 + radius * 0.25);
        int depth = (int) ((maxdepth * dist / (double) radius) + (Math.sin(dist * 0.15 + 2) * 2));

        depth = Math.max(y - depth, level.getMinY());

        while (y > depth) {

            BlockPos pos = new BlockPos(pX, y, pZ);
            BlockState state = level.getBlockState(pos);

            if (state.is(ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get())) {
                if (level.getRandom().nextInt(10) == 0) {
                    level.setBlockAndUpdate(
                            pos.above(), ModBlocks.BALEFIRE.get().defaultBlockState());
                    level.setBlock(
                            pos,
                            copyPillarAxis(state, ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()),
                            Block.UPDATE_ALL);
                }
                return;
            }

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

            y--;
        }

        if (level.getRandom().nextInt(10) == 0) {
            level.setBlockAndUpdate(
                    new BlockPos(pX, depth + 1, pZ), ModBlocks.BALEFIRE.get().defaultBlockState());

            BlockPos floor = new BlockPos(pX, y, pZ);
            BlockState state = level.getBlockState(floor);
            if (state.is(ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get())) {
                level.setBlock(
                        floor,
                        copyPillarAxis(state, ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get()),
                        Block.UPDATE_ALL);
            }
        }

        for (int i = depth; i > depth - 5; i--) {
            BlockPos pos = new BlockPos(pX, i, pZ);
            if (level.getBlockState(pos).is(Blocks.STONE)) {
                level.setBlock(
                        pos,
                        ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }
    }

    private static BlockState copyPillarAxis(BlockState from, Block to) {
        BlockState result = to.defaultBlockState();
        if (from.hasProperty(BlockStateProperties.AXIS)
                && result.hasProperty(BlockStateProperties.AXIS)) {
            return result.setValue(
                    BlockStateProperties.AXIS, from.getValue(BlockStateProperties.AXIS));
        }
        return result;
    }
}
