// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.tileentity.machine.BlockEntityHeaterOilburner;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class HeaterOilburner extends BlockMultiblockCore
        implements ITickingBlock, IToolable, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {1, 0, 1, 1, 1, 1};

    public HeaterOilburner(Properties props) {
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
        visitor.cell(core.offset(0, 1, 0), MASK_NONE);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public boolean heatSourceAt(int lx, int ly, int lz) {
        return lx == 0 && ly == 1 && lz == 0;
    }

    @Override
    protected boolean proxyCellAt(int lx, int ly, int lz) {
        return heatSourceAt(lx, ly, lz);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityHeaterOilburner(pos, state);
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
        if (!(level.getBlockEntity(pos) instanceof BlockEntityHeaterOilburner burner)) return false;

        burner.toggleSetting();
        burner.setChanged();
        burner.networkPackNT(25);
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityHeaterOilburner burner)) return;

        info.title(getName().getString(), 0xFFFF00, 0x404000)
                .line(
                        ChatFormatting.GREEN
                                + "-> "
                                + ChatFormatting.RESET
                                + burner.setting
                                + " mB/t");

        Fluid type = burner.tank.getTankType();
        FT_Flammable trait = NTMFluidProperties.getTrait(type, FT_Flammable.class);
        if (trait != null) {
            int heat = (int) (trait.getHeatEnergy() * burner.setting / 1000L);
            info.line(
                    ChatFormatting.RED
                            + "<- "
                            + ChatFormatting.RESET
                            + String.format(Locale.US, "%,d", heat)
                            + " TU/t");
        }
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut();
    }
}
