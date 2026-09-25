// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.storage.BlockEntityBarrel;
import com.hbm.tileentity.machine.storage.BlockEntityMachineOrbus;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
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

public class MachineOrbus extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock, IPersistentInfoProvider {

    private static final int[] DIMENSIONS = {4, 0, 2, 1, 2, 1};

    public MachineOrbus(Properties props) {
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

        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        BlockPos top = core.above(4);

        visitor.cell(core.relative(back), MASK_DOWN);
        visitor.cell(core.relative(rot), MASK_DOWN);
        visitor.cell(core.relative(back).relative(rot), MASK_DOWN);

        visitor.cell(top, MASK_UP);
        visitor.cell(top.relative(back), MASK_UP);
        visitor.cell(top.relative(rot), MASK_UP);
        visitor.cell(top.relative(back).relative(rot), MASK_UP);
    }

    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise();
        BlockPos top = core.above(4);

        visitor.passiveCell(core.relative(back), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(back).relative(rot), MASK_ALL, PASSIVE_FLUID_IN);

        visitor.passiveCell(top, MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(top.relative(back), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(top.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(top.relative(back).relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
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
        if (!(held.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineOrbus be))
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

        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineOrbus(pos, state);
    }

    @Override
    public MachineCaps caps() {

        return MachineCaps.of(ModBlockEntities.ORBUS)
                .fluidIn()
                .fluidOut()
                .items()
                .fluidFaces(
                        BlockEntityBarrel.class,
                        (be, face) ->
                                face.fluid() == null || be.acceptsFluid(face.fluid(), face.side()));
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (content == null) return;
        adder.accept(IPersistentInfoProvider.tankLine(content, BlockEntityMachineOrbus.CAPACITY));
    }
}
