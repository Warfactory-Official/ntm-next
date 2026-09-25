// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.RefreshesNeighborState;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.BlockEntityHeatBoiler;
import com.hbm.util.I18nUtil;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

@RefreshesNeighborState(be = BlockEntityHeatBoiler.class, calling = "refreshHeatBelow")
public class MachineHeatBoiler extends BlockMultiblockCore
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    private static final int[] DIMENSIONS = {3, 0, 1, 1, 1, 1};

    public MachineHeatBoiler(Properties props) {
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

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(rot), MASK_WEST);
        visitor.cell(core.relative(rot.getOpposite()), MASK_EAST);
        visitor.cell(core.above(3), MASK_UP);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        visitor.passiveCell(core.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.relative(rot.getOpposite()), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.above(3), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityHeatBoiler(pos, state);
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
        if (!(level.getBlockEntity(core) instanceof BlockEntityHeatBoiler be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;
        FT_Heatable trait = NTMFluidProperties.getTrait(type, FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.BOILER) <= 0)
            return InteractionResult.PASS;

        be.tanks[0].setTankTypeByIdentifier(type);
        be.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                        "hbm.message.boilerSet", NTMFluidProperties.getDisplayName(type)));
        return InteractionResult.CONSUME;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                        instanceof BlockEntityHeatBoiler be
                && be.hasExploded) {
            return List.of(
                    new ItemStack(ModItems.ingot(Mats.MAT_STEEL), 4),
                    new ItemStack(ModItems.plate(Mats.MAT_COPPER), 8));
        }
        return super.getDrops(state, params);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        var owner = BlockMultiblockCore.ownerOf(level, pos);
        BlockPos core = owner != null ? owner.pos() : null;
        if (core == null) return;
        if (!(level.getBlockEntity(core) instanceof BlockEntityHeatBoiler be)) return;
        if (be.hasExploded) return;
        info.title(I18nUtil.resolveKey(getDescriptionId()), 0xFFFF00, 0x404000);
        info.line(String.format(Locale.US, "%,d", be.heat) + "TU");
        info.line(
                ChatFormatting.GREEN
                        + "-> "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(be.tanks[0].getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", be.tanks[0].getFill())
                        + " / "
                        + String.format(Locale.US, "%,d", be.tanks[0].getMaxFill())
                        + "mB");
        info.line(
                ChatFormatting.RED
                        + "<- "
                        + ChatFormatting.RESET
                        + NTMFluidProperties.clientName(be.tanks[1].getFluid())
                        + ": "
                        + String.format(Locale.US, "%,d", be.tanks[1].getFill())
                        + " / "
                        + String.format(Locale.US, "%,d", be.tanks[1].getMaxFill())
                        + "mB");
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.blockKeyed().fluidIn().fluidOut();
    }
}
