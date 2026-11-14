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

import com.hbm.blocks.MinecartRail;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class RailGenericBlock extends RailBlock implements MinecartRail {
    private final MapCodec<RailBlock> codec;
    private final float maxSpeed;

    public RailGenericBlock(BlockBehaviour.Properties properties, float maxSpeed) {
        super(properties);
        this.maxSpeed = maxSpeed;
        this.codec = simpleCodec(props -> new RailGenericBlock(props, maxSpeed));
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
