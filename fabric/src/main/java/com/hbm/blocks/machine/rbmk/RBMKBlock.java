/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.machine.rbmk;

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.FluidHandler;
import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKDiagnostics;
import com.hbm.blockentity.machine.rbmk.RbmkParams;
import com.hbm.blocks.TickingEntityBlock;
import com.hbm.blocks.multiblock.MultiblockCellShapes;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.blocks.multiblock.MultiblockLayout;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmCapabilities;
import com.hbm.interfaces.LookOverlay;
import com.hbm.interfaces.LookOverlayProvider;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.RBMKLidItem;
import com.hbm.platform.Caps;
import com.hbm.registration.Registrar;
import com.hbm.registration.RegistryHandle;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class RBMKBlock extends MultiblockCoreBlock
        implements TickingEntityBlock, ToolInteractionHandler, LookOverlayProvider {

    private static final VoxelShape LIDDED = Shapes.box(0D, 0D, 0D, 1D, 1.25D, 1D);

    protected static void declareColumnEnds(RegistryHandle<? extends Block> column) {
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        FluidCaps.RECEIVER,
                        (Supplier<? extends Block>) column,
                        (level, pos, state, be, face) ->
                                columnCap(
                                        level,
                                        pos,
                                        state,
                                        face,
                                        NtmCapabilities.CapRole.FLUID_IN,
                                        false)));
        Caps.pendingBlocks.add(
                new Caps.BlockRegistration<>(
                        FluidCaps.PROVIDER,
                        (Supplier<? extends Block>) column,
                        (level, pos, state, be, face) ->
                                columnCap(
                                        level,
                                        pos,
                                        state,
                                        face,
                                        NtmCapabilities.CapRole.FLUID_OUT,
                                        true)));
    }

    private static @Nullable FluidHandler columnCap(
            Level level,
            BlockPos pos,
            BlockState state,
            FluidFace face,
            NtmCapabilities.CapRole role,
            boolean columnTop) {
        if (!state.hasProperty(PART)) return null;
        boolean isCore = state.getValue(PART) == Part.CORE;
        return isCore == columnTop
                ? null
                : NtmCapabilities.surfaceCap(level, pos, state, face, FluidHandler.class, role);
    }

    public static final EnumProperty<Lid> LID = EnumProperty.create("lid", Lid.class);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final Direction DIR_NO_LID = Direction.NORTH;
    private static final int MAX_ABOVE = 15;
    public static boolean digamma = false;
    public static boolean dropLids = true;

    protected RBMKBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LID, Lid.NONE).setValue(PART, Part.CORE));
    }

    protected static int above() {
        return RbmkParams.heightAboveCore();
    }

    public static Lid lidOf(BlockState state) {
        return state.hasProperty(LID) ? state.getValue(LID) : Lid.NONE;
    }

    private static @Nullable BlockPos coreOf(Level level, BlockPos pos, BlockState state) {
        return MultiblockSurface.coreOfAny(level, pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookOverlay info) {
        RBMKDiagnostics.build(level, pos, level.getBlockState(pos), info);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LID, PART);
    }

    private static int[] columnDimensions() {
        return new int[] {above(), 0, 0, 0, 0, 0};
    }

    @Override
    protected MultiblockLayout runtimeLayout(int declaredCaps) {
        int above = above();
        MultiblockMaskTable.Builder table =
                MultiblockMaskTable.builder().cell(0, 0, 0, coreMask(), ROLE_ALL);
        long[] offsets = new long[above];
        for (int y = 1; y <= above; y++) {
            table.cell(0, y, 0, y == above ? topMask() : MASK_NONE, ROLE_ALL);
            table.passive(
                    0,
                    y,
                    0,
                    MASK_ALL,
                    y == above ? columnPassive() | topPassive() : columnPassive());
            offsets[y - 1] = BlockPos.asLong(0, y, 0);
        }
        table.passive(0, 0, 0, passiveCoreMask(), passiveCoreDomains(declaredCaps));
        if (proxiesTop()) table.flag(0, above, 0, MultiblockMaskTable.FLAG_PROXY);
        int[] dimensions = columnDimensions();
        return MultiblockLayout.uniform(
                declaredCaps,
                table.build(),
                offsets,
                new MultiblockLayout.Placement(
                        dimensions,
                        0,
                        0,
                        new int[] {dimensions[0], 0, 0, 0, 0, 0, 0, 0, 0},
                        new double[0]));
    }

    protected boolean proxiesTop() {
        return false;
    }

    protected int columnPassive() {
        return PASSIVE_NONE;
    }

    protected int topPassive() {
        return PASSIVE_NONE;
    }

    public static void rebakeColumns() {
        for (RegistryHandle<? extends Block> handle : Registrar.registeredBlocks) {
            if (handle.get() instanceof RBMKBlock column)
                column.installBakedLayout(column.declaredCaps());
        }
        MultiblockCellShapes.publish();
    }

    protected int topMask() {
        return MASK_NONE;
    }

    @Override
    protected void visitTeardownCells(BlockPos core, Direction facing, FootprintVisitor visitor) {
        for (int y = 1; y <= MAX_ABOVE; y++) visitor.cell(core.above(y));
    }

    @Override
    protected BlockState cellStateFor(int lx, int ly, int lz, Direction facing, int shapeId) {
        return defaultBlockState().setValue(PART, ly >= above() ? Part.TOP : Part.MIDDLE);
    }

    @Override
    public boolean isCellState(BlockState state) {
        return state.getValue(PART) != Part.CORE;
    }

    @Override
    public boolean usesSharedCells() {
        return false;
    }

    @Override
    public Direction getDirModified(Direction dir) {
        return DIR_NO_LID;
    }

    public int coreMask() {
        return MASK_NONE;
    }

    public boolean hasOwnLid() {
        return false;
    }

    protected abstract BlockEntityType<? extends RBMKBaseBlockEntity> beType();

    protected abstract RBMKBaseBlockEntity createCore(BlockPos pos, BlockState state);

    @Override
    public void reindexLoadedCells(ServerLevel level, BlockPos core) {
        super.reindexLoadedCells(level, core);
        RBMKLoaderBlock.refreshBelow(level, (RBMKBaseBlockEntity) level.getBlockEntity(core));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.CORE ? createCore(pos, state) : null;
    }

    @Override
    protected void removedAt(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.getValue(PART) == Part.CORE) {
            if (dropLids && !movedByPiston) dropLid(level, pos, lidOf(state));
            super.removedAt(state, level, pos, movedByPiston);
            return;
        }
        MultiblockCoreBlock.foldedCellRemoved(level, pos, movedByPiston);
    }

    private static void dropLid(Level level, BlockPos core, Lid lid) {
        if (!lid.present()) return;
        ItemStack drop =
                new ItemStack(
                        lid == Lid.GLASS ? ModItems.RBMK_LID_GLASS.get() : ModItems.RBMK_LID.get());
        level.addFreshEntity(
                new ItemEntity(
                        level,
                        core.getX() + 0.5,
                        core.getY() + 0.5 + RbmkParams.heightAboveCore(),
                        core.getZ() + 0.5,
                        drop));
    }

    @Override
    public @Nullable BlockPos rigidStructureCore(Level level, BlockPos member) {
        BlockState state = level.getBlockState(member);
        if (!(state.getBlock() instanceof RBMKBlock)) return null;
        return coreOf(level, member, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        BlockPos core = coreOf(level, pos, state);
        if (core == null) return InteractionResult.PASS;

        if (level.getBlockEntity(core) instanceof MenuProvider menu) {
            openCoreMenu(player, core, menu);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onToolUse(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || hasOwnLid()) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RBMKBlock)) return false;
        BlockPos core = coreOf(level, pos, state);
        if (core == null) return false;
        BlockState coreState = level.getBlockState(core);
        Lid lid = lidOf(coreState);
        if (!lid.present()) return false;

        if (level instanceof ServerLevel server) {
            dropLid(server, core, lid);
            writeLid(server, core, coreState, Lid.NONE);
        }
        return true;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return hasOwnLid() || lidOf(state).present()
                ? LIDDED
                : super.getCollisionShape(state, getter, pos, context);
    }

    public void writeLid(ServerLevel level, BlockPos core, BlockState coreState, Lid lid) {
        setLid(level, core, coreState, lid);
        long corePacked = core.asLong();
        visitTeardownCells(
                core,
                coreState.getValue(FACING),
                pos -> {
                    BlockState cell = level.getBlockState(pos);
                    if (cell.getBlock() != this || !isCellState(cell)) return;
                    if (MultiblockSurface.recordedCorePacked(
                                    level, pos.getX(), pos.getY(), pos.getZ())
                            != corePacked) {
                        return;
                    }
                    setLid(level, pos.immutable(), cell, lid);
                });
    }

    @Override
    public void assembled(
            LevelAccessor level, BlockPos core, BlockState coreState, @Nullable BoundingBox clip) {
        Lid lid = lidOf(coreState);
        if (!lid.present()) return;
        visitPlacedCells(
                level,
                core,
                coreState.getValue(FACING),
                pos -> {
                    if (clip != null && !clip.isInside(pos)) return;
                    BlockState cell = level.getBlockState(pos);
                    if (cell.getBlock() == this && isCellState(cell))
                        level.setBlock(pos, cell.setValue(LID, lid), 3);
                });
    }

    private static void setLid(ServerLevel level, BlockPos pos, BlockState from, Lid lid) {
        level.setBlock(pos, Block.pushEntitiesUp(from, from.setValue(LID, lid), level, pos), 3);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack held,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (held.getItem() instanceof RBMKLidItem && !hasOwnLid()) {
            BlockPos core = coreOf(level, pos, state);
            if (core != null && !lidOf(level.getBlockState(core)).present())
                return InteractionResult.PASS;
        }
        return super.useItemOn(held, state, level, pos, player, hand, hit);
    }

    public enum Lid implements StringRepresentable {
        NONE("none"),
        CONCRETE("concrete"),
        GLASS("glass");

        private final String name;

        Lid(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public boolean present() {
            return this != NONE;
        }
    }

    public enum Part implements StringRepresentable {
        CORE("core"),
        MIDDLE("middle"),
        TOP("top");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
