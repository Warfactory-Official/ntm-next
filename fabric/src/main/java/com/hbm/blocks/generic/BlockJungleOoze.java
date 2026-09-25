// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.potion.HbmPotion;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockJungleOoze extends Block {

    public static final MapCodec<BlockJungleOoze> CODEC = simpleCodec(BlockJungleOoze::new);

    public BlockJungleOoze(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<BlockJungleOoze> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), 15 * 20, 9));
        }
        super.stepOn(level, pos, state, entity);
    }
}
