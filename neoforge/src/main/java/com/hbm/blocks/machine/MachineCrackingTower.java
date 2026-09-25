// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.oil.BlockEntityMachineCrackingTower;
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

public class MachineCrackingTower extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] PLACEMENT_DIMENSIONS = {
        0, 0, 3, 3, 2, 3, 0, 0, 0,
        8, -1, 3, -1, 2, 0, 0, 0, 0,
        13, 0, 0, 3, 2, 1, 0, 0, 0,
        14, -13, -1, 2, 1, 0, 0, 0, 0,
        3, -1, 2, 3, -1, 3, 0, 0, 0,
    };
    private static final int[] DIMENSIONS = {0, 0, 3, 3, 2, 3};
    private static final int[] LEG_SPACE_A = {8, -1, 3, -1, 2, 0};
    private static final int[] LEG_SPACE_B = {13, 0, 0, 3, 2, 1};
    private static final int[] LEG_SPACE_C = {14, -13, -1, 2, 1, 0};
    private static final int[] LEG_SPACE_D = {3, -1, 2, 3, -1, 3};

    public MachineCrackingTower(Properties props) {
        super(props);
    }

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos core = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);

        return MultiblockHandlerXR.checkSpace(level, core, LEG_SPACE_A, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, core, LEG_SPACE_B, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, core, LEG_SPACE_C, placed, dir)
                && MultiblockHandlerXR.checkSpace(level, core, LEG_SPACE_D, placed, dir);
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        MultiblockHandlerXR.visitBox(core, LEG_SPACE_A, facing, visitor);
        MultiblockHandlerXR.visitBox(core, LEG_SPACE_B, facing, visitor);
        MultiblockHandlerXR.visitBox(core, LEG_SPACE_C, facing, visitor);
        MultiblockHandlerXR.visitBox(core, LEG_SPACE_D, facing, visitor);

        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(dx * 3 + rx, 0, dz * 3 + rz), MASK_SOUTH);
        visitor.cell(core.offset(dx * 3 - rx * 2, 0, dz * 3 - rz * 2), MASK_SOUTH);
        visitor.cell(core.offset(-dx * 3 + rx, 0, -dz * 3 + rz), MASK_NORTH);
        visitor.cell(core.offset(-dx * 3 - rx * 2, 0, -dz * 3 - rz * 2), MASK_NORTH);

        visitor.cell(core.offset(dx * 2 + rx * 2, 0, dz * 2 + rz * 2), MASK_WEST);
        visitor.cell(core.offset(dx * 2 - rx * 3, 0, dz * 2 - rz * 3), MASK_EAST);
        visitor.cell(core.offset(-dx * 2 + rx * 2, 0, -dz * 2 + rz * 2), MASK_WEST);
        visitor.cell(core.offset(-dx * 2 - rx * 3, 0, -dz * 2 - rz * 3), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.passiveCell(core.offset(dx * 3 + rx, 0, dz * 3 + rz), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.offset(dx * 3 - rx * 2, 0, dz * 3 - rz * 2), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-dx * 3 + rx, 0, -dz * 3 + rz), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.offset(-dx * 3 - rx * 2, 0, -dz * 3 - rz * 2), MASK_ALL, PASSIVE_FLUID_IN);

        visitor.passiveCell(
                core.offset(dx * 2 + rx * 2, 0, dz * 2 + rz * 2), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.offset(dx * 2 - rx * 3, 0, dz * 2 - rz * 3), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.offset(-dx * 2 + rx * 2, 0, -dz * 2 + rz * 2), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.offset(-dx * 2 - rx * 3, 0, -dz * 2 - rz * 3), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineCrackingTower(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        return InteractionResult.SUCCESS;
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
        if (!(stack.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown())
            return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineCrackingTower be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;

        be.tanks[0].setTankTypeByIdentifier(type);
        be.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                        "hbm.message.crackingTowerSet", NTMFluidProperties.getDisplayName(type)));
        return InteractionResult.CONSUME;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineCrackingTower be)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);

        for (int i = 0; i < BlockEntityMachineCrackingTower.TANK_COUNT; i++) {
            FluidTankNTM tank = be.tanks[i];
            info.line(
                    (i < 2 ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ")
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(tank.getFluid())
                            + ": "
                            + tank.getFill()
                            + "/"
                            + tank.getMaxFill()
                            + "mB");
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut();
    }
}
