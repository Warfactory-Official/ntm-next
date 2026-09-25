// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.interfaces;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface IToolable {

    boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool);

    interface Tool {
        ToolType toolType();
    }

    enum ToolType {
        SCREWDRIVER,
        HAND_DRILL,
        DEFUSER,
        WRENCH,
        TORCH,
        BOLT;

        private @Nullable List<Item> items;

        public static @Nullable ToolType getType(ItemStack stack) {
            return stack.getItem() instanceof Tool tool ? tool.toolType() : null;
        }

        public List<Item> items() {
            List<Item> resolved = items;
            if (resolved == null) {
                resolved =
                        BuiltInRegistries.ITEM.stream()
                                .filter(
                                        item ->
                                                item instanceof Tool tool
                                                        && tool.toolType() == this)
                                .toList();
                items = resolved;
            }
            return resolved;
        }
    }
}
