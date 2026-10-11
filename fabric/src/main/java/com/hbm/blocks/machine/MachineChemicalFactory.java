// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.BlockEntityMachineChemicalFactory;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.util.BobMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class MachineChemicalFactory extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {
    public MachineChemicalFactory(Properties props) {
        super(props);
    }

    private static int gradient(double frac) {
        frac = Math.clamp(frac, 0D, 1D);
        return ((int) (0xFF - 0xFF * frac) << 16) | ((int) (0xFF * frac) << 8);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
            String name =
                    rec != null ? rec.getLocalizedName().getString() : be.module[i].getRecipeName();
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
