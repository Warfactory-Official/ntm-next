/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class RailBoosterBlock extends RailStraightBlock {
    private final MapCodec<? extends RailBoosterBlock> codec;

    public RailBoosterBlock(BlockBehaviour.Properties properties, float maxSpeed) {
        super(properties, maxSpeed);
        this.codec = simpleCodec(props -> new RailBoosterBlock(props, maxSpeed));
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
