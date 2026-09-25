// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.lib.ModDamageTypes;
import com.hbm.potion.HbmPotion;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class BlockBarbedWireUltradeath extends BlockBarbedWire {

    public static final MapCodec<BlockBarbedWireUltradeath> CODEC =
            simpleCodec(BlockBarbedWireUltradeath::new);

    private static final float DAMAGE = 5.0F;

    public BlockBarbedWireUltradeath(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void sting(Level level, Entity entity) {
        if (level instanceof ServerLevel server) {
            entity.hurtServer(
                    server, server.damageSources().source(ModDamageTypes.PINK_CLOUD), DAMAGE);
        }
        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotion.radiation(), EFFECT_TICKS, 9));
        }
    }
}
