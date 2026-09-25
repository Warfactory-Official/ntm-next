// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.extprop.ContaminationEffect;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.potion.HbmPotion;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BlockEntityDecon extends BlockEntity {

    private static final float RAD_REMOVE = -0.5F;

    private static final float NEUTRON_REMOVE = -0.005F;
    private static final float NEUTRON_DECAY = 0.9998074776F;

    private static final ContaminationEffect[] NO_CONTAMINATION = new ContaminationEffect[0];

    public BlockEntityDecon(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECON.get(), pos, state);
    }

    public void tickServer() {

        AABB box =
                new AABB(
                        worldPosition.getX() - 0.5D,
                        worldPosition.getY(),
                        worldPosition.getZ() - 0.5D,
                        worldPosition.getX() + 1.5D,
                        worldPosition.getY() + 2D,
                        worldPosition.getZ() + 1.5D);

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            HbmLivingProps.incrementRadiation(entity, RAD_REMOVE);
            entity.removeEffect(HbmPotion.radiation());
            HbmLivingProps.getData(entity).setCont(NO_CONTAMINATION);

            if (entity instanceof Player player) {
                ContaminationUtil.neutronActivateInventory(player, NEUTRON_REMOVE, NEUTRON_DECAY);
            }
        }
    }

    public void tickClient() {
        RandomSource rand = level.getRandom();
        level.addParticle(
                ParticleTypes.MYCELIUM,
                worldPosition.getX() + 0.125D + rand.nextDouble() * 0.75D,
                worldPosition.getY() + 1.1D,
                worldPosition.getZ() + 0.125D + rand.nextDouble() * 0.75D,
                0D,
                0.04D,
                0D);
    }
}
