// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemScraps;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockDynamicSlag extends Block implements EntityBlock {

    public BlockDynamicSlag(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof BlockEntitySlag slag && slag.amount > 0) {
            return Shapes.box(
                    0D,
                    0D,
                    0D,
                    1D,
                    Math.min(1D, (double) slag.amount / BlockEntitySlag.MAX_AMOUNT),
                    1D);
        }
        return Shapes.box(0D, 0D, 0D, 1D, 0.0625D, 1D);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof BlockEntitySlag self)) {
            level.removeBlock(pos, false);
            return;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.canBeReplaced() && below.getY() >= level.getMinY()) {
            level.setBlockAndUpdate(below, ModBlocks.SLAG.get().defaultBlockState());
            if (level.getBlockEntity(below) instanceof BlockEntitySlag tile) {
                tile.mat = self.mat;
                tile.amount = self.amount;
                tile.setChanged();
            }
            level.removeBlock(pos, false);
            level.scheduleTick(below, this, 1);
            return;
        } else if (level.getBlockEntity(below) instanceof BlockEntitySlag belowSlag) {
            if (belowSlag.mat == self.mat && belowSlag.amount < BlockEntitySlag.MAX_AMOUNT) {
                int transfer = Math.min(BlockEntitySlag.MAX_AMOUNT - belowSlag.amount, self.amount);
                belowSlag.amount += transfer;
                self.amount -= transfer;

                if (self.amount <= 0) {
                    level.removeBlock(pos, false);
                } else {
                    self.setChanged();
                }
                belowSlag.setChanged();
                level.scheduleTick(below, this, 1);
                return;
            }
        }

        Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        int count = 0;
        for (Direction dir : sides) {
            if (level.getBlockState(pos.relative(dir)).canBeReplaced()) count++;
        }

        if (self.amount >= BlockEntitySlag.MAX_AMOUNT / 5 && count > 0) {
            int toSpread = Math.max(self.amount / (count * 2), 1);

            for (Direction dir : sides) {
                BlockPos target = pos.relative(dir);
                if (level.getBlockState(target).canBeReplaced()) {
                    level.setBlockAndUpdate(target, ModBlocks.SLAG.get().defaultBlockState());
                    if (level.getBlockEntity(target) instanceof BlockEntitySlag tile) {
                        tile.mat = self.mat;
                        tile.amount = toSpread;
                        tile.setChanged();
                    }
                    self.amount -= toSpread;
                    self.setChanged();
                    level.scheduleTick(target, this, 1);
                }
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.isCreative()
                && level.getBlockEntity(pos) instanceof BlockEntitySlag slag
                && slag.mat != null
                && slag.amount > 0) {
            level.addFreshEntity(
                    new ItemEntity(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            ItemScraps.create(new MaterialStack(slag.mat, slag.amount))));
            slag.amount = 0;
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {

        Float radius = params.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
        if (radius != null && params.getLevel().getRandom().nextFloat() > 1.0F / radius)
            return List.of();
        return params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                                instanceof BlockEntitySlag slag
                        && slag.mat != null
                        && slag.amount > 0
                ? List.of(ItemScraps.create(new MaterialStack(slag.mat, slag.amount)))
                : List.of();
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof BlockEntitySlag slag && slag.mat != null) {
            return ItemScraps.create(new MaterialStack(slag.mat, slag.amount));
        }
        return super.getCloneItemStack(level, pos, state, includeData);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntitySlag(pos, state);
    }

    public static class BlockEntitySlag extends BlockEntity
            implements Synced, SyncUnitSchema, GraphResident, FoldedCoreResident {

        public static final int MAX_AMOUNT = MaterialShapes.BLOCK.q(16);

        @SyncField(units = 1L)
        public @Nullable NTMMaterial mat;

        @SyncField(units = 2L)
        public int amount;

        public BlockEntitySlag(BlockPos pos, BlockState state) {
            super(ModBlockEntities.FOUNDRY_SLAG.get(), pos, state);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            networkPackNT(50);
        }

        @Override
        public long syncUnitMask() {
            return 3L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            switch (unit) {
                case 0 -> output.writeInt(mat == null ? -1 : mat.id);
                case 1 -> output.writeInt(amount);
                default -> throw new IllegalArgumentException();
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            switch (unit) {
                case 0 -> mat = Mats.matById.get(input.readInt());
                case 1 -> amount = input.readInt();
                default -> throw new IllegalArgumentException();
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            this.mat = Mats.matById.get(input.getIntOr("mat", -1));
            this.amount = input.getIntOr("amount", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (this.mat != null) output.putInt("mat", this.mat.id);
            output.putInt("amount", this.amount);
        }
    }
}
