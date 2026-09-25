// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.machine;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.interfaces.IToolable;
import com.hbm.items.machine.ItemStamp;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.BlockEntityConveyorPress;
import com.hbm.util.BobMathUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class MachineConveyorPress extends BlockMultiblockCore
        implements IConveyorBelt, ILookOverlay, IToolable, ITickingBlock, ICapabilityBlock {

    public static final MapCodec<MachineConveyorPress> CODEC =
            simpleCodec(MachineConveyorPress::new);

    private static final int[] DIMENSIONS = {2, 0, 0, 0, 0, 0};

    private static final int RED = 0xFF5555;

    public MachineConveyorPress(Properties props) {
        super(props);
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
    public MachineCaps caps() {

        return MachineCaps.of(ModBlockEntities.CONVEYOR_PRESS)
                .powerIn()
                .fe()
                .faces(BlockEntityConveyorPress.class, (be, side) -> side != Direction.DOWN);
    }

    @Override
    public int coreMask() {
        return MASK_HORIZONTAL;
    }

    private @Nullable BlockEntityConveyorPress pressUnder(Level level, BlockPos beltPos) {
        return level.getBlockEntity(beltPos.below()) instanceof BlockEntityConveyorPress press
                ? press
                : null;
    }

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return pressUnder(level, pos) != null;
    }

    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        BlockPos core = pos.below();
        return level.getBlockState(core).getValue(FACING).getClockWise();
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 clamped = clamp(pos, itemPos);
        Vec3 snap = snap(level, pos, clamped, dir);

        Vec3 dest =
                snap.subtract(
                        dir.getStepX() * speed, dir.getStepY() * speed, dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(clamped);
        double len = motion.length();
        return clamped.add(motion.x / len * speed, motion.y / len * speed, motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        return snap(level, pos, clamp(pos, itemPos), getTravelDirection(level, pos, itemPos));
    }

    private static Vec3 snap(Level level, BlockPos pos, Vec3 clamped, Direction dir) {
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
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityConveyorPress(pos, state);
    }

    @Override
    protected InteractionResult useAtCore(
            BlockState coreState, Level level, BlockPos core, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof BlockEntityConveyorPress press)) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemStamp)
                || !press.getItem(BlockEntityConveyorPress.SLOT_STAMP).isEmpty()) {
            return InteractionResult.PASS;
        }

        ItemStack stamp = held.copyWithCount(1);
        press.setItem(BlockEntityConveyorPress.SLOT_STAMP, stamp);
        held.consume(1, player);
        level.playSound(null, core, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        press.markChanged();
        return InteractionResult.SUCCESS;
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

        BlockPos core = coreOf(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof BlockEntityConveyorPress press))
            return false;

        ItemStack stamp = press.getItem(BlockEntityConveyorPress.SLOT_STAMP);
        if (stamp.isEmpty()) return false;

        player.getInventory().placeItemBackInInventory(stamp.copy());
        press.setItem(BlockEntityConveyorPress.SLOT_STAMP, ItemStack.EMPTY);
        press.markChanged();
        return true;
    }

    private static @Nullable BlockPos coreOf(Level level, BlockPos pos) {
        FoldedOwner owner = ownerOf(level, pos);
        if (owner != null) return owner.pos();
        return level.getBlockEntity(pos) instanceof BlockEntityConveyorPress ? pos : null;
    }

    @Override
    public void buildLookOverlay(Level level, BlockPos pos, LookInfo info) {
        BlockPos core = coreOf(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof BlockEntityConveyorPress press))
            return;

        info.title(getName().getString(), 0xffff00, 0x404000);
        info.line(
                BobMathUtil.getShortNumber(press.power)
                        + "HE / "
                        + BobMathUtil.getShortNumber(BlockEntityConveyorPress.MAX_POWER)
                        + "HE");

        ItemStack stamp = press.syncStack;
        info.line(
                "Installed stamp: " + (stamp.isEmpty() ? "NONE" : stamp.getHoverName().getString()),
                stamp.isEmpty() ? RED : 0xFFFFFF);
    }
}
