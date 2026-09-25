// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.client.ToolConversionLookOverlay;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
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

public class BlockToolConversion extends Block implements IToolable, ILookOverlay {

    private final IToolable.ToolType tool;
    private final Supplier<Block> result;
    private final List<CountIngredient> cost;

    public BlockToolConversion(
            Properties props,
            IToolable.ToolType tool,
            Supplier<Block> result,
            CountIngredient... cost) {
        super(props);
        this.tool = tool;
        this.result = result;
        this.cost = List.of(cost);
    }

    public IToolable.ToolType tool() {
        return tool;
    }

    public Block result() {
        return result.get();
    }

    public List<CountIngredient> cost() {
        return cost;
    }

    @Override
    public boolean onScrew(
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
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        ToolConversionLookOverlay.build(level, getName().getString(), tool, cost, info);
    }
}
