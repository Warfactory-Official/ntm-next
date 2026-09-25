// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.machine.BlockEntityHeaterElectric;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityHeaterElectric.class, calling = "refreshHeatBelow")
public class HeaterElectric extends BlockMultiblockCore
        implements ITickingBlock, IToolable, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {0, 0, 1, 2, 1, 1};

    public HeaterElectric(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);
        visitor.cell(core.relative(facing, 2), MASK_SOUTH);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.relative(facing, 2), MASK_ALL, PASSIVE_POWER_IN);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide()) return true;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityHeaterElectric heater)) return false;

        heater.toggleSetting();
        heater.setChanged();
        heater.networkPackNT(25);
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityHeaterElectric heater)) return;

        info.title(getName().getString(), 0xFFFF00, 0x404000)
                .line(String.format(Locale.US, "%,d TU", heater.heatEnergy))
                .line(
                        ChatFormatting.GREEN
                                + "-> "
                                + ChatFormatting.RESET
                                + heater.getConsumption()
                                + " HE/t")
                .line(
                        ChatFormatting.RED
                                + "<- "
                                + ChatFormatting.RESET
                                + heater.getHeatGen()
                                + " TU/t");
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityHeaterElectric(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fe();
    }
}
