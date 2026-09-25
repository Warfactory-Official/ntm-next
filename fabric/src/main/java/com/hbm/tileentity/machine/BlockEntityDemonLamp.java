// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlockEntityDemonLamp extends BlockEntity {

    public static final float RADS = 100000F;
    public static final double RANGE = 25D;
    public static final double BURN_RANGE = 2D;
    public static final float BURN_DAMAGE = 100F;

    public BlockEntityDemonLamp(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEMON_LAMP.get(), pos, state);
    }

    public static void tickServer(BlockEntityDemonLamp lamp) {
        radiate((ServerLevel) lamp.level, lamp.worldPosition);
    }

    public static void radiate(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;
        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();
        for (LivingEntity e :
                level.getEntitiesOfClass(
                        LivingEntity.class, new AABB(cx, cy, cz, cx, cy, cz).inflate(RANGE))) {
            Vec3 vec = new Vec3(e.getX() - cx, e.getEyeY() - cy, e.getZ() - cz);
            double len = vec.length();
            vec = vec.normalize();

            float res = 0F;
            for (int i = 1; i < len; i++) {
                cell.set(
                        Mth.floor(cx + vec.x * i),
                        Mth.floor(cy + vec.y * i),
                        Mth.floor(cz + vec.z * i));
                res += level.getBlockState(cell).getBlock().getExplosionResistance();
            }
            if (res < 1F) res = 1F;

            float eRads = RADS;
            eRads /= res;
            eRads /= (float) (len * len);
            ContaminationUtil.contaminate(
                    e, HazardType.RADIATION, ContaminationType.CREATIVE, eRads);

            if (len < BURN_RANGE) e.hurtServer(level, level.damageSources().inFire(), BURN_DAMAGE);
        }
    }
}
