// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform.services;

import com.google.gson.JsonElement;
import com.hbm.inventory.BlockMenuContext;
import com.hbm.packet.toclient.GunSoundPayload;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.TriState;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface IPlatformHelper {
    String getPlatformName();

    boolean isPhysicalClient();

    boolean isModLoaded(String modId);

    boolean hasDifferentLightProperties(
            BlockGetter level, BlockPos pos, BlockState oldState, BlockState newState);

    void openMenu(ServerPlayer player, MenuProvider provider, BlockMenuContext context);

    default void playGunAnimationSound(
            LivingEntity entity, SoundEvent sound, float volume, float pitch, boolean legacy) {
        GunSoundPayload.send(
                (ServerLevel) entity.level(),
                legacy,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                SoundSource.PLAYERS,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                volume,
                pitch,
                entity.level().soundSeedGenerator.nextLong());
    }

    boolean allowPlayerBreak(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity);

    void afterPlayerBreak(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity);

    boolean isFakePlayer(Player player);

    float biomeDownfall(Biome biome);

    boolean isFlammable(BlockGetter level, BlockPos pos, BlockState state, Direction face);

    int burnOdds(BlockGetter level, BlockPos pos, BlockState state, Direction face);

    int igniteOdds(BlockGetter level, BlockPos pos, BlockState state, Direction face);

    void setFlammable(Block block, int encouragement, int flammability);

    boolean canHarvestBlock(BlockGetter level, BlockPos pos, BlockState state, Player player);

    float explosionResistance(
            BlockGetter level, BlockPos pos, BlockState state, Explosion explosion);

    @Nullable SpawnGroupData finalizeSpawn(
            Mob mob,
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData data);

    TriState bonemealEvent(
            @Nullable Player player, Level level, BlockPos pos, BlockState state, ItemStack stack);

    boolean canEntityGrief(ServerLevel level, Entity entity);

    boolean canEntityDestroyBlock(
            ServerLevel level, BlockPos pos, BlockState state, LivingEntity entity);

    void updateNeighbourForOutputSignal(ServerLevel level, BlockPos pos, Block changed);

    void dropResources(
            ServerLevel level,
            BlockPos pos,
            BlockPos dropAt,
            BlockState state,
            @Nullable BlockEntity blockEntity,
            @Nullable Entity breaker,
            ItemStack tool);

    boolean canDropFromExplosion(
            BlockGetter level, BlockPos pos, BlockState state, Explosion explosion);

    int burnTime(ItemStack stack, FuelValues fuelValues);

    JsonElement componentIngredient(String itemId, String componentId, JsonElement value);

    Ingredient exactIngredient(ItemStack stack);

    Ingredient anyIngredient(List<Ingredient> children);

    JsonElement fluidContentIngredient(String fluidId, int amount);

    void itemTooltipEvent(
            ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines);

    void setItemLifespan(ItemEntity item, int ticks);

    boolean canFitInsideContainerItems(ItemStack stack);
}
