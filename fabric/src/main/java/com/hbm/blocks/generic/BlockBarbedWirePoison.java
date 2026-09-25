// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class BlockBarbedWirePoison extends BlockBarbedWire {

    public static final MapCodec<BlockBarbedWirePoison> CODEC =
            simpleCodec(BlockBarbedWirePoison::new);

    public BlockBarbedWirePoison(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void sting(Level level, Entity entity) {
        super.sting(level, entity);
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, EFFECT_TICKS, 2));
        }
    }
}
