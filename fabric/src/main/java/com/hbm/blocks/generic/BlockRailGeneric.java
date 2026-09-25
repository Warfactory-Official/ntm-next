// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.IMinecartRail;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class BlockRailGeneric extends RailBlock implements IMinecartRail {

    private final MapCodec<RailBlock> codec;
    private final float maxSpeed;

    public BlockRailGeneric(BlockBehaviour.Properties properties, float maxSpeed) {
        super(properties);
        this.maxSpeed = maxSpeed;
        this.codec = simpleCodec(props -> new BlockRailGeneric(props, maxSpeed));
    }

    @Override
    public float railMaxSpeed() {
        return maxSpeed;
    }

    @Override
    public MapCodec<RailBlock> codec() {
        return codec;
    }
}
