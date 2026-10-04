// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.inventory.BlockMenuContext;
import com.hbm.inventory.IGUIProvider;
import com.hbm.tags.HbmItemTags;
import com.hbm.util.ChunkUtil;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public final class FabricPlatform {
    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean allowPlayerBreak(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity) {
        if (PlayerBlockBreakEvents.BEFORE
                .invoker()
                .beforeBlockBreak(level, player, pos, state, blockEntity)) {
            return true;
        }
        PlayerBlockBreakEvents.CANCELED
                .invoker()
                .onBlockBreakCanceled(level, player, pos, state, blockEntity);
        return false;
    }

    public static boolean isFlammable(
            BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return ((FireBlock) Blocks.FIRE).getBurnOdds(state) > 0;
    }

    public static int burnOdds(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return ((FireBlock) Blocks.FIRE).getBurnOdds(state);
    }

    public static int igniteOdds(
            BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return ((FireBlock) Blocks.FIRE).getIgniteOdds(state);
    }

    public static boolean canHarvestBlock(
            BlockGetter level, BlockPos pos, BlockState state, Player player) {
        return player.hasCorrectToolForDrops(state);
    }

    public static @Nullable SpawnGroupData finalizeSpawn(
            Mob mob,
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        return mob.finalizeSpawn(level, difficulty, reason, data);
    }

    public static boolean canEntityGrief(ServerLevel level, Entity entity) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    public static boolean canEntityDestroyBlock(
            ServerLevel level, BlockPos pos, BlockState state, LivingEntity entity) {
        return true;
    }

    public static void updateNeighbourForOutputSignal(
            ServerLevel level, BlockPos pos, Block changed) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos relative = pos.relative(direction);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, relative);
            if (state == null) continue;
            if (!state.is(Blocks.COMPARATOR)) {
                if (!state.isRedstoneConductor(level, relative)) continue;
                relative = relative.relative(direction);
                state = ChunkUtil.blockStateIfLoaded(level, relative);
                if (state == null || !state.is(Blocks.COMPARATOR)) continue;
            }
            level.neighborChanged(state, relative, changed, null, false);
        }
    }

    public static void dropResources(
            ServerLevel level,
            BlockPos pos,
            BlockPos dropAt,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            @Nullable Entity breaker,
            ItemStack tool) {
        Block.getDrops(state, level, pos, blockEntity, breaker, tool)
                .forEach(stack -> Block.popResource(level, dropAt, stack));
        state.spawnAfterBreak(level, pos, tool, true);
    }

    public static JsonElement componentIngredient(
            String itemId, String componentId, JsonElement value) {
        JsonObject components = new JsonObject();
        components.add(componentId, value);
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("fabric:type", "fabric:components");
        ingredient.addProperty("base", itemId);
        ingredient.add("components", components);
        return ingredient;
    }

    public static JsonElement fluidContentIngredient(String fluidId, int amount) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("fabric:type", "hbm:fluid_content");
        ingredient.addProperty("fluid", fluidId);
        ingredient.addProperty("amount", amount);
        return ingredient;
    }

    public static boolean canFitInsideContainerItems(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems()
                && !stack.is(HbmItemTags.NO_CONTAINER_NESTING)
                && !stack.has(DataComponents.CONTAINER)
                && !stack.has(DataComponents.BUNDLE_CONTENTS)
                && ContainerItemContext.withConstant(stack).find(ItemStorage.ITEM) == null;
    }

    public record BlockMenu(MenuProvider provider, BlockMenuContext context)
            implements ExtendedMenuProvider<BlockMenuContext> {
        @Override
        public BlockMenuContext getScreenOpeningData(ServerPlayer player) {
            return context;
        }

        @Override
        public Component getDisplayName() {
            return provider.getDisplayName();
        }

        @Override
        public AbstractContainerMenu createMenu(
                int containerId, Inventory inventory, Player player) {
            return IGUIProvider.createDispatchedMenu(
                    provider, containerId, inventory, player, context.dispatchId());
        }
    }
}
