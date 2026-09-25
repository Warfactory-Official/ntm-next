// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.rbmk;

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.capability.NtmCapabilities;
import com.hbm.config.HudConfig;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKNeutronNode;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemRBMKLid;
import com.hbm.platform.Services;
import com.hbm.registration.RegistryHandle;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.RBMKConfig;
import com.hbm.tileentity.machine.rbmk.RBMKDiagnostics;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class RBMKBase extends BlockMultiblockCore
        implements ITickingBlock, IToolable, ILookOverlay {

    private static final VoxelShape LIDDED = Shapes.box(0D, 0D, 0D, 1D, 1.25D, 1D);

    protected static void declareColumnEnds(RegistryHandle<? extends Block> column) {
        Services.CAPS.registerBlockProvider(
                FluidCaps.RECEIVER,
                column,
                (level, pos, state, be, face) ->
                        columnCap(
                                level,
                                pos,
                                state,
                                face.side(),
                                NtmCapabilities.CapRole.FLUID_IN,
                                false));
        Services.CAPS.registerBlockProvider(
                FluidCaps.PROVIDER,
                column,
                (level, pos, state, be, face) ->
                        columnCap(
                                level,
                                pos,
                                state,
                                face.side(),
                                NtmCapabilities.CapRole.FLUID_OUT,
                                true));
    }

    private static @Nullable IFluidHandlerMK2 columnCap(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable Direction side,
            NtmCapabilities.CapRole role,
            boolean columnTop) {
        if (!state.hasProperty(PART)) return null;
        boolean isCore = state.getValue(PART) == Part.CORE;
        return isCore == columnTop
                ? null
                : NtmCapabilities.fullSurfaceCap(
                        level, pos, state, side, IFluidHandlerMK2.class, role);
    }

    public static final EnumProperty<Lid> LID = EnumProperty.create("lid", Lid.class);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    public static final Direction DIR_NO_LID = Direction.NORTH;

    private static final int MAX_ABOVE = 15;

    public static boolean digamma = false;

    public static boolean dropLids = true;

    protected RBMKBase(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LID, Lid.NONE).setValue(PART, Part.CORE));
    }

    protected static int above() {
        return RBMKConfig.getColumnHeight(null);
    }

    public static Lid lidOf(BlockState state) {
        return state.hasProperty(LID) ? state.getValue(LID) : Lid.NONE;
    }

    private static @Nullable BlockPos coreOf(Level level, BlockPos pos, BlockState state) {
        return MultiblockSurface.coreOfAny(level, pos, state);
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        if (HudConfig.doddRbmkDiagnostic)
            RBMKDiagnostics.build(level, pos, level.getBlockState(pos), info);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LID, PART);
    }

    @Override
    public int[] getDimensions() {
        return new int[] {above(), 0, 0, 0, 0, 0};
    }

    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        int above = above();
        for (int y = 1; y <= above; y++) {
            visitor.cell(core.above(y), y == above ? topMask() : MASK_NONE);
        }
    }

    public static void rebakeColumns() {
        for (RegistryHandle<? extends Block> handle : Services.REGISTRAR.blocks()) {
            if (handle.get() instanceof RBMKBase column)
                column.bakeMaskTable(column.maskTable().declaredCaps);
        }
    }

    protected int topMask() {
        return MASK_NONE;
    }

    @Override
    protected void visitTeardownCells(BlockPos core, Direction facing, CellVisitor visitor) {

        for (int y = 1; y <= MAX_ABOVE; y++) visitor.cell(core.above(y), MASK_NONE);
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
    public int getOffset() {
        return 0;
    }

    @Override
    public Direction getDirModified(Direction dir) {
        return DIR_NO_LID;
    }

    @Override
    public int coreMask() {
        return MASK_NONE;
    }

    public boolean hasOwnLid() {
        return false;
    }

    protected abstract BlockEntityType<? extends BlockEntityRBMKBase> beType();

    protected abstract BlockEntityRBMKBase createCore(BlockPos pos, BlockState state);

    @Override
    public void reindexLoadedCells(ServerLevel level, BlockPos core) {
        super.reindexLoadedCells(level, core);
        RBMKLoader.refreshBelow(level, (BlockEntityRBMKBase) level.getBlockEntity(core));
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
        BlockMultiblockCore.foldedCellRemoved(level, pos, movedByPiston);
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
                        core.getY() + 0.5 + RBMKConfig.getColumnHeight(level),
                        core.getZ() + 0.5,
                        drop));
    }

    @Override
    public @Nullable BlockPos rigidStructureCore(Level level, BlockPos member) {
        BlockState state = level.getBlockState(member);
        if (!(state.getBlock() instanceof RBMKBase)) return null;
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
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            IToolable.ToolType tool) {
        if (tool != IToolable.ToolType.SCREWDRIVER || hasOwnLid()) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RBMKBase)) return false;
        BlockPos core = coreOf(level, pos, state);
        if (core == null) return false;
        BlockState coreState = level.getBlockState(core);
        Lid lid = lidOf(coreState);
        if (!lid.present()) return false;

        if (NeutronNodeWorld.getNode(level, core) instanceof RBMKNeutronNode node) node.removeLid();
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
                (pos, mask) -> {
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
        if (held.getItem() instanceof ItemRBMKLid && !hasOwnLid()) {
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
