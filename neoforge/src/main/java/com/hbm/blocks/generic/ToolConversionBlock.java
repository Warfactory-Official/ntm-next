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

import com.hbm.client.ToolConversionLookOverlay;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.util.InventoryUtil;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ToolConversionBlock extends Block
        implements ToolInteractionHandler, LookOverlayProvider {

    private final ToolType tool;
    private final Supplier<Block> result;
    private final List<CountIngredient> cost;

    public ToolConversionBlock(
            Properties props, ToolType tool, Supplier<Block> result, CountIngredient... cost) {
        super(props);
        this.tool = tool;
        this.result = result;
        this.cost = List.of(cost);
    }

    public ToolType tool() {
        return tool;
    }

    public Block result() {
        return result.get();
    }

    public List<CountIngredient> cost() {
        return cost;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != this.tool) return false;
        if (level.isClientSide()) return false;
        if (!cost.isEmpty() && !InventoryUtil.consumeIngredients(player, cost)) return false;
        level.setBlockAndUpdate(pos, result.get().defaultBlockState());
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        ToolConversionLookOverlay.build(level, getName(), tool, cost, info);
    }
}
