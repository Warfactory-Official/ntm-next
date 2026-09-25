// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityMachineHephaestus;
import com.hbm.util.I18nUtil;
import java.util.Locale;
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

public class MachineHephaestus extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {11, 0, 1, 1, 1, 1};

    public MachineHephaestus(Properties props) {
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

        for (int dy : new int[] {0, 11}) {
            visitor.cell(core.offset(1, dy, 0), MASK_EAST);
            visitor.cell(core.offset(-1, dy, 0), MASK_WEST);
            visitor.cell(core.offset(0, dy, 1), MASK_SOUTH);
            visitor.cell(core.offset(0, dy, -1), MASK_NORTH);
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (int dy : new int[] {0, 11}) {
            visitor.passiveCell(core.offset(1, dy, 0), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(-1, dy, 0), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(0, dy, 1), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.offset(0, dy, -1), MASK_ALL, PASSIVE_FLUID_IN);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineHephaestus(pos, state);
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
        if (!(stack.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineHephaestus be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;

        be.input.setTankTypeByIdentifier(type);
        be.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                        "hbm.message.hephaestusSet", NTMFluidProperties.getDisplayName(type)));
        return InteractionResult.CONSUME;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineHephaestus be)) return;
        info.title(I18nUtil.resolveKey(getDescriptionId()), 0xFFFF00, 0x404000);
        info.line(String.format(Locale.US, "%,d", be.bufferedHeat) + " TU");

        FluidTankNTM[] tanks = {be.input, be.output};
        for (int i = 0; i < tanks.length; i++) {
            FluidTankNTM tank = tanks[i];
            String arrow = i == 0 ? ChatFormatting.GREEN + "-> " : ChatFormatting.RED + "<- ";
            info.line(
                    arrow
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

        return MachineCaps.blockKeyed()
                .fluidIn()
                .fluidOut()
                .fluidFaces(
                        BlockEntityMachineHephaestus.class,
                        (be, face) ->
                                face.side() == null || face.side().getAxis() != Direction.Axis.Y);
    }
}
