// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.bomb.BlockCrashedBomb.EnumDudType;
import com.hbm.blocks.bomb.BlockCrashedBomb;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class TileEntityCrashedBomb extends BlockEntity {

    public TileEntityCrashedBomb(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRASHED_BALEFIRE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityCrashedBomb be) {
        be.serverTick((ServerLevel) level, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (!TickPhase.every(this, 2)) return;
        if (!(state.getBlock() instanceof BlockCrashedBomb bomb)) return;
        EnumDudType type = bomb.type;

        if (type == EnumDudType.BALEFIRE) affectEntities(level, pos, 15D, 1F);
        if (type == EnumDudType.NUKE) affectEntities(level, pos, 10D, 0.25F);
        if (type == EnumDudType.SALTED) affectEntities(level, pos, 10D, 0.5F);
    }

    private void affectEntities(ServerLevel level, BlockPos pos, double range, float maxIntensity) {
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        AABB box = new AABB(cx, cy, cz, cx, cy, cz).inflate(range);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
        for (LivingEntity entity : entities) {

            double dx = cx - entity.getX();
            double dy = cy - (entity.getY() + entity.getBbHeight() / 2);
            double dz = cz - entity.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > range) continue;
            float intensity = (float) (maxIntensity * (1D - dist / range));
            ContaminationUtil.contaminate(
                    entity, HazardType.RADIATION, ContaminationType.CREATIVE, intensity);
        }
    }
}
