// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.IGUIProvider;
import com.hbm.items.machine.ItemPWRPrinter;
import com.hbm.packet.toclient.MarkerPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachinePWRController;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachinePWRController extends Block implements ITickingBlock, ICapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int MAX_SIZE = 4096;

    public MachinePWRController(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    static boolean isValidCore(Block b) {
        return b == ModBlocks.PWR_FUEL.get()
                || b == ModBlocks.PWR_CONTROL.get()
                || b == ModBlocks.PWR_CHANNEL.get()
                || b == ModBlocks.PWR_HEATEX.get()
                || b == ModBlocks.PWR_HEATSINK.get()
                || b == ModBlocks.PWR_NEUTRON_SOURCE.get();
    }

    private static boolean isValidCasing(Block b) {
        return b == ModBlocks.PWR_CASING.get()
                || b == ModBlocks.PWR_REFLECTOR.get()
                || b == ModBlocks.PWR_PORT.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityMachinePWRController(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.PWR_CONTROLLER).fluidIn().fluidOut();
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
        if (stack.getItem() instanceof ItemPWRPrinter) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachinePWRController controller))
            return InteractionResult.PASS;

        if (!controller.assembled) {
            assemble(level, pos, state, player);
        } else {
            IGUIProvider.openBlockMenu(player, controller, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private void assemble(Level level, BlockPos pos, BlockState state, Player player) {
        Map<BlockPos, BlockState> assembly = new HashMap<>();
        Map<BlockPos, BlockState> fuelRods = new HashMap<>();
        int sources = 0;

        assembly.put(pos, state);
        Direction dir = state.getValue(FACING).getOpposite();

        ArrayDeque<BlockPos> stack = new ArrayDeque<>();
        stack.push(pos.relative(dir));

        Map<BlockPos, Component> errors = new LinkedHashMap<>();
        while (!stack.isEmpty()) {
            BlockPos p = stack.pop();
            if (assembly.containsKey(p)) continue;
            if (assembly.size() >= MAX_SIZE) {
                errors.put(p, Component.translatable("marker.hbm.max_size"));
                continue;
            }

            BlockState s = level.getBlockState(p);
            Block b = s.getBlock();

            if (isValidCasing(b)) {
                assembly.put(p, s);
                continue;
            }
            if (isValidCore(b)) {
                assembly.put(p, s);
                if (b == ModBlocks.PWR_FUEL.get()) fuelRods.put(p, s);
                if (b == ModBlocks.PWR_NEUTRON_SOURCE.get()) sources++;

                stack.push(p.north());
                stack.push(p.south());
                stack.push(p.below());
                stack.push(p.above());
                stack.push(p.west());
                stack.push(p.east());
                continue;
            }
            errors.put(p, Component.translatable("marker.hbm.pwr.non_reactor"));
        }

        if (fuelRods.isEmpty()) {
            errors.put(pos, Component.translatable("marker.hbm.pwr.fuel_required"));
        }
        if (sources == 0) {
            errors.put(pos, Component.translatable("marker.hbm.pwr.source_required"));
        }

        if (!errors.isEmpty()) {
            if (player instanceof ServerPlayer sp) {
                Services.NETWORK.sendTo(new MarkerPayload(0xff0000, 5000, 128.0, errors), sp);
            }
            return;
        }

        List<BlockPos> members = new ArrayList<>(assembly.size());
        for (Map.Entry<BlockPos, BlockState> entry : assembly.entrySet()) {
            BlockPos partPos = entry.getKey();
            if (partPos.equals(pos)) continue;
            level.setBlock(partPos, BlockPWR.shellOf(entry.getValue()), 3);
            members.add(partPos);
        }
        AssembledMembers.assemble((ServerLevel) level, pos, members);

        if (level.getBlockEntity(pos) instanceof BlockEntityMachinePWRController controller) {
            controller.setup(assembly, fuelRods);
            controller.assembled = true;
            controller.markChanged();
        }
    }
}
