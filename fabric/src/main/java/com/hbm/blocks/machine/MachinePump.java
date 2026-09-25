// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.data.MachineData;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.tileentity.machine.BlockEntityMachinePumpBase;
import com.hbm.tileentity.machine.BlockEntityMachinePumpElectric;
import com.hbm.tileentity.machine.BlockEntityMachinePumpSteam;
import com.hbm.util.BobMathUtil;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachinePump extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {3, 0, 1, 1, 1, 1};

    public MachinePump(Properties props) {
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
        visitor.cell(core.east(), MASK_EAST);
        visitor.cell(core.west(), MASK_WEST);
        visitor.cell(core.south(), MASK_SOUTH);
        visitor.cell(core.north(), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains =
                this == ModBlocks.PUMP_ELECTRIC.get()
                        ? PASSIVE_POWER_IN | PASSIVE_FLUID_IN
                        : PASSIVE_FLUID_IN;

        visitor.passiveCell(core.east(), MASK_ALL, domains);
        visitor.passiveCell(core.west(), MASK_ALL, domains);
        visitor.passiveCell(core.south(), MASK_ALL, domains);
        visitor.passiveCell(core.north(), MASK_ALL, domains);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        FoldedOwner owner = ownerOf(level, pos);
        if (owner == null) return;
        if (!(level.getBlockEntity(owner.pos()) instanceof BlockEntityMachinePumpBase pump)) return;

        info.title(getName().getString(), 0xFFFF00, 0x404000);

        if (pump instanceof BlockEntityMachinePumpSteam steamPump) {
            info.line(
                    ChatFormatting.GREEN
                            + "-> "
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(steamPump.steam.getFluid())
                            + ": "
                            + String.format(Locale.US, "%,d", steamPump.steam.getFill())
                            + " / "
                            + String.format(Locale.US, "%,d", steamPump.steam.getMaxFill())
                            + "mB");
            info.line(
                    ChatFormatting.RED
                            + "<- "
                            + ChatFormatting.RESET
                            + NTMFluidProperties.clientName(steamPump.lps.getFluid())
                            + ": "
                            + String.format(Locale.US, "%,d", steamPump.lps.getFill())
                            + " / "
                            + String.format(Locale.US, "%,d", steamPump.lps.getMaxFill())
                            + "mB");
        } else if (pump instanceof BlockEntityMachinePumpElectric electricPump) {
            info.line(
                    ChatFormatting.GREEN
                            + "-> "
                            + ChatFormatting.RESET
                            + String.format(Locale.US, "%,d", electricPump.getPower())
                            + " / "
                            + String.format(Locale.US, "%,d", electricPump.getMaxPower())
                            + "HE");
        }

        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(pump.water.getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", pump.water.getFill())
                        + " / "
                        + String.format(Locale.US, "%,d", pump.water.getMaxFill())
                        + "mB");

        if (owner.pos().getY() > MachineData.PUMP_GROUND_HEIGHT.get()) {
            info.line("! ! ! ALTITUDE ! ! !", BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
        }
        if (!pump.onGround) {
            info.line("! ! ! NO VALID GROUND ! ! !", BobMathUtil.getBlink() ? 0xFF0000 : 0xFFFF00);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this == ModBlocks.PUMP_ELECTRIC.get()
                ? new BlockEntityMachinePumpElectric(pos, state)
                : new BlockEntityMachinePumpSteam(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return this == ModBlocks.PUMP_ELECTRIC.get()
                ? MachineCaps.blockKeyed().powerIn().fluidOut().fe()
                : MachineCaps.blockKeyed().fluidIn().fluidOut();
    }
}
