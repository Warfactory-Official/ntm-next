// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.tileentity.machine.BlockEntitySteamEngine;
import com.hbm.util.I18nUtil;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineSteamEngine extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 5, 1, 1, 1};

    public MachineSteamEngine(Properties props) {
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
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        for (int i = -1; i <= 1; i++) {
            visitor.cell(
                    core.offset(
                            rot.getStepX() + facing.getStepX() * i,
                            1,
                            rot.getStepZ() + facing.getStepZ() * i),
                    MASK_WEST);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        for (int i = -1; i <= 1; i++) {
            visitor.passiveCell(
                    core.offset(
                            rot.getStepX() + facing.getStepX() * i,
                            1,
                            rot.getStepZ() + facing.getStepZ() * i),
                    MASK_ALL,
                    PASSIVE_POWER_IN | PASSIVE_FLUID_IN);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySteamEngine(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, ILookOverlay.LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntitySteamEngine engine)) return;

        info.line(
                ChatFormatting.GREEN
                        + "-> "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(engine.tanks[0].getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", engine.tanks[0].getFill())
                        + " / "
                        + String.format(Locale.US, "%,d", engine.tanks[0].getMaxFill())
                        + "mB");
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(engine.tanks[1].getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", engine.tanks[1].getFill())
                        + " / "
                        + String.format(Locale.US, "%,d", engine.tanks[1].getMaxFill())
                        + "mB");
        info.title(I18nUtil.resolveKey(getDescriptionId()), 0xffff00, 0x404000);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed()
                .powerOut()
                .fluidIn()
                .fluidOut()
                .fe()
                .faces(BlockEntitySteamEngine.class, (be, side) -> side.getAxis().isHorizontal());
    }
}
