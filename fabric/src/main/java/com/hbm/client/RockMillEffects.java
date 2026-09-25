// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.BlockEntityMachineRockMill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class RockMillEffects {
    private RockMillEffects() {}

    public static void tick(BlockEntityMachineRockMill mill) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || client.player.distanceToSqr(
                                mill.getBlockPos().getX() + 0.5,
                                mill.getBlockPos().getY() + 1.5,
                                mill.getBlockPos().getZ() + 0.5)
                        >= 35 * 35) return;

        BlockState dust = Blocks.GRAVEL.defaultBlockState();
        GenericRecipe recipe = mill.module.getRecipe();
        if (recipe != null && recipe.getIcon().getItem() instanceof BlockItem block) {
            dust = block.getBlock().defaultBlockState();
        }

        double angle = mill.getLevel().getRandom().nextDouble() * Math.PI * 2;
        double x = Math.cos(angle);
        double z = -Math.sin(angle);
        double vx = x * 0.125;
        double vz = z * 0.125;
        Particle particle =
                client.particleEngine.createParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, dust),
                        mill.getBlockPos().getX() + 0.5 + x * 2.25,
                        mill.getBlockPos().getY() + 1.5,
                        mill.getBlockPos().getZ() + 0.5 + z * 2.25,
                        vx,
                        0.1,
                        vz);
        if (particle != null) {
            particle.setParticleSpeed(vx, 0.1, vz);
            particle.setLifetime(10 + mill.getLevel().getRandom().nextInt(20));
            if (particle instanceof SingleQuadParticle quad) quad.setColor(0.8F, 0.8F, 0.8F);
        }
    }
}
