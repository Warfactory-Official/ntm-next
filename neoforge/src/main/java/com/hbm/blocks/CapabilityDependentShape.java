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

package com.hbm.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public interface CapabilityDependentShape {
    static BlockBehaviour.Properties deferShapeToFullChunk(BlockBehaviour.Properties props) {
        return props.postProcess((state, level, pos) -> pos);
    }

    default BlockState withPowerArm(BlockState state, Direction side, boolean joins) {
        var arm = PipeBlock.PROPERTY_BY_DIRECTION.get(side);
        return state.hasProperty(arm) ? state.setValue(arm, joins) : state;
    }

    interface Endpoint extends CapabilityDependentShape {}
}
