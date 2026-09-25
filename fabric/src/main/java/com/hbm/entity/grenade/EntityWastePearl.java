// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.grenade;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EntityWastePearl extends EntityGrenadeBase {

    public EntityWastePearl(EntityType<? extends EntityWastePearl> type, Level level) {
        super(type, level);
    }

    public EntityWastePearl(Level level) {
        this(ModEntities.WASTE_PEARL.get(), level);
    }

    public EntityWastePearl(Level level, LivingEntity thrower) {
        this(level);
        initThrower(thrower);
    }

    public EntityWastePearl(Level level, double x, double y, double z) {
        this(level);
        setPos(x, y, z);
    }

    @Override
    public void explode() {

        if (level().isClientSide()) return;

        this.discard();

        int x = Mth.floor(getX());
        int y = Mth.floor(getY());
        int z = Mth.floor(getZ());

        Block fallout = ModBlocks.FALLOUT.get();
        Block radon = ModBlocks.GAS_RADON.get();
        Block radonDense = ModBlocks.GAS_RADON_DENSE.get();

        for (int ix = x - 3; ix <= x + 3; ix++) {
            for (int iy = y - 3; iy <= y + 3; iy++) {
                for (int iz = z - 3; iz <= z + 3; iz++) {

                    BlockPos pos = new BlockPos(ix, iy, iz);
                    BlockState state = level().getBlockState(pos);
                    BlockState falloutState = fallout.defaultBlockState();

                    if (random.nextInt(3) == 0
                            && state.canBeReplaced()
                            && falloutState.canSurvive(level(), pos)) {
                        level().setBlockAndUpdate(pos, falloutState);
                    } else if (state.isAir()) {
                        level().setBlockAndUpdate(
                                        pos,
                                        random.nextBoolean()
                                                ? radon.defaultBlockState()
                                                : radonDense.defaultBlockState());
                    }
                }
            }
        }
    }
}
