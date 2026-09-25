// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.inventory.BlockMenuContext;
import com.hbm.inventory.IGUIProvider;
import com.hbm.platform.services.IPlatformHelper;
import com.hbm.util.ChunkUtil;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.lighting.LightEngine;
import org.jspecify.annotations.Nullable;

public final class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean hasDifferentLightProperties(
            BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState) {
        return LightEngine.hasDifferentLightProperties(oldState, newState);
    }

    @Override
    public boolean allowPlayerBreak(
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

    @Override
    public void afterPlayerBreak(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity) {
        PlayerBlockBreakEvents.AFTER
                .invoker()
                .afterBlockBreak(level, player, pos, state, blockEntity);
    }

    @Override
    public boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, BlockMenuContext context) {
        player.openMenu(
                new ExtendedMenuProvider<BlockMenuContext>() {
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
                });
    }

    @Override
    public float biomeDownfall(Biome biome) {

        return biome.climateSettings.downfall();
    }

    @Override
    public boolean isFlammable(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return burnOdds(level, pos, state, face) > 0;
    }

    @Override
    public int burnOdds(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return ((FireBlock) Blocks.FIRE).getBurnOdds(state);
    }

    @Override
    public int igniteOdds(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        return ((FireBlock) Blocks.FIRE).getIgniteOdds(state);
    }

    @Override
    public boolean canHarvestBlock(
            BlockGetter level, BlockPos pos, BlockState state, Player player) {
        return player.hasCorrectToolForDrops(state);
    }

    @Override
    public float explosionResistance(
            BlockGetter level, BlockPos pos, BlockState state, Explosion explosion) {
        return state.getBlock().getExplosionResistance();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            Mob mob,
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        return mob.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public TriState bonemealEvent(
            @Nullable Player player, Level level, BlockPos pos, BlockState state, ItemStack stack) {
        return TriState.DEFAULT;
    }

    @Override
    public boolean canEntityGrief(ServerLevel level, Entity entity) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    @Override
    public boolean canEntityDestroyBlock(
            ServerLevel level, BlockPos pos, BlockState state, LivingEntity entity) {
        return true;
    }

    @Override
    public void updateNeighbourForOutputSignal(ServerLevel level, BlockPos pos, Block changed) {
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

    @Override
    public void dropResources(
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

    @Override
    public boolean canDropFromExplosion(
            BlockGetter level, BlockPos pos, BlockState state, Explosion explosion) {
        return state.getBlock().dropFromExplosion(explosion);
    }

    @Override
    public void setFlammable(Block block, int encouragement, int flammability) {

        FlammableBlockRegistry.getDefaultInstance().add(block, encouragement, flammability);
    }

    @Override
    public int burnTime(ItemStack stack, FuelValues fuelValues) {
        return fuelValues.burnDuration(stack);
    }

    @Override
    public JsonElement componentIngredient(String itemId, String componentId, JsonElement value) {

        JsonObject components = new JsonObject();
        components.add(componentId, value);
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("fabric:type", "fabric:components");
        ingredient.addProperty("base", itemId);
        ingredient.add("components", components);
        return ingredient;
    }

    @Override
    public Ingredient exactIngredient(ItemStack stack) {
        return DefaultCustomIngredients.components(stack);
    }

    @Override
    public Ingredient anyIngredient(List<Ingredient> children) {
        return DefaultCustomIngredients.any(children.toArray(Ingredient[]::new));
    }

    @Override
    public JsonElement fluidContentIngredient(String fluidId, int amount) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("fabric:type", "hbm:fluid_content");
        ingredient.addProperty("fluid", fluidId);
        ingredient.addProperty("amount", amount);
        return ingredient;
    }

    @Override
    public void itemTooltipEvent(
            ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        ItemTooltipCallback.EVENT.invoker().getTooltip(stack, context, flag, lines);
    }

    @Override
    public void setItemLifespan(ItemEntity item, int ticks) {
        item.age = ItemEntity.LIFETIME - ticks;
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems();
    }
}
