// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.storage.BlockEntityBarrel;
import com.hbm.tileentity.machine.storage.BlockEntityMachineBigAssTank;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

public class MachineBigAssTank extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock, IPersistentInfoProvider {

    private static final int[] PLACEMENT_DIMENSIONS = {
        5, 0, 4, 4, 4, 4, 0, 0, 0,
        4, 0, 5, -4, 2, 2, 0, 0, 0,
        4, 0, -4, 5, 2, 2, 0, 0, 0,
        4, 0, 2, 2, 5, -4, 0, 0, 0,
        4, 0, 2, 2, -4, 5, 0, 0, 0,
        3, 0, 6, -5, 0, 0, 0, 0, 0,
        3, 0, -5, 6, 0, 0, 0, 0, 0,
    };

    @Override
    protected int[] placementDimensionBoxes() {
        return PLACEMENT_DIMENSIONS;
    }

    private static final int[] DIMENSIONS = {5, 0, 4, 4, 4, 4};
    private static final int[][] BOXES = {
        {4, 0, 5, -4, 2, 2},
        {4, 0, -4, 5, 2, 2},
        {4, 0, 2, 2, 5, -4},
        {4, 0, 2, 2, -4, 5},
        {3, 0, 6, -5, 0, 0},
        {3, 0, -5, 6, 0, 0}
    };

    public MachineBigAssTank(Properties props) {
        super(props);
    }

    @Override
    protected boolean tilts() {
        return true;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 6;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placed, Direction dir, int o) {
        if (!super.checkRequirement(level, placed, dir, o)) return false;
        BlockPos core = placed.offset(dir.getStepX() * o, dir.getStepY() * o, dir.getStepZ() * o);
        for (int[] box : BOXES) {
            if (!MultiblockHandlerXR.checkSpace(level, core, box, placed, dir)) return false;
        }
        return true;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] box : BOXES) MultiblockHandlerXR.visitBox(core, box, facing, visitor);

        Direction back = facing.getOpposite();
        visitor.cell(core.relative(facing, 6), MultiblockSurface.maskBit(facing));
        visitor.cell(core.relative(back, 6), MultiblockSurface.maskBit(back));
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.relative(facing, 6), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(facing.getOpposite(), 6), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (MultiblockSurface.foldedCore(state) != null) {
            BlockEntityMachineBigAssTank.removeVirtualNodes(level, pos, state.getValue(FACING));
        }
        super.removedAt(state, level, pos, movedByPiston);
    }

    @Override
    protected InteractionResult useItemOnAtCore(
            ItemStack held,
            BlockState coreState,
            Level level,
            BlockPos core,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(held.getItem() instanceof FluidIdentifierItem)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineBigAssTank be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                held.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;

        be.tank.setTankTypeByIdentifier(type);
        be.setChanged();
        player.sendSystemMessage(
                Component.translatable("desc.shared.changedTypeTo")
                        .append(NTMFluidProperties.getDisplayName(type))
                        .append("!")
                        .withStyle(ChatFormatting.YELLOW));
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineBigAssTank(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.BIGASSTANK)
                .fluidIn()
                .fluidOut()
                .items()
                .fluidFaces(
                        BlockEntityBarrel.class,
                        (be, face) ->
                                face.fluid() == null || be.acceptsFluid(face.fluid(), face.side()));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected boolean cellsHaveAnalogOutput() {
        return true;
    }

    @Override
    protected boolean comparatorCellAt(int lx, int ly, int lz) {
        return ly == 0 && lx == 0 && Math.abs(lz) == 6;
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (content == null) return;
        adder.accept(
                IPersistentInfoProvider.tankLine(content, BlockEntityMachineBigAssTank.CAPACITY));
    }
}
