// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.IInsertable;
import com.hbm.interfaces.IToolable;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.special.Autogen;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BlockGraphiteDrilledBase extends Block implements IToolable, IInsertable {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty SHROUDED = BooleanProperty.create("shrouded");

    public static final BooleanProperty PU239 = BooleanProperty.create("pu239");

    public static final BooleanProperty WITHDRAWN = BooleanProperty.create("withdrawn");
    private static final Logger LOGGER = LoggerFactory.getLogger("NTM");

    protected BlockGraphiteDrilledBase(BlockBehaviour.Properties props) {
        super(props);
        BlockState def =
                stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(SHROUDED, false);
        if (def.hasProperty(PU239)) def = def.setValue(PU239, false);
        if (def.hasProperty(WITHDRAWN)) def = def.setValue(WITHDRAWN, false);
        registerDefaultState(def);
    }

    public static BlockBehaviour.Properties pileProperties() {
        return BlockBehaviour.Properties.of()
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    public static boolean isChannelFace(BlockState state, @Nullable Direction side) {
        return side != null && side.getAxis() == state.getValue(AXIS);
    }

    public static BlockState copyShape(BlockState from, BlockState to) {
        return to.setValue(AXIS, from.getValue(AXIS)).setValue(SHROUDED, from.getValue(SHROUDED));
    }

    protected static void ejectItem(Level level, BlockPos pos, Direction dir, ItemStack stack) {
        ItemEntity dust =
                new ItemEntity(
                        level,
                        pos.getX() + 0.5D + dir.getStepX() * 0.75D,
                        pos.getY() + 0.5D + dir.getStepY() * 0.75D,
                        pos.getZ() + 0.5D + dir.getStepZ() * 0.75D,
                        stack);
        dust.setDeltaMovement(dir.getStepX() * 0.25, dir.getStepY() * 0.25, dir.getStepZ() * 0.25);
        level.addFreshEntity(dust);
    }

    public static Item aluminiumShell() {
        return Autogen.require(MaterialShapes.SHELL, Mats.MAT_ALUMINIUM);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, SHROUDED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {

        if (tool != ToolType.SCREWDRIVER) return false;

        if (!level.isClientSide()) {

            BlockState state = level.getBlockState(pos);

            if (isChannelFace(state, side)) {
                Item inserted = getInsertedItem(state);
                level.setBlock(
                        pos,
                        copyShape(
                                state, ModBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()),
                        3);
                ejectItem(level, pos, side, new ItemStack(inserted));
            }
        }

        return true;
    }

    protected @Nullable Item getInsertedItem(BlockState state) {
        return getInsertedItem();
    }

    public @Nullable Item getInsertedItem() {
        return null;
    }

    protected @Nullable BlockState checkInteractions(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.PILE_ROD_URANIUM.get())
            return ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_PU239.get())
            return ModBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState().setValue(PU239, true);
        if (item == ModItems.PILE_ROD_PLUTONIUM.get())
            return ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_SOURCE.get())
            return ModBlocks.BLOCK_GRAPHITE_SOURCE.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_BORON.get())
            return ModBlocks.BLOCK_GRAPHITE_ROD.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_LITHIUM.get())
            return ModBlocks.BLOCK_GRAPHITE_LITHIUM.get().defaultBlockState();
        if (item == ModItems.CELL_TRITIUM.get())
            return ModBlocks.BLOCK_GRAPHITE_TRITIUM.get().defaultBlockState();
        if (item == ModItems.PILE_ROD_DETECTOR.get())
            return ModBlocks.BLOCK_GRAPHITE_DETECTOR.get().defaultBlockState();
        return null;
    }

    @Override
    public boolean insertItem(Level level, BlockPos pos, Direction dir, ItemStack stack) {

        if (stack.isEmpty()) return false;

        BlockState insert = checkInteractions(stack);
        if (insert == null) return false;

        BlockState here = level.getBlockState(pos);
        Direction.Axis axis = here.getValue(AXIS);
        if (dir.getAxis() != axis) return false;

        for (int i = 0; i <= 3; i++) {
            BlockPos probe = pos.relative(dir, i);
            BlockState state = level.getBlockState(probe);

            if (state.getBlock() instanceof BlockGraphiteDrilledBase graphite) {
                if (state.getValue(AXIS) != axis) return false;

                if (graphite.getInsertedItem(state) == null) break;
                else if (i >= 3) return false;
            } else {

                if (state.isCollisionShapeFullBlock(level, probe)
                        && state.canOcclude()
                        && !state.isSignalSource()) return false;
                else break;
            }
        }

        HolderLookup.Provider registries = level.registryAccess();
        BlockState carried = copyShape(here, insert);
        CompoundTag carriedData = new CompoundTag();

        for (int i = 0; i <= 3; i++) {
            BlockPos probe = pos.relative(dir, i);
            BlockState state = level.getBlockState(probe);

            if (state.getBlock() instanceof BlockGraphiteDrilledBase) {
                BlockEntity existing = level.getBlockEntity(probe);
                CompoundTag data = existing != null ? existing.saveCustomOnly(registries) : null;

                level.setBlock(probe, carried.setValue(SHROUDED, state.getValue(SHROUDED)), 3);

                if (carriedData != null) {
                    BlockEntity moved = level.getBlockEntity(probe);
                    if (moved != null) {
                        try (ProblemReporter.ScopedCollector reporter =
                                new ProblemReporter.ScopedCollector(moved.problemPath(), LOGGER)) {
                            moved.loadCustomOnly(
                                    TagValueInput.create(reporter, registries, carriedData));
                        }
                        moved.setChanged();
                    }
                }

                carried = state;
                carriedData = data;

                if (state.getBlock() instanceof BlockGraphiteDrilled) break;
            } else {
                Item eject =
                        ((BlockGraphiteDrilledBase) carried.getBlock()).getInsertedItem(carried);
                ejectItem(level, probe.relative(dir.getOpposite()), dir, new ItemStack(eject));
                level.playSound(
                        null, probe, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.25F, 1.0F);
                break;
            }
        }

        return true;
    }
}
