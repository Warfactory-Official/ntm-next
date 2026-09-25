// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ITickingBlock;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.inventory.recipes.PedestalRecipe;
import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.particle.helper.ExplosionSmallCreator;
import com.hbm.stats.ModStats;
import com.hbm.tileentity.BlockEntityPedestal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public class BlockPedestal extends Block implements ITickingBlock {

    private static final BlockPos[] RITUAL_OFFSETS = {
        new BlockPos(-2, 0, -2), new BlockPos(0, 0, -3), new BlockPos(2, 0, -2),
        new BlockPos(-3, 0, 0), BlockPos.ZERO, new BlockPos(3, 0, 0),
        new BlockPos(-2, 0, 2), new BlockPos(0, 0, 3), new BlockPos(2, 0, 2)
    };

    public BlockPedestal(Properties properties) {
        super(properties);
    }

    private static BlockEntityPedestal[] findPedestals(Level level, BlockPos center) {
        BlockEntityPedestal[] result = new BlockEntityPedestal[RITUAL_OFFSETS.length];
        for (int i = 0; i < result.length; i++) {
            if (level.getBlockEntity(center.offset(RITUAL_OFFSETS[i]))
                    instanceof BlockEntityPedestal pedestal) result[i] = pedestal;
        }
        return result;
    }

    private static void update(
            Level level, BlockPos pos, BlockState state, BlockEntityPedestal pedestal) {
        pedestal.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    private static boolean matchesExtra(
            Level level, BlockPos pos, PedestalRecipes.ExtraCondition extra) {
        float celestialAngle = celestialAngle(level.getOverworldClockTime());
        return switch (extra) {
            case NONE -> true;
            case FULL_MOON ->
                    celestialAngle >= 0.35F
                            && celestialAngle <= 0.65F
                            && level.environmentAttributes()
                                            .getValue(EnvironmentAttributes.MOON_PHASE, pos)
                                    == MoonPhase.FULL_MOON;
            case NEW_MOON ->
                    celestialAngle >= 0.35F
                            && celestialAngle <= 0.65F
                            && level.environmentAttributes()
                                            .getValue(EnvironmentAttributes.MOON_PHASE, pos)
                                    == MoonPhase.NEW_MOON;
            case SUN -> celestialAngle <= 0.15F || celestialAngle >= 0.85F;
            case BAD_KARMA -> hasReputation(level, pos, -10, false);
            case GOOD_KARMA -> hasReputation(level, pos, 10, true);
        };
    }

    private static float celestialAngle(long time) {
        float angle = (float) Math.floorMod(time, 24000L) / 24000.0F - 0.25F;
        if (angle < 0.0F) angle++;
        if (angle > 1.0F) angle--;
        float linear = angle;
        angle = 1.0F - ((float) Math.cos(angle * Math.PI) + 1.0F) / 2.0F;
        return linear + (angle - linear) / 3.0F;
    }

    private static boolean hasReputation(
            Level level, BlockPos pos, int threshold, boolean positive) {
        AABB nearby =
                new AABB(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 1,
                                pos.getZ() + 1)
                        .inflate(20.0);
        for (Player player : level.getEntitiesOfClass(Player.class, nearby)) {
            int reputation = HbmPlayerProps.getData(player).reputation;
            if (positive ? reputation >= threshold : reputation <= threshold) return true;
        }
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityPedestal(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPedestal pedestal))
            return InteractionResult.PASS;

        ItemStack held = player.getMainHandItem();
        if (pedestal.item.isEmpty() && !held.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            pedestal.item = held.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            update(level, pos, state, pedestal);
            return InteractionResult.SUCCESS;
        }
        if (!pedestal.item.isEmpty() && held.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            player.setItemInHand(InteractionHand.MAIN_HAND, pedestal.item.copy());
            pedestal.item = ItemStack.EMPTY;
            update(level, pos, state, pedestal);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.isClientSide() || !level.hasNeighborSignal(pos)) return;
        if (!(level.getBlockEntity(pos) instanceof BlockEntityPedestal center)) return;

        BlockEntityPedestal[] pedestals = findPedestals(level, pos);
        ItemStack[] stacks = new ItemStack[pedestals.length];
        for (int i = 0; i < pedestals.length; i++) {
            stacks[i] = pedestals[i] == null ? ItemStack.EMPTY : pedestals[i].item;
        }

        for (PedestalRecipe recipe : PedestalRecipes.INSTANCE.recipes()) {
            if (!matchesExtra(level, pos, recipe.extra()) || !recipe.matches(stacks)) continue;

            for (int i = 0; i < pedestals.length; i++) {
                if (i == 4 || pedestals[i] == null || pedestals[i].item.isEmpty()) continue;
                pedestals[i].item = ItemStack.EMPTY;
                update(level, pos.offset(RITUAL_OFFSETS[i]), state, pedestals[i]);
            }

            center.item = recipe.output();
            update(level, pos, state, center);
            ExplosionSmallCreator.composeEffect(
                    level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 10, 2.5F, 1F);

            AABB players =
                    new AABB(
                                    pos.getX() + 0.5,
                                    pos.getY(),
                                    pos.getZ() + 0.5,
                                    pos.getX() + 0.5,
                                    pos.getY(),
                                    pos.getZ() + 0.5)
                            .inflate(50.0);
            for (Player player : level.getEntitiesOfClass(Player.class, players))
                player.awardStat(ModStats.LEGENDARY.get());
            return;
        }
    }
}
