// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockGeysir;
import com.hbm.entity.particle.EntityOrangeFX;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.toclient.EffectNTPayload;
import com.hbm.particle.HbmEffectNT;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BlockEntityGeysir extends BlockEntity {

    private static final int RANGE = 32;

    private int timer;

    public BlockEntityGeysir(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEYSIR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityGeysir be) {
        be.serverTick(level, pos, state);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {

        if (!level.getBlockState(pos.above()).isAir()) return;
        if (!(state.getBlock() instanceof BlockGeysir)) return;

        timer--;
        boolean active = state.getValue(BlockGeysir.ACTIVE);
        boolean chlorine = state.is(ModBlocks.GEYSIR_CHLORINE.get());

        if (timer <= 0) {

            timer = getDelay(chlorine, active, level.getRandom());
            level.setBlock(pos, state.setValue(BlockGeysir.ACTIVE, !active), Block.UPDATE_CLIENTS);
        }

        if (active) {

            if (chlorine) chlorine(level, pos);
            else fire((ServerLevel) level, pos);
        }
    }

    private int getDelay(boolean chlorine, boolean active, RandomSource rand) {
        if (chlorine) return active ? (400 + rand.nextInt(100)) : 20;
        return active ? (80 + rand.nextInt(60)) : (rand.nextBoolean() ? 300 : 450);
    }

    private void chlorine(Level level, BlockPos pos) {
        RandomSource rand = level.getRandom();
        for (int i = 0; i < 3; i++) {
            EntityOrangeFX fx =
                    new EntityOrangeFX(
                            level,
                            pos.getX() + 0.5D,
                            pos.getY() + 1.5D,
                            pos.getZ() + 0.5D,
                            0D,
                            0D,
                            0D);
            fx.setDeltaMovement(
                    rand.nextGaussian() * 0.45D, timer * 0.3D, rand.nextGaussian() * 0.45D);
            level.addFreshEntity(fx);
        }
    }

    private void fire(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;
        AABB box = new AABB(cx, cy, cz, cx, cy, cz).inflate(RANGE);

        if (level.getEntitiesOfClass(Player.class, box).isEmpty()) return;

        RandomSource rand = level.getRandom();
        if (rand.nextInt(3) == 0) {
            EntityShrapnel shrap = new EntityShrapnel(level, cx, pos.getY() + 1.5D, cz);
            shrap.setDeltaMovement(
                    rand.nextGaussian() * 0.05D,
                    0.5D + rand.nextDouble() * timer * 0.01D,
                    rand.nextGaussian() * 0.05D);
            level.addFreshEntity(shrap);
        }

        if (timer % 2 == 0) {
            double px = cx, py = pos.getY() + 1.1D, pz = cz;
            Services.NETWORK.sendToAllAround(
                    new EffectNTPayload(HbmEffectNT.GasFlame, px, py, pz),
                    new TargetPoint(level, px, py, pz, 75));
        }
    }
}
