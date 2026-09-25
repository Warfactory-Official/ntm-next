// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ZirnoxDestroyed;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class BlockEntityZirnoxDestroyed extends BlockEntity {

    public boolean onFire = true;

    private int waveTimer;

    public BlockEntityZirnoxDestroyed(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX_DESTROYED.get(), pos, state);
    }

    public static void tick(
            Level level, BlockPos pos, BlockState state, BlockEntityZirnoxDestroyed be) {
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        ServerLevel server = (ServerLevel) level;
        RandomSource rand = server.getRandom();

        radiate(server, pos);

        if (rand.nextInt(5000) == 0) onFire = false;

        if (--waveTimer <= 0) {
            waveTimer = 100 + rand.nextInt(20);
            ((ZirnoxDestroyed) getBlockState().getBlock())
                    .visitCells(
                            pos,
                            getBlockState().getValue(BlockMultiblockCore.FACING),
                            (cell, mask) -> {
                                BlockPos vent = cell.above();
                                BlockState above = server.getBlockState(vent);
                                if (above.isAir()) {
                                    if (rand.nextInt(10) == 0)
                                        server.setBlockAndUpdate(
                                                vent,
                                                ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
                                } else if (above.is(ModBlocks.FOAM_LAYER.get())
                                        || above.is(ModBlocks.BLOCK_FOAM.get())) {
                                    if (rand.nextInt(25) == 0) onFire = false;
                                }

                                if (rand.nextInt(10) == 0 && server.getBlockState(vent).isAir()) {
                                    server.setBlockAndUpdate(
                                            vent, ModBlocks.GAS_MELTDOWN.get().defaultBlockState());
                                }
                            });
        }

        if (onFire && TickPhase.every(this, 50)) {
            ZirnoxDestroyed.flame(server, pos);
            server.playSound(
                    null,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    SoundEvents.FIRE_AMBIENT,
                    SoundSource.BLOCKS,
                    1.0F + rand.nextFloat(),
                    rand.nextFloat() * 0.7F + 0.3F);
        }
    }

    private void radiate(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5D, cy = pos.getY() + 0.5D, cz = pos.getZ() + 0.5D;

        ContaminationUtil.radiate(
                level,
                cx,
                cy,
                cz,
                100D,
                onFire ? 500000F : 75000F,
                ContaminationUtil.ContaminationType.CREATIVE);

        if (onFire) {
            AABB box = new AABB(cx, cy, cz, cx, cy, cz).inflate(5D);
            List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, box);
            DamageSource fire = level.damageSources().onFire();
            for (LivingEntity e : near) {
                if (e.distanceToSqr(cx, cy, cz) < 25D) e.hurtServer(level, fire, 2F);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        onFire = input.getBooleanOr("onFire", true);
        waveTimer = input.getIntOr("waveTimer", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("onFire", onFire);
        output.putInt("waveTimer", waveTimer);
    }
}
