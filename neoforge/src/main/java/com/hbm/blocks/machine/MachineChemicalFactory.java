// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.tileentity.machine.BlockEntityMachineChemicalFactory;
import com.hbm.util.BobMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineChemicalFactory extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {2, 0, 2, 2, 2, 2};

    public MachineChemicalFactory(Properties props) {
        super(props);
    }

    private static int ringMask(int forward, int lateral) {
        int mask = MASK_NONE;
        if (forward == -2) mask |= MASK_NORTH;
        if (forward == 2) mask |= MASK_SOUTH;
        if (lateral == 2) mask |= MASK_WEST;
        if (lateral == -2) mask |= MASK_EAST;
        return mask;
    }

    private static int gradient(double frac) {
        frac = Math.clamp(frac, 0D, 1D);
        return ((int) (0xFF - 0xFF * frac) << 16) | ((int) (0xFF * frac) << 8);
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
    protected RenderShape getRenderShape(BlockState state) {

        return RenderShape.MODEL;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        MultiblockHandlerXR.visitBox(core, DIMENSIONS, facing, visitor);
        Direction rot = facing.getClockWise();

        for (int f = -2; f <= 2; f++) {
            for (int l = -2; l <= 2; l++) {
                if (Math.abs(f) != 2 && Math.abs(l) != 2) continue;
                visitor.cell(
                        core.offset(
                                facing.getStepX() * f + rot.getStepX() * l,
                                0,
                                facing.getStepZ() * f + rot.getStepZ() * l),
                        ringMask(f, l));
            }
        }

        for (int f = -2; f <= 2; f++) {
            visitor.cell(
                    core.offset(
                            facing.getStepX() * f + rot.getStepX() * 2,
                            2,
                            facing.getStepZ() * f + rot.getStepZ() * 2),
                    MASK_UP);
            visitor.cell(
                    core.offset(
                            facing.getStepX() * f - rot.getStepX() * 2,
                            2,
                            facing.getStepZ() * f - rot.getStepZ() * 2),
                    MASK_UP);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;

        for (int f = -2; f <= 2; f++) {
            for (int l = -2; l <= 2; l++) {
                if (Math.abs(f) != 2 && Math.abs(l) != 2) continue;
                visitor.passiveCell(
                        core.offset(
                                facing.getStepX() * f + rot.getStepX() * l,
                                0,
                                facing.getStepZ() * f + rot.getStepZ() * l),
                        MASK_ALL,
                        domains);
            }
        }
        for (int f = -2; f <= 2; f++) {
            visitor.passiveCell(
                    core.offset(
                            facing.getStepX() * f + rot.getStepX() * 2,
                            2,
                            facing.getStepZ() * f + rot.getStepZ() * 2),
                    MASK_ALL,
                    domains);
            visitor.passiveCell(
                    core.offset(
                            facing.getStepX() * f - rot.getStepX() * 2,
                            2,
                            facing.getStepZ() * f - rot.getStepZ() * 2),
                    MASK_ALL,
                    domains);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineChemicalFactory(pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineChemicalFactory be)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000);
        double frac = be.getMaxPower() <= 0 ? 0 : (double) be.getPower() / be.getMaxPower();
        info.line(
                BobMathUtil.getShortNumber(be.getPower())
                        + " / "
                        + BobMathUtil.getShortNumber(be.getMaxPower())
                        + " HE",
                gradient(frac));

        for (int i = 0; i < BlockEntityMachineChemicalFactory.MODULES; i++) {
            if (be.module[i].getRecipeName().isEmpty()) continue;
            GenericRecipe rec = be.module[i].getRecipe();
            String name = rec != null ? rec.getLocalizedName() : be.module[i].getRecipeName();
            info.line(
                    "Module "
                            + (i + 1)
                            + ": "
                            + name
                            + " ("
                            + (int) Math.round(be.module[i].progress * 100)
                            + "%)",
                    0xFFFF00);
        }

        info.line(
                "Water: " + be.water().getFill() + " / " + be.water().getMaxFill() + " mB",
                0x5555FF);
        info.line(
                "Spent Steam: " + be.lps().getFill() + " / " + be.lps().getMaxFill() + " mB",
                0xAAAAAA);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().powerIn().fluidIn().fluidOut().itemsAtCells().fe();
    }
}
