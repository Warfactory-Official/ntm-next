// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.FluidContainerRows;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Unsiphonable;
import com.hbm.items.tool.ItemPipette;
import com.hbm.platform.IFluidHandlerView;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class ItemFluidSiphon extends Item {

    private static final int PIPETTE_REMAINDER = 1000;

    public ItemFluidSiphon(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos target = MultiblockSurface.coreOfAny(level, ctx.getClickedPos());
        if (target == null) target = ctx.getClickedPos();

        BlockEntity be = level.getBlockEntity(target);
        Player player = ctx.getPlayer();
        if (!(be instanceof FluidTankEndpoint endpoint) || player == null)
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        for (FluidTankNTM tank : endpoint.getReceivingTanks()) {
            Fluid type = tank.getTankType();
            if (tank.getFill() <= 0 || type == null) continue;
            if (NTMFluidProperties.hasTrait(type, FT_Unsiphonable.class)) continue;

            if (drain(player, tank, tank.getFluid())) {
                be.setChanged();
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private boolean drain(Player player, FluidTankNTM tank, Fluid type) {
        Inventory inventory = player.getInventory();
        ItemPipette pipette = null;
        ItemStack pipetteStack = ItemStack.EMPTY;
        boolean drained = false;

        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;

            if (pipette == null
                    && stack.getItem() instanceof ItemPipette held
                    && !held.fizzlesOn(type)
                    && held.variant() != ItemPipette.Variant.LABORATORY) {
                pipette = held;
                pipetteStack = stack;
            }

            ItemStack filled = fillOne(stack, tank, type);
            while (filled != null) {
                drained = true;
                stack.shrink(1);
                player.getInventory().placeItemBackInInventory(filled);
                if (stack.isEmpty()) break;
                filled = fillOne(stack, tank, type);
            }
        }

        if (pipette != null && tank.getFill() < PIPETTE_REMAINDER) {
            int taken = pipette.fill(pipetteStack, type, tank.getFill(), tank.getPressure());
            if (taken > 0) {
                tank.setFill(tank.getFill() - taken);
                drained = true;
            }
        }
        return drained;
    }

    private @Nullable ItemStack fillOne(ItemStack stack, FluidTankNTM tank, Fluid type) {
        if (stack.getItem() instanceof ItemPipette) return null;
        if (FluidContainerRows.answers(stack)) {
            int amount = FluidContainerRows.fillAmount(stack, type);
            if (amount <= 0 || amount > tank.getFill()) return null;
            tank.setFill(tank.getFill() - amount);
            return FluidContainerRows.filled(stack, type);
        }
        SimpleContainer scratch = new SimpleContainer(stack.copyWithCount(1));
        IFluidHandlerView view = Services.CAPS.findFluidHandler(scratch, 0);
        if (view == null) return null;

        long room = view.insertable(type);
        if (room <= 0 || room > tank.getFill()) return null;
        long given = view.insert(type, room);
        if (given <= 0) return null;

        tank.setFill(tank.getFill() - given);
        return scratch.getItem(0);
    }
}
