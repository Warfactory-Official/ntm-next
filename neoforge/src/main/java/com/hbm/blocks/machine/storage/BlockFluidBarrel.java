// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.storage;

import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.items.ModDataComponents;
import com.hbm.items.PersistentInfoBlockItem;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.machine.storage.BlockEntityBarrel;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.util.AutoRotate;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

@AutoRotate
public class BlockFluidBarrel extends Block implements IPersistentInfoProvider {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final VoxelShape SHAPE = Shapes.box(2 / 16D, 0, 2 / 16D, 14 / 16D, 1, 14 / 16D);

    public final int capacity;
    private final int descLines;

    public BlockFluidBarrel(Properties props, int capacity, int descLines) {
        super(props);
        this.capacity = capacity;
        this.descLines = descLines;
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    public static class Functional extends BlockFluidBarrel
            implements ITickingBlock, ICapabilityBlock {

        public Functional(Properties props, int capacity, int descLines) {
            super(props, capacity, descLines);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlockEntityBarrel(pos, state);
        }

        @Override
        protected InteractionResult useItemOn(
                ItemStack stack,
                BlockState state,
                Level level,
                BlockPos pos,
                Player player,
                InteractionHand hand,
                BlockHitResult hit) {

            if (stack.getItem() instanceof FluidIdentifierItem && !player.isShiftKeyDown()) {
                if (!level.isClientSide()
                        && level.getBlockEntity(pos) instanceof BlockEntityBarrel barrel) {
                    FluidIdentifierData data =
                            stack.getOrDefault(
                                    ModDataComponents.FLUID_IDENTIFIER.get(),
                                    FluidIdentifierData.EMPTY);
                    Fluid target = data.primary();
                    barrel.tank.setTankTypeByIdentifier(target == Fluids.EMPTY ? null : target);
                    barrel.setChanged();
                    player.sendSystemMessage(
                            Component.translatable("desc.shared.changedTypeTo")
                                    .append(NTMFluidProperties.getDisplayName(target))
                                    .append("!")
                                    .withStyle(ChatFormatting.YELLOW));
                }
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        @Override
        protected InteractionResult useWithoutItem(
                BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

            if (player.isSecondaryUseActive()) return InteractionResult.SUCCESS;
            if (!level.isClientSide()
                    && level.getBlockEntity(pos) instanceof MenuProvider provider) {
                IGUIProvider.openBlockMenu(player, provider, pos);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected void affectNeighborsAfterRemoval(
                BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {

            LevelNodeGraph<PipeData> graph = FluidPipeGraph.graphAt(level, pos.asLong());
            if (graph != null) graph.removeNode(pos.asLong());
            super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        }

        @Override
        protected void neighborChanged(
                BlockState state,
                Level level,
                BlockPos pos,
                Block block,
                @Nullable Orientation orientation,
                boolean movedByPiston) {
            super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
            if (level.getBlockEntity(pos) instanceof BlockEntityBarrel barrel)
                barrel.markConnectorsDirty();
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        protected int getAnalogOutputSignal(
                BlockState state, Level level, BlockPos pos, Direction direction) {
            return level.getBlockEntity(pos) instanceof BlockEntityMachineBase machine
                    ? machine.getComparatorPower()
                    : 0;
        }

        @Override
        public MachineCaps caps() {
            return MachineCaps.of(ModBlockEntities.FLUID_BARREL)
                    .fluidIn()
                    .fluidOut()
                    .items()
                    .fluidFaces(
                            BlockEntityBarrel.class,
                            (be, face) ->
                                    face.fluid() == null
                                            || be.acceptsFluid(face.fluid(), face.side()));
        }
    }

    public static class BarrelBlockItem extends PersistentInfoBlockItem {

        public BarrelBlockItem(BlockFluidBarrel block, Properties props) {
            super(block, props);
        }

        @Override
        public void appendHoverText(
                ItemStack stack,
                Item.TooltipContext context,
                TooltipDisplay display,
                Consumer<Component> adder,
                TooltipFlag flag) {
            BlockFluidBarrel barrel = (BlockFluidBarrel) getBlock();
            String key = "desc.hbm." + BuiltInRegistries.BLOCK.getKey(barrel).getPath();

            adder.accept(
                    Component.translatable(key + ".capacity", String.format("%,d", barrel.capacity))
                            .withStyle(ChatFormatting.AQUA));
            for (int i = 1; i <= barrel.descLines; i++) {
                adder.accept(Component.translatable(key + ".line" + i));
            }
            super.appendHoverText(stack, context, display, adder, flag);
        }
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        FluidStackNTM content = stack.get(ModDataComponents.FLUID_CONTENT.get());
        if (content == null) return;
        adder.accept(IPersistentInfoProvider.tankLine(content, capacity));
    }
}
