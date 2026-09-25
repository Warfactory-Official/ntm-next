// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.multiblock;

import com.hbm.interfaces.ICopiable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.interfaces.RigidPistonStructure;
import com.hbm.platform.Services;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockMultiblockCell extends Block
        implements ICopiable, ILookOverlay, IToolable, RigidPistonStructure {

    public static final int SOUND_STONE = 0;
    public static final int SOUND_METAL = 1;

    public static final IntegerProperty SOUND =
            IntegerProperty.create("sound", SOUND_STONE, SOUND_METAL);

    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    public static final BooleanProperty COMPARATOR = BooleanProperty.create("comparator");

    public static BlockMultiblockCell create(CellBuckets.Plain key, Properties properties) {
        return CellBuckets.supportsComparator(key)
                ? new Analog(properties)
                : new BlockMultiblockCell(properties);
    }

    private static final class Analog extends BlockMultiblockCell {
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
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        return owner == null
                ? 0
                : owner.block().comparatorOutputAtCell(owner, level, pos, direction);
    }

    public BlockMultiblockCell(Properties properties) {
        super(properties);

        BlockState any = stateDefinition.any();
        if (any.hasProperty(SOUND)) any = any.setValue(SOUND, SOUND_STONE);
        if (any.hasProperty(SEALED)) any = any.setValue(SEALED, Boolean.FALSE);
        registerDefaultState(any);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SOUND, SEALED);
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        return state.getValue(SOUND) == SOUND_METAL ? SoundType.METAL : SoundType.STONE;
    }

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
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        if (owner == null) return InteractionResult.PASS;
        return owner.block()
                .useItemOnAtCore(held, owner.state(), level, owner.pos(), player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        if (owner == null) return InteractionResult.PASS;
        return owner.block().useAtCore(owner.state(), level, owner.pos(), player, hit);
    }

    @Override
    public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        return owner == null ? null : owner.block().getSettings(level, owner.pos());
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        if (owner != null) owner.block().pasteSettings(nbt, index, level, player, owner.pos());
    }

    @Override
    public String @Nullable [] infoForDisplay(Level level, BlockPos pos) {
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        return owner == null ? null : owner.block().infoForDisplay(level, owner.pos());
    }

    @Override
    public String getSettingsSourceID(Level level, BlockPos pos) {
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        return owner == null
                ? ICopiable.super.getSettingsSourceID(level, pos)
                : owner.block().getSettingsSourceID(level, owner.pos());
    }

    @Override
    public Component getSettingsSourceDisplay(Level level, BlockPos pos) {
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        return owner == null
                ? ICopiable.super.getSettingsSourceDisplay(level, pos)
                : owner.block().getSettingsSourceDisplay(level, owner.pos());
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockState(core).getBlock() instanceof IToolable toolable))
            return false;
        return toolable.onScrew(level, player, core, side, hit, tool);
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
        BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(level, pos);
        if (owner != null) {
            owner.block().onExplosionHitAtCore(owner.state(), level, owner.pos(), explosion, onHit);
            return;
        }
        super.onExplosionHit(state, level, pos, explosion, onHit);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server
                && !player.preventsBlockDrops()
                && Services.PLATFORM.canHarvestBlock(level, pos, state, player)) {
            BlockMultiblockCore.FoldedOwner owner = BlockMultiblockCore.ownerOf(server, pos);
            if (owner != null) {
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
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockPos core = MultiblockSurface.clientCoreOf(level, pos);
        if (core != null && level.getBlockState(core).getBlock() instanceof ILookOverlay overlay) {
            overlay.buildLookOverlay(level, core, info);
        }
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
        return other instanceof BlockMultiblockCore || other instanceof BlockMultiblockCell;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        Services.CAPS.invalidateCaps(level, pos);
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
        if (!(level instanceof ServerLevel server)) return;
        BlockPos core = MultiblockSurface.indexedCore(server, pos);
        if (core == null) return;
        BlockMultiblockCore owner = MultiblockSurface.foldedCore(server.getBlockState(core));
        if (owner != null && owner.wantsNeighborUpdates()) {
            owner.cellNeighborChanged(server, core, pos.immutable());
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        BlockMultiblockCore.foldedCellRemoved(level, pos, movedByPiston);
    }
}
