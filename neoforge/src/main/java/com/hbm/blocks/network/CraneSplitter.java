// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.tileentity.network.BlockEntityCraneSplitter;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CraneSplitter extends BlockMultiblockCore
        implements ITickingBlock, IConveyorBelt, IEnterableBlock, IToolable, ILookOverlay {

    public static final MapCodec<CraneSplitter> CODEC = simpleCodec(CraneSplitter::new);

    private static final int[] DIMENSIONS = {0, 0, 0, 0, 0, 1};

    public CraneSplitter(Properties props) {
        super(props);
    }

    public static Direction rightLane(Direction facing) {
        return facing.getCounterClockWise();
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
        return 0;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCraneSplitter(pos, state);
    }

    private @Nullable BlockPos coreOf(Level level, BlockPos pos) {
        FoldedOwner owner = ownerOf(level, pos);
        if (owner != null) return owner.pos();
        return level.getBlockEntity(pos) instanceof BlockEntityCraneSplitter ? pos : null;
    }

    private @Nullable Direction facingAt(Level level, BlockPos pos) {
        BlockPos core = coreOf(level, pos);
        return core == null ? null : coreFacing(level.getBlockState(core));
    }

    public Direction getTravelDirection(Level level, BlockPos pos, @Nullable Vec3 itemPos) {
        Direction facing = facingAt(level, pos);
        return facing == null ? Direction.NORTH : facing;
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return getTravelDirection(level, pos, null) == dir;
    }

    @Override
    public boolean canPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return false;
    }

    @Override
    public void onPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {}

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        BlockPos core = coreOf(level, pos);
        if (core == null) return;
        if (!(level.getBlockEntity(core) instanceof BlockEntityCraneSplitter splitter)) return;

        Direction right = rightLane(coreFacing(level.getBlockState(core)));
        ItemStack[] splits = splitter.splitStack(entity.getItemStack());

        spawnMovingItem(level, core, splits[0]);
        spawnMovingItem(level, core.relative(right), splits[1]);
    }

    private void spawnMovingItem(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;

        Vec3 snap = getClosestSnappingPosition(level, pos, Vec3.atCenterOf(pos));
        EntityMovingItem moving = new EntityMovingItem(level);
        moving.setItemStack(stack);
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        level.addFreshEntity(moving);
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);
        Vec3 snap = snap(pos, clamped, dir);

        Vec3 dest =
                snap.subtract(
                        dir.getStepX() * speed, dir.getStepY() * speed, dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(clamped);
        double len = motion.length();
        return clamped.add(motion.x / len * speed, motion.y / len * speed, motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        return snap(pos, clamp(pos, itemPos), getTravelDirection(level, pos, itemPos));
    }

    private static Vec3 snap(BlockPos pos, Vec3 clamped, Direction dir) {
        double posX = dir.getStepX() != 0 ? clamped.x : pos.getX() + 0.5;
        double posZ = dir.getStepZ() != 0 ? clamped.z : pos.getZ() + 0.5;
        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    private static Vec3 clamp(BlockPos pos, Vec3 itemPos) {
        return new Vec3(
                Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1D),
                itemPos.y,
                Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1D));
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (level.isClientSide()) return true;
        if (tool != ToolType.SCREWDRIVER) return false;

        BlockPos core = coreOf(level, pos);
        if (core == null) return false;
        if (!(level.getBlockEntity(core) instanceof BlockEntityCraneSplitter splitter))
            return false;

        splitter.adjustRatio(core.equals(pos), player.isSecondaryUseActive() ? -1 : 1);
        return true;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockPos core = coreOf(level, pos);
        if (core == null) return;
        if (!(level.getBlockEntity(core) instanceof BlockEntityCraneSplitter splitter)) return;

        info.title(getName().getString(), 0xffff00, 0x404000);
        info.line("Splitter ratio: " + splitter.leftRatio + ":" + splitter.rightRatio);
    }
}
