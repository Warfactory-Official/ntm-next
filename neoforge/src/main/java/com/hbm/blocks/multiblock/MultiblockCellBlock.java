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

package com.hbm.blocks.multiblock;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.PlantSoil;
import com.hbm.interfaces.RigidPistonStructure;
import com.hbm.platform.Caps;
import com.hbm.platform.Platform;
import com.hbm.registration.RegistryHandle;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class MultiblockCellBlock extends Block implements RigidPistonStructure, PlantSoil {

    public static final int SOUND_STONE = 0;
    public static final int SOUND_METAL = 1;

    public static final BooleanProperty COMPARATOR = BooleanProperty.create("comparator");

    private boolean forwardsNeighbors;

    public static MultiblockCellBlock create(CellBuckets.Plain key, Properties properties) {
        return CellBuckets.supportsComparator(key)
                ? new Analog(properties)
                : new MultiblockCellBlock(properties);
    }

    private static final class Analog extends MultiblockCellBlock {
        private Analog(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(COMPARATOR, false));
        }

        @Override
        protected void createBlockStateDefinition(
                StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(COMPARATOR);
        }

        @Override
        protected boolean hasAnalogOutputSignal(BlockState state) {
            return state.getValue(COMPARATOR);
        }

        @Override
        protected int getAnalogOutputSignal(
                BlockState state, Level level, BlockPos pos, Direction direction) {
            return analogOutputAtCell(level, pos, direction);
        }
    }

    static int analogOutputAtCell(Level level, BlockPos pos, Direction direction) {
        MultiblockCoreBlock.FoldedOwner owner = MultiblockCoreBlock.ownerOf(level, pos);
        return owner == null
                ? 0
                : owner.block().comparatorOutputAtCell(owner, level, pos, direction);
    }

    public MultiblockCellBlock(Properties properties) {
        super(
                properties
                        .isValidSpawn((state, level, pos, type) -> false)
                        .isSuffocating((state, level, pos) -> false)
                        .isViewBlocking((state, level, pos) -> false));
        registerDefaultState(stateDefinition.any());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {}

    @Override
    public float getDestroyProgress(
            BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockPos core = level instanceof Level l ? MultiblockSurface.coreOfAny(l, pos) : null;
        if (core != null) {
            return level.getBlockState(core).getDestroyProgress(player, level, core);
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
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
        MultiblockCoreBlock.FoldedOwner owner = MultiblockCoreBlock.ownerOf(level, pos);
        if (owner == null) return InteractionResult.PASS;
        return owner.block()
                .useItemOnAtCore(held, owner.state(), level, owner.pos(), player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        MultiblockCoreBlock.FoldedOwner owner = MultiblockCoreBlock.ownerOf(level, pos);
        if (owner == null) return InteractionResult.PASS;
        return owner.block().useAtCore(owner.state(), level, owner.pos(), player, hit);
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockPos core =
                level instanceof ServerLevel server
                        ? MultiblockSurface.indexedCore(server, pos)
                        : MultiblockSurface.clientCoreOf(level, pos);
        if (core == null) return ItemStack.EMPTY;
        return level.getBlockState(core).getCloneItemStack(level, core, includeData);
    }

    @Override
    protected void onExplosionHit(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> onHit) {
        MultiblockCoreBlock.FoldedOwner owner = MultiblockCoreBlock.ownerOf(level, pos);
        if (owner != null) {
            owner.block().onExplosionHitAtCore(owner.state(), level, owner.pos(), explosion, onHit);
            return;
        }
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !player.preventsBlockDrops()) {
            MultiblockCoreBlock.FoldedOwner owner = MultiblockCoreBlock.ownerOf(server, pos);
            if (owner != null
                    && Platform.canHarvestBlock(server, owner.pos(), owner.state(), player)) {
                Block.dropResources(
                        owner.state(),
                        server,
                        pos,
                        server.getBlockEntity(owner.pos()),
                        player,
                        player.getMainHandItem());
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public @Nullable BlockPos rigidStructureCore(Level level, BlockPos member) {
        return MultiblockSurface.coreOfAny(level, member);
    }

    @Override
    public List<BlockPos> rigidStructureBlocks(Level level, BlockPos core) {
        return MultiblockSurface.rigidStructureBlocks(level, core);
    }

    @Override
    public boolean isSameMultiblock(Block other) {
        return other instanceof MultiblockCoreBlock || other instanceof MultiblockCellBlock;
    }

    @Override
    public boolean sustainsPlant(BlockGetter level, BlockPos soil, BlockState soilState) {
        return MultiblockSurface.sustainsPlant(level, soil, soilState);
    }

    @Override
    public TriState canSustainPlant(
            BlockState state,
            BlockGetter level,
            BlockPos soilPosition,
            Direction facing,
            BlockState plant) {
        return sustainsPlant(level, soilPosition, state) ? TriState.TRUE : TriState.DEFAULT;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        Caps.invalidateCaps(level, pos);
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
        if (!forwardsNeighbors || !(level instanceof ServerLevel server)) return;
        BlockPos core = MultiblockSurface.indexedCore(server, pos);
        if (core == null) return;
        MultiblockCoreBlock owner = MultiblockSurface.foldedCore(server.getBlockState(core));
        if (owner != null && owner.wantsNeighborUpdates()) {
            owner.cellNeighborChanged(server, core, pos.immutable());
        }
    }

    public boolean forwardsNeighbors() {
        return forwardsNeighbors;
    }

    void forwardNeighborUpdates() {
        forwardsNeighbors = true;
    }

    public static void resetNeighborForwarding() {
        for (RegistryHandle<? extends Block> handle : ModBlocks.cellHandles()) {
            if (handle.get() instanceof MultiblockCellBlock cell) cell.forwardsNeighbors = false;
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        MultiblockCoreBlock.foldedCellRemoved(level, pos, movedByPiston);
    }

    public boolean bulkCellRelevant(BlockState ignoredState) {
        return true;
    }

    public boolean bulkSnapshotCell(BlockState ignoredState) {
        return true;
    }
}
