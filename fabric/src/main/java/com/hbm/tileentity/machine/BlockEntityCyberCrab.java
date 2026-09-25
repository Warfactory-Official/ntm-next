// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.EntityCyberCrab;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BlockEntityCyberCrab extends BlockEntity {

    private int age;

    public BlockEntityCyberCrab(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRABS.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BlockEntityCyberCrab be) {
        be.serverTick((ServerLevel) level);
    }

    private void serverTick(ServerLevel level) {
        age++;
        double x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        if (age > 200
                && level.getBlockState(worldPosition.above()).isAir()
                && level.getNearestPlayer(x + 0.5, y + 1, z + 0.5, 25, EntitySelector.NO_SPECTATORS)
                        != null) {
            if (level.getEntitiesOfClass(
                                    EntityCyberCrab.class,
                                    new AABB(x - 5, y - 2, z - 5, x + 6, y + 4, z + 6))
                            .size()
                    < 5) {
                EntityCyberCrab crab =
                        Objects.requireNonNull(
                                (level.getRandom().nextInt(5) == 0
                                                ? ModEntities.TESLA_CRAB
                                                : ModEntities.CYBER_CRAB)
                                        .get()
                                        .create(level, EntitySpawnReason.SPAWNER));
                crab.setPos(x + 0.5, y + 1, z + 0.5);
                level.addFreshEntity(crab);
            }
            age = 0;
        }
    }
}
