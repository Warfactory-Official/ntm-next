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
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityHeaterHeatex;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class HeaterHeatex extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {0, 0, 1, 1, 1, 1};

    public HeaterHeatex(Properties props) {
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

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing).relative(rot), MASK_SOUTH);
        visitor.cell(core.relative(facing).relative(rot, -1), MASK_SOUTH);
        visitor.cell(core.relative(facing, -1).relative(rot), MASK_NORTH);
        visitor.cell(core.relative(facing, -1).relative(rot, -1), MASK_NORTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        visitor.passiveCell(core.relative(facing).relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(facing).relative(rot, -1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(facing, -1).relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(
                core.relative(facing, -1).relative(rot, -1), MASK_ALL, PASSIVE_FLUID_IN);
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
        if (!player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (stack.getItem() instanceof FluidIdentifierItem
                && level.getBlockEntity(core) instanceof BlockEntityHeaterHeatex be) {
            FluidIdentifierData data =
                    stack.getOrDefault(
                            ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
            Fluid type = data.primary();
            if (type != null && type != Fluids.EMPTY) {
                be.tanks[0].setTankTypeByIdentifier(type);
                be.setChanged();
                player.sendSystemMessage(
                        Component.translatable(
                                "hbm.message.heatexSet", NTMFluidProperties.getDisplayName(type)));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu)
            openCoreMenu(player, core, menu);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityHeaterHeatex be)) return;
        info.title(getName().getString(), 0xFFFF00, 0x404000)
                .line(String.format(Locale.US, "%,d", be.heatEnergy) + " TU");
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityHeaterHeatex(pos, state);
    }

    @Override
    public MachineCaps caps() {

        return MachineCaps.blockKeyed()
                .fluidIn()
                .fluidOut()
                .fluidFaces(
                        BlockEntityHeaterHeatex.class,
                        (be, face) ->
                                face.side() == null
                                        || face.side().getAxis()
                                                == be.getBlockState().getValue(FACING).getAxis());
    }
}
