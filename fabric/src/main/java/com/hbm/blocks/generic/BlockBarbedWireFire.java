// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class BlockBarbedWireFire extends BlockBarbedWire {

    public static final MapCodec<BlockBarbedWireFire> CODEC = simpleCodec(BlockBarbedWireFire::new);

    private static final int BURN_SECONDS = 1;

    public BlockBarbedWireFire(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void sting(Level level, Entity entity) {
        super.sting(level, entity);
        entity.igniteForSeconds(BURN_SECONDS);
    }
}
