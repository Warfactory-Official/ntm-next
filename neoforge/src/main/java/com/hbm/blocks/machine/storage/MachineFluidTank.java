// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.client.RepairLookOverlay;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IRepairable;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.machine.storage.BlockEntityMachineFluidTank;
import com.hbm.tileentity.machine.storage.FluidTankContents;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MachineFluidTank extends BlockMultiblockCore
        implements ITickingBlock,
                ILookOverlay,
                ICapabilityBlock,
                IPersistentInfoProvider,
                IToolable {

    public static final BooleanProperty DAMAGED = BooleanProperty.create("damaged");
    private static final int[] DIMENSIONS = {2, 0, 1, 1, 2, 2};

    public MachineFluidTank(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(DAMAGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DAMAGED);
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
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        Direction rot = facing.getClockWise();
        BlockPos front = core.relative(facing, 1);
        BlockPos back = core.relative(facing, -1);

        visitor.cell(front.relative(rot), MASK_SOUTH);
        visitor.cell(front.relative(rot, -1), MASK_SOUTH);
        visitor.cell(back.relative(rot), MASK_NORTH);
        visitor.cell(back.relative(rot, -1), MASK_NORTH);
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        BlockPos front = core.relative(facing, 1);
        BlockPos back = core.relative(facing, -1);

        visitor.passiveCell(front.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(front.relative(rot, -1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(back.relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(back.relative(rot, -1), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public AABB climbBox(Direction facing) {
        Direction rot = facing.getClockWise();
        return new AABB(0, 0, 0, 1, 2.875, 1)
                .move(
                        facing.getStepX() * 0.5 - rot.getStepX() * 2.25,
                        0,
                        facing.getStepZ() * 0.5 - rot.getStepZ() * 2.25);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        RepairLookOverlay.build(level, pos, info);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos core,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        return tool == ToolType.TORCH && IRepairable.tryRepairMultiblock(level, core, player);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (coreState.getValue(DAMAGED)) return InteractionResult.PASS;
        return super.useAtCore(coreState, level, core, player, hit);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos core,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        BlockEntityMachineFluidTank tank = (BlockEntityMachineFluidTank) level.getBlockEntity(core);
        if (tank.lastExplosion == explosion) return;
        tank.lastExplosion = explosion;
        if (tank.isDamaged()) {
            level.removeBlock(core, false);
        } else {
            tank.explode();
            if (explosion.getDirectSourceEntity() instanceof EntityBombletZeta
                    && NTMFluidProperties.hasTrait(tank.tank.getTankType(), FT_Flammable.class)) {
                AwardRegions.within(
                        level,
                        core.getX() + 0.5,
                        core.getY() + 0.5,
                        core.getZ() + 0.5,
                        100,
                        player ->
                                HbmCriteria.detonation(player, DetonationTrigger.Kind.FLUID_TANK));
            }
        }
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
        if (coreState.getValue(DAMAGED)) return InteractionResult.PASS;
        if (!(held.getItem() instanceof FluidIdentifierItem) || player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityMachineFluidTank be))
            return InteractionResult.PASS;

        FluidIdentifierData data =
                held.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        Fluid type = data.primary();
        if (type == null || type == Fluids.EMPTY) return InteractionResult.PASS;

        be.tank.setTankTypeByIdentifier(type);
        be.setChanged();
        player.sendSystemMessage(
                Component.translatable(
                                "desc.fluidtank.changed_type",
                                NTMFluidProperties.getDisplayName(type))
                        .withStyle(ChatFormatting.YELLOW));
        return InteractionResult.CONSUME;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachineFluidTank(pos, state);
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
        return ly == 0 && Math.abs(lx) == 1 && Math.abs(lz) == 1;
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.FLUID_TANK).fluidIn().fluidOut().items();
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        FluidTankContents contents = stack.get(ModDataComponents.FLUID_TANK_CONTENTS.get());
        if (contents == null) return;
        adder.accept(IPersistentInfoProvider.tankLine(contents.tank(), contents.capacity()));
    }
}
