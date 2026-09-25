// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.bomb;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.inventory.container.MenuNukePrototype;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEntityNukePrototype extends BlockEntityNukeAssembly {

    public static final int SLOT_COUNT = 14;

    public BlockEntityNukePrototype(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_PROTOTYPE.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1, 12, 13 -> stack.is(ModItems.CELL_SAS3.get());
            case 2, 3, 10, 11 -> ModItems.ROD_QUAD.is(stack, BreedingRodType.URANIUM);
            case 4, 5, 8, 9 -> ModItems.ROD_QUAD.is(stack, BreedingRodType.LEAD);
            case 6, 7 -> ModItems.ROD_QUAD.is(stack, BreedingRodType.NP237);
            default -> false;
        };
    }

    public static boolean isReady(Container c) {
        return c.getItem(0).is(ModItems.CELL_SAS3.get())
                && c.getItem(1).is(ModItems.CELL_SAS3.get())
                && ModItems.ROD_QUAD.is(c.getItem(2), BreedingRodType.URANIUM)
                && ModItems.ROD_QUAD.is(c.getItem(3), BreedingRodType.URANIUM)
                && ModItems.ROD_QUAD.is(c.getItem(4), BreedingRodType.LEAD)
                && ModItems.ROD_QUAD.is(c.getItem(5), BreedingRodType.LEAD)
                && ModItems.ROD_QUAD.is(c.getItem(6), BreedingRodType.NP237)
                && ModItems.ROD_QUAD.is(c.getItem(7), BreedingRodType.NP237)
                && ModItems.ROD_QUAD.is(c.getItem(8), BreedingRodType.LEAD)
                && ModItems.ROD_QUAD.is(c.getItem(9), BreedingRodType.LEAD)
                && ModItems.ROD_QUAD.is(c.getItem(10), BreedingRodType.URANIUM)
                && ModItems.ROD_QUAD.is(c.getItem(11), BreedingRodType.URANIUM)
                && c.getItem(12).is(ModItems.CELL_SAS3.get())
                && c.getItem(13).is(ModItems.CELL_SAS3.get());
    }

    @Override
    public boolean isReady() {
        return isReady(this);
    }

    @Override
    public int yield() {
        return isReady() ? ExplosionData.PROTOTYPE_RADIUS.get() : -1;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.nukePrototype");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuNukePrototype(containerId, inventory, this);
    }
}
