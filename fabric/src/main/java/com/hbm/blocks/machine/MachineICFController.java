// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.AssembledMembers;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.packet.toclient.MarkerPayload;
import com.hbm.platform.Services;
import com.hbm.tileentity.machine.BlockEntityMachineICFController;
import com.hbm.util.BobMathUtil;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineICFController extends Block
        implements ITickingBlock, ILookOverlay, ICapabilityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final int MAX_SIZE = 1024;

    public MachineICFController(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        return new BlockEntityMachineICFController(pos, state);
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.ICF_CONTROLLER).powerIn();
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineICFController controller))
            return InteractionResult.PASS;

        if (!controller.assembled) assemble(level, pos, state, player);
        return InteractionResult.SUCCESS;
    }

    private void assemble(Level level, BlockPos pos, BlockState state, Player player) {
        Map<BlockPos, BlockState> assembly = new HashMap<>();
        Set<BlockPos> ports = new HashSet<>();
        Set<BlockPos> cells = new HashSet<>();
        Set<BlockPos> emitters = new HashSet<>();
        Set<BlockPos> capacitors = new HashSet<>();
        Set<BlockPos> turbochargers = new HashSet<>();

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

            if (b == ModBlocks.ICF_CASING.get()) {
                assembly.put(p, s);
                continue;
            }
            if (b == ModBlocks.ICF_PORT.get()) {
                assembly.put(p, s);
                ports.add(p);
                continue;
            }
            if (b == ModBlocks.ICF_CELL.get()
                    || b == ModBlocks.ICF_EMITTER.get()
                    || b == ModBlocks.ICF_CAPACITOR.get()
                    || b == ModBlocks.ICF_TURBOCHARGER.get()) {
                assembly.put(p, s);
                if (b == ModBlocks.ICF_CELL.get()) cells.add(p);
                else if (b == ModBlocks.ICF_EMITTER.get()) emitters.add(p);
                else if (b == ModBlocks.ICF_CAPACITOR.get()) capacitors.add(p);
                else turbochargers.add(p);

                stack.push(p.north());
                stack.push(p.south());
                stack.push(p.below());
                stack.push(p.above());
                stack.push(p.west());
                stack.push(p.east());
                continue;
            }

            errors.put(p, Component.translatable("marker.hbm.icf.non_laser"));
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
            level.setBlock(partPos, BlockICF.shellOf(entry.getValue()), 3);
            members.add(partPos);
        }
        AssembledMembers.assemble((ServerLevel) level, pos, members);

        if (level.getBlockEntity(pos) instanceof BlockEntityMachineICFController controller) {
            controller.setup(ports, cells, emitters, capacitors, turbochargers);
            controller.assembled(
                    BoundingBox.encapsulatingPositions(assembly.keySet()).orElseThrow());
        }
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntityMachineICFController icf)) return;
        info.title(getName().getString(), 0xffff00, 0x404000);
        info.line(
                BobMathUtil.getShortNumber(icf.getPower())
                        + "/"
                        + BobMathUtil.getShortNumber(icf.getMaxPower())
                        + "HE");
    }
}
