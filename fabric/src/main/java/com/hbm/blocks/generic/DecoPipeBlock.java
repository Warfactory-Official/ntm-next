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

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DecoPipeBlock extends RotatedPillarBlock {
    private final Kind kind;

    public DecoPipeBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        return direction.getAxis() == state.getValue(AXIS)
                && neighbor.getBlock() instanceof DecoPipeBlock pipe
                && pipe.kind == kind
                && neighbor.getValue(AXIS) == direction.getAxis();
    }

    public enum Kind {
        PLAIN("pipe", false),
        RIM("pipe_rim", false),
        QUAD("pipe_quad", false),
        FRAMED("pipe_rim", true);

        public final Identifier obj;
        public final boolean framed;

        Kind(String model, boolean framed) {
            obj = Identifier.fromNamespaceAndPath("hbm", "models/blocks/" + model + ".obj");
            this.framed = framed;
        }

        public DecoPipeBlock create(Properties properties) {
            return new DecoPipeBlock(this, properties);
        }
    }
}
