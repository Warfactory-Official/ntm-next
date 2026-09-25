// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ExplosionDrying {

    public int posX;
    public int posY;
    public int posZ;
    public int lastposX = 0;
    public int lastposZ = 0;
    public int radius;
    public int radius2;
    public Level worldObj;
    public float explosionCoefficient = 1.0F;
    public float explosionCoefficient2 = 1.0F;
    public UUID detonator;
    private int n = 1;
    private int nlimit;
    private int shell;
    private int leg;
    private int element;

    public ExplosionDrying(
            int x, int y, int z, Level world, int rad, float coefficient, float coefficient2) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.worldObj = world;
        this.radius = rad;
        this.radius2 = rad * rad;
        this.explosionCoefficient = coefficient;
        this.explosionCoefficient2 = coefficient2;
        this.nlimit = this.radius2 * 4;
    }

    public boolean update() {
        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(this.n) + 1) / 2);
        this.shell = Math.max(this.shell, 1);
        int shell2 = this.shell * 2;
        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
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
        int dist = this.radius2 - (x * x + z * z);
        if (dist > 0) {
            dist = (int) Math.sqrt(dist);
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int y = (int) (dist / this.explosionCoefficient2);
                    y > -dist / this.explosionCoefficient;
                    y--) {
                pos.set(this.posX + x, this.posY + y, this.posZ + z);
                BlockState state = this.worldObj.getBlockState(pos);
                if (!state.isAir() && !state.getFluidState().isEmpty()) {
                    this.worldObj.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    public void saveToNbt(CompoundTag nbt, String name) {
        nbt.putInt(name + "posX", posX);
        nbt.putInt(name + "posY", posY);
        nbt.putInt(name + "posZ", posZ);
        nbt.putInt(name + "lastposX", lastposX);
        nbt.putInt(name + "lastposZ", lastposZ);
        nbt.putInt(name + "radius", radius);
        nbt.putInt(name + "radius2", radius2);
        nbt.putInt(name + "n", n);
        nbt.putInt(name + "nlimit", nlimit);
        nbt.putInt(name + "shell", shell);
        nbt.putInt(name + "leg", leg);
        nbt.putInt(name + "element", element);
        nbt.putFloat(name + "explosionCoefficient", explosionCoefficient);
        nbt.putFloat(name + "explosionCoefficient2", explosionCoefficient2);
    }

    public void readFromNbt(CompoundTag nbt, String name) {
        posX = nbt.getIntOr(name + "posX", 0);
        posY = nbt.getIntOr(name + "posY", 0);
        posZ = nbt.getIntOr(name + "posZ", 0);
        lastposX = nbt.getIntOr(name + "lastposX", 0);
        lastposZ = nbt.getIntOr(name + "lastposZ", 0);
        radius = nbt.getIntOr(name + "radius", 0);
        radius2 = nbt.getIntOr(name + "radius2", 0);
        n = nbt.getIntOr(name + "n", 1);
        nlimit = nbt.getIntOr(name + "nlimit", 0);
        shell = nbt.getIntOr(name + "shell", 0);
        leg = nbt.getIntOr(name + "leg", 0);
        element = nbt.getIntOr(name + "element", 0);
        explosionCoefficient = nbt.getFloatOr(name + "explosionCoefficient", 1.0F);
        explosionCoefficient2 = nbt.getFloatOr(name + "explosionCoefficient2", 1.0F);
    }
}
