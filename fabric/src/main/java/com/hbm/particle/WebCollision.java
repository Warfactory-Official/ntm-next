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

package com.hbm.particle;

import com.hbm.blocks.fluid.AcidBlock;
import com.hbm.blocks.fluid.FiniteCoriumBlock;
import com.hbm.blocks.fluid.MudBlock;
import com.hbm.blocks.fluid.SchrabidicBlock;
import com.hbm.blocks.fluid.ToxicBlock;
import com.hbm.blocks.generic.BarbedWireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;

final class WebCollision {

    private WebCollision() {}

    static boolean isWeb(BlockState state) {
        Block block = state.getBlock();
        return block instanceof WebBlock
                || block instanceof BarbedWireBlock
                || block instanceof AcidBlock
                || block instanceof ToxicBlock
                || block instanceof SchrabidicBlock
                || block instanceof MudBlock
                || block instanceof FiniteCoriumBlock;
    }
}
