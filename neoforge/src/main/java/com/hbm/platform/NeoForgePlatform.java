// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm.inventory.BlockMenuContext;
import com.hbm.inventory.IGUIProvider;
import com.hbm.packet.toclient.GunSoundPayload;
import com.hbm.tags.HbmItemTags;
import com.hbm.util.ChunkUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import org.jspecify.annotations.Nullable;

public final class NeoForgePlatform {
    public static final Direction[] NEIGHBOR_UPDATE_LIST =
            Stream.concat(Direction.Plane.HORIZONTAL.stream(), Direction.Plane.VERTICAL.stream())
                    .toArray(Direction[]::new);

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static boolean allowPlayerBreak(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity) {
        return !NeoForge.EVENT_BUS
                .post(new BreakBlockEvent(level, pos, state, player))
                .isCanceled();
    }

    public static void afterPlayerBreak(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            @Nullable BlockEntity blockEntity) {}

    public static void playGunAnimationSound(
            LivingEntity entity, SoundEvent sound, float volume, float pitch, boolean legacy) {
        ServerLevel level = (ServerLevel) entity.level();
        long seed = level.soundSeedGenerator.nextLong();
        var event =
                EventHooks.onPlaySoundAtPosition(
                        level,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound),
                        SoundSource.PLAYERS,
                        volume,
                        pitch);
        if (event.isCanceled() || event.getSound() == null) return;
        GunSoundPayload.send(
                level,
                legacy,
                event.getSound(),
                event.getSource(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                event.getNewVolume(),
                event.getNewPitch(),
                seed);
    }

    public static void openMenu(
            ServerPlayer player, MenuProvider provider, BlockMenuContext context) {
        player.openMenu(
                new BlockMenu(provider, context),
                buf -> {
                    buf.writeBlockPos(context.pos());
                    buf.writeVarInt(context.dispatchId());
                });
    }

    public static boolean canHarvestBlock(
            BlockGetter level, BlockPos pos, BlockState state, Player player) {
        return state.canHarvestBlock(level, pos, player);
    }

    public static @Nullable SpawnGroupData finalizeSpawn(
            Mob mob,
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        return EventHooks.finalizeMobSpawn(mob, level, difficulty, reason, data);
    }

    public static TriState bonemealEvent(
            @Nullable Player player, Level level, BlockPos pos, BlockState state, ItemStack stack) {
        BonemealEvent event = EventHooks.fireBonemealEvent(player, level, pos, state, stack);
        return event.isCanceled() ? TriState.from(event.isSuccessful()) : TriState.DEFAULT;
    }

    public static boolean canEntityDestroyBlock(
            ServerLevel level, BlockPos pos, BlockState state, LivingEntity entity) {
        return state.canEntityDestroy(level, pos, entity)
                && EventHooks.onEntityDestroyBlock(entity, pos, state);
    }

    public static void updateNeighbourForOutputSignal(
            ServerLevel level, BlockPos pos, Block changed) {
        for (Direction direction : NEIGHBOR_UPDATE_LIST) {
            BlockPos relative = pos.relative(direction);
            BlockState state = ChunkUtil.blockStateIfLoaded(level, relative);
            if (state == null) continue;
            state.onNeighborChange(level, relative, pos);
            if (!state.isRedstoneConductor(level, relative)) continue;
            relative = relative.relative(direction);
            state = ChunkUtil.blockStateIfLoaded(level, relative);
            if (state != null
                    && state.getWeakChanges(level, relative)
                    && (direction.getAxis().isHorizontal() || !state.is(Blocks.COMPARATOR))) {
                level.neighborChanged(state, relative, changed, null, false);
            }
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
        List<ItemEntity> drops = new ArrayList<>();
        double halfHeight = EntityTypes.ITEM.getHeight() / 2.0;
        RandomSource random = level.getRandom();
        for (ItemStack stack : Block.getDrops(state, level, pos, blockEntity, breaker, tool)) {
            double x = dropAt.getX() + 0.5 + Mth.nextDouble(random, -0.25, 0.25);
            double y = dropAt.getY() + 0.5 + Mth.nextDouble(random, -0.25, 0.25) - halfHeight;
            double z = dropAt.getZ() + 0.5 + Mth.nextDouble(random, -0.25, 0.25);
            if (stack.isEmpty()
                    || !level.getGameRules().get(GameRules.BLOCK_DROPS)
                    || level.restoringBlockSnapshots) {
                continue;
            }
            ItemEntity entity = new ItemEntity(level, x, y, z, stack);
            entity.setDefaultPickUpDelay();
            drops.add(entity);
        }
        CommonHooks.handleBlockDrops(level, pos, state, blockEntity, drops, breaker, tool);
    }

    public static JsonElement componentIngredient(
            String itemId, String componentId, JsonElement value) {
        JsonObject components = new JsonObject();
        components.add(componentId, value);
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("neoforge:ingredient_type", "neoforge:components");
        ingredient.addProperty("items", itemId);
        ingredient.add("components", components);
        return ingredient;
    }

    public static JsonElement fluidContentIngredient(String fluidId, int amount) {
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("neoforge:ingredient_type", "hbm:fluid_content");
        ingredient.addProperty("fluid", fluidId);
        ingredient.addProperty("amount", amount);
        return ingredient;
    }

    public static boolean canFitInsideContainerItems(ItemStack stack) {
        return stack.canFitInsideContainerItems()
                && !stack.is(HbmItemTags.NO_CONTAINER_NESTING)
                && !stack.has(DataComponents.CONTAINER)
                && !stack.has(DataComponents.BUNDLE_CONTENTS)
                && (stack.isEmpty()
                        || stack.getCapability(Capabilities.Item.ITEM, ItemAccess.forStack(stack))
                                == null)
                && (!ModList.get().isLoaded("sophisticatedcore")
                        || !SophisticatedCoreNesting.hasStorage(stack));
    }

    public record BlockMenu(MenuProvider provider, BlockMenuContext context)
            implements MenuProvider {
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
