// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blocks.ITickingBlock;
import com.hbm.capability.NtmContracts;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.inventory.IGUIProvider;
import com.hbm.items.tool.ItemConveyorWand;
import com.hbm.tileentity.network.BlockEntityCraneRouter;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CraneRouter extends Block implements ITickingBlock, IEnterableBlock {

    public static final MapCodec<CraneRouter> CODEC = simpleCodec(CraneRouter::new);

    public static final int[] FACE_COLOURS = {
        0xFFFF0000, 0xFFFF8000, 0xFFFFFF00, 0xFF00FF00, 0xFF0080FF, 0xFF8000FF,
    };

    private static final int UNSORTABLE = 6;

    public CraneRouter(Properties props) {
        super(props);
    }

    public static List<List<ItemStack>> sort(Level level, BlockPos pos, List<ItemStack> stacks) {
        List<List<ItemStack>> output = new ArrayList<>(UNSORTABLE + 1);
        for (int i = 0; i <= UNSORTABLE; i++) output.add(new ArrayList<>());

        if (!(level.getBlockEntity(pos) instanceof BlockEntityCraneRouter router)) return output;

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            Direction dir = getOutputDir(router, stack.copy());
            output.get(dir == null ? UNSORTABLE : dir.ordinal()).add(stack);
        }

        return output;
    }

    public static @Nullable Direction getOutputDir(BlockEntityCraneRouter router, ItemStack stack) {
        List<Direction> valid = new ArrayList<>();

        for (int side = 0; side < BlockEntityCraneRouter.SIDES; side++) {
            int mode = router.modes[side];
            if (mode == BlockEntityCraneRouter.MODE_NONE
                    || mode == BlockEntityCraneRouter.MODE_WILDCARD) {
                continue;
            }

            boolean matchesFilter = false;

            for (int slot = 0; slot < BlockEntityCraneRouter.FILTERS_PER_SIDE; slot++) {
                ItemStack filter =
                        router.getItem(side * BlockEntityCraneRouter.FILTERS_PER_SIDE + slot);
                if (filter.isEmpty()) continue;

                if (router.patterns[side].isValidForFilter(filter, slot, stack)) {
                    matchesFilter = true;
                    break;
                }
            }

            if ((mode == BlockEntityCraneRouter.MODE_WHITELIST && matchesFilter)
                    || (mode == BlockEntityCraneRouter.MODE_BLACKLIST && !matchesFilter)) {
                valid.add(Direction.from3DDataValue(side));
            }
        }

        if (valid.isEmpty()) {
            for (int side = 0; side < BlockEntityCraneRouter.SIDES; side++) {
                if (router.modes[side] == BlockEntityCraneRouter.MODE_WILDCARD) {
                    valid.add(Direction.from3DDataValue(side));
                }
            }
        }

        if (valid.isEmpty()) return null;

        Level level = router.getLevel();
        return valid.get(level == null ? 0 : level.getRandom().nextInt(valid.size()));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCraneRouter(pos, state);
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
        if (stack.getItem() instanceof ItemConveyorWand) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof IGUIProvider provider))
            return InteractionResult.PASS;
        if (!level.isClientSide()) provider.openMenu(player, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return true;
    }

    @Override
    public boolean canPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return true;
    }

    @Override
    public void onItemEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        List<List<ItemStack>> sorted = sort(level, pos, List.of(entity.getItemStack()));

        for (int i = 0; i <= UNSORTABLE; i++) {
            for (ItemStack stack : sorted.get(i)) {
                if (i == UNSORTABLE) spill(level, pos, stack);
                else sendOnRoute(level, pos, stack, Direction.from3DDataValue(i));
            }
        }
    }

    protected void sendOnRoute(Level level, BlockPos pos, ItemStack item, Direction dir) {
        BlockPos target = pos.relative(dir);
        IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, target);
        Vec3 mouth = mouth(pos, dir);

        if (belt == null) {
            level.addFreshEntity(new ItemEntity(level, mouth.x, mouth.y, mouth.z, item));
            return;
        }

        Vec3 snap = belt.getClosestSnappingPosition(level, target, mouth);
        EntityMovingItem moving = new EntityMovingItem(level);
        moving.setItemStack(item);
        moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        level.addFreshEntity(moving);
    }

    @Override
    public void onPackageEnter(
            Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        List<List<ItemStack>> sorted = sort(level, pos, entity.getItemStacks());

        for (int i = 0; i <= UNSORTABLE; i++) {
            List<ItemStack> list = sorted.get(i);
            if (list.isEmpty()) continue;

            if (i == UNSORTABLE) {
                for (ItemStack stack : list) spill(level, pos, stack);
                continue;
            }

            Direction face = Direction.from3DDataValue(i);
            BlockPos target = pos.relative(face);
            IConveyorBelt belt = NtmContracts.CONVEYOR_BELT.at(level, target);
            Vec3 mouth = mouth(pos, face);

            if (belt == null) {
                for (ItemStack stack : list) {
                    level.addFreshEntity(new ItemEntity(level, mouth.x, mouth.y, mouth.z, stack));
                }
                continue;
            }

            Vec3 snap = belt.getClosestSnappingPosition(level, target, mouth);
            EntityMovingPackage moving = new EntityMovingPackage(level);
            moving.setItemStacks(list);
            moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
            level.addFreshEntity(moving);
        }
    }

    private static Vec3 mouth(BlockPos pos, Direction dir) {
        return new Vec3(
                pos.getX() + 0.5 + dir.getStepX() * 0.55,
                pos.getY() + 0.5 + dir.getStepY() * 0.55,
                pos.getZ() + 0.5 + dir.getStepZ() * 0.55);
    }

    private static void spill(Level level, BlockPos pos, ItemStack stack) {
        level.addFreshEntity(
                new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
    }
}
