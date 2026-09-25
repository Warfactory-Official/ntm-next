// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.blocks.IPersistentInfoProvider;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.storage.BlockEntityBatteryREDD;
import com.hbm.tileentity.machine.storage.BlockEntityMachineBattery;
import com.hbm.util.BobMathUtil;
import com.mojang.serialization.MapCodec;
import java.math.BigInteger;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MachineBatteryREDD extends BlockMultiblockCore
        implements ITickingBlock, ICapabilityBlock, IPersistentInfoProvider {

    public static final MapCodec<MachineBatteryREDD> CODEC = simpleCodec(MachineBatteryREDD::new);

    private static final int[] DIMENSIONS = {9, 0, 2, 2, 4, 4};

    public MachineBatteryREDD(Properties props) {
        super(props);
    }

    public static BlockPos[] ports(BlockPos core, Direction facing) {
        Direction across = facing.getClockWise();
        BlockPos[] out = new BlockPos[6];
        int i = 0;
        for (int forward : new int[] {2, -2}) {
            for (int side : new int[] {2, -2}) {
                out[i++] = core.relative(facing, forward).relative(across, side);
            }
        }
        out[i++] = core.relative(across, 4);
        out[i] = core.relative(across, -4);
        return out;
    }

    private static Direction[] portFaces(Direction facing) {
        Direction across = facing.getClockWise();
        return new Direction[] {
            facing, facing, facing.getOpposite(), facing.getOpposite(), across, across.getOpposite()
        };
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public int[] getDimensions() {
        return DIMENSIONS;
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        for (BlockPos port : ports(core, facing)) {
            visitor.passiveCell(port, MASK_ALL, PASSIVE_POWER_IN);
        }
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        BlockPos[] ports = ports(core, facing);
        MultiblockHandlerXR.visitBox(
                core,
                getDimensions(),
                facing,
                (CellVisitor)
                        (pos, mask) -> {
                            for (BlockPos port : ports) {
                                if (port.equals(pos)) {
                                    visitor.cell(pos, MASK_ALL);
                                    return;
                                }
                            }
                            visitor.cell(pos, mask);
                        });
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean wantsNeighborUpdates() {
        return true;
    }

    @Override
    public void cellNeighborChanged(ServerLevel level, BlockPos core, BlockPos cell) {
        if (level.getBlockEntity(core) instanceof BlockEntityMachineBattery battery)
            battery.refreshMode();
    }

    @Override
    public MachineCaps caps() {
        return MachineCaps.of(ModBlockEntities.BATTERY_REDD).powerIn().powerOut().items().fe();
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            mintRing(server, pos, state, state.getValue(FACING));
        }
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.removedAt(state, level, pos, movedByPiston);
        Direction facing = coreFacing(state);
        visitRing(pos, facing, (cell, mask) -> CableConductorBlockBase.dropNode(level, cell));
    }

    private void mintRing(ServerLevel server, BlockPos core, BlockState state, Direction facing) {
        visitRing(
                core,
                facing,
                (cell, mask) -> CableConductorBlockBase.mintNode(server, cell, state, mask));
    }

    private static void visitRing(BlockPos core, Direction facing, RingVisitor visitor) {
        Direction across = facing.getClockWise();
        BlockPos[] ports = ports(core, facing);
        Direction[] faces = portFaces(facing);

        for (int t = -3; t <= 3; t++) {
            visitor.cell(
                    core.relative(across, t),
                    (1 << across.ordinal())
                            | (1 << across.getOpposite().ordinal())
                            | (t == 2 || t == -2
                                    ? (1 << facing.ordinal())
                                            | (1 << facing.getOpposite().ordinal())
                                    : 0));
        }

        for (int side : new int[] {2, -2}) {
            for (int forward : new int[] {1, -1}) {
                visitor.cell(
                        core.relative(across, side).relative(facing, forward),
                        (1 << facing.ordinal()) | (1 << facing.getOpposite().ordinal()));
            }
        }

        for (int i = 0; i < ports.length; i++) {
            visitor.cell(
                    ports[i], (1 << faces[i].ordinal()) | (1 << faces[i].getOpposite().ordinal()));
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityBatteryREDD(pos, state);
    }

    private interface RingVisitor {
        void cell(BlockPos pos, int mask);
    }

    @Override
    public void appendPersistentInfo(ItemStack stack, Consumer<Component> adder) {
        BigInteger charge = stack.get(ModDataComponents.REDD_CHARGE.get());
        if (charge == null) return;
        adder.accept(
                Component.literal(BobMathUtil.format(charge) + " HE")
                        .withStyle(ChatFormatting.YELLOW));
    }
}
