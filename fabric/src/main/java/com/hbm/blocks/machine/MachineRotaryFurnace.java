// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.tileentity.machine.BlockEntityMachineRotaryFurnace;
import com.hbm.util.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

public class MachineRotaryFurnace extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {4, 0, 1, 1, 2, 2};

    public MachineRotaryFurnace(Properties props) {
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
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);

        Direction rot = facing.getCounterClockWise();
        BlockPos back = core.relative(facing.getOpposite());

        visitor.cell(back.relative(rot, -2), MASK_NORTH, ROLE_FLUID);
        visitor.cell(back.relative(rot, -1), MASK_NORTH, ROLE_FLUID);

        visitor.cell(core.relative(facing).relative(rot, 2), MASK_EAST, ROLE_FLUID_IN);
        visitor.cell(
                core.relative(facing.getOpposite()).relative(rot, 2), MASK_EAST, ROLE_FLUID_IN);

        visitor.cell(core.relative(rot).above(4), MASK_UP, ROLE_FLUID_OUT);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getCounterClockWise();
        BlockPos back = core.relative(facing.getOpposite());

        for (int i = -2; i <= 2; i++) {
            visitor.passiveCell(back.relative(rot, i), MASK_ALL, PASSIVE_FLUID_IN);
        }
        visitor.passiveCell(core.relative(facing).relative(rot, 2), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(rot).above(4), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(facing).relative(rot), MASK_ALL, PASSIVE_ITEMS);

        passiveEverywhere(core, facing, visitor, PASSIVE_ITEMS);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockPos hit = info.getHitPos();
        if (hit == null) return;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineRotaryFurnace furnace)) return;

        Direction dir = furnace.coreFacing();
        Direction rot = dir.getCounterClockWise();
        BlockPos front = pos.relative(dir);
        BlockPos back = pos.relative(dir.getOpposite());

        if (hit.equals(back.relative(rot, -1)) || hit.equals(back.relative(rot, -2))) {
            info.line(
                    ChatFormatting.GREEN
                            + "-> "
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(furnace.tanks[1].getTankType()));
            info.line(
                    ChatFormatting.RED
                            + "<- "
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(furnace.tanks[2].getTankType()));
        }

        if (hit.equals(front.relative(rot, 2)) || hit.equals(back.relative(rot, 2))) {

            Fluid recipe = furnace.tanks[0].getTankType();
            info.line(
                    ChatFormatting.GREEN
                            + "-> "
                            + ChatFormatting.RESET
                            + (recipe == null ? "None" : NTMFluidProperties.clientName(recipe)));
        }

        if (hit.equals(front.relative(rot))) {
            info.line(ChatFormatting.YELLOW + "-> " + ChatFormatting.RESET + "Fuel");
        }

        if (!info.isEmpty()) {
            info.title(I18nUtil.resolveKey(getDescriptionId()), 0xffff00, 0x404000);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineRotaryFurnace(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ROTARY_FURNACE)
                .fluidIn()
                .fluidOut()
                .items()
                .itemsAtCells();
    }
}
