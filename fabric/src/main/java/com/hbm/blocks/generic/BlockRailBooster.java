// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockRailBooster extends BlockRailStraight {

    private final MapCodec<? extends BlockRailBooster> codec;

    public BlockRailBooster(BlockBehaviour.Properties properties, float maxSpeed) {
        super(properties, maxSpeed);
        this.codec = simpleCodec(props -> new BlockRailBooster(props, maxSpeed));
    }

    @Override
    public void onMinecartPass(AbstractMinecart cart) {
        cart.setDeltaMovement(cart.getDeltaMovement().scale(1.15F));
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return codec;
    }
}
