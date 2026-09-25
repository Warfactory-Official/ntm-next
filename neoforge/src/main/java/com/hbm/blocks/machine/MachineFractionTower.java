// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.oil.BlockEntityMachineFractionTower;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineFractionTower extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 1, 1, 1, 1};

    public MachineFractionTower(Properties props) {
        super(props);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack stack,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (player.isShiftKeyDown() || !(stack.getItem() instanceof FluidIdentifierItem))
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineFractionTower frac)) {
            return InteractionResult.PASS;
        }

        if (level.getBlockEntity(core.below(3)) instanceof BlockEntityMachineFractionTower) {
            player.sendSystemMessage(Component.translatable("chat.fractioning.onlybottom"));
            return InteractionResult.SUCCESS;
        }

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;

        frac.tanks[0].setTankTypeByIdentifier(type);
        frac.setChanged();
        player.sendSystemMessage(typeChanged(type));
        return InteractionResult.SUCCESS;
    }

    public static Component typeChanged(Fluid type) {
        return Component.translatable("desc.shared.changedTypeTo")
                .withStyle(ChatFormatting.YELLOW)
                .append(NTMFluidProperties.getDisplayName(type))
                .append("!");
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineFractionTower frac)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        for (int i = 0; i < frac.tanks.length; i++) {
            FluidTankNTM tank = frac.tanks[i];
            if (tank.getTankType() == null) continue;
            String arrow = i == 0 ? "-> " : "<- ";
            int color = i == 0 ? 0x55FF55 : 0xFF5555;
            info.line(
                    arrow
                            + NTMFluidProperties.clientName(tank.getFluid())
                            + ": "
                            + tank.getFill()
                            + " / "
                            + tank.getMaxFill()
                            + " mB",
                    color);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineFractionTower(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FRACTION_TOWER).fluidIn().fluidOut().items();
    }
}
