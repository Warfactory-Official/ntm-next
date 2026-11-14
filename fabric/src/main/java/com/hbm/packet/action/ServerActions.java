/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.packet.action;

import com.hbm.advancement.HbmCriteria;
import com.hbm.api.environment.Environment;
import com.hbm.api.fluidmk2.FluidPipeGraph;
import com.hbm.api.fluidmk2.FluidPipeTintData;
import com.hbm.api.fluidmk2.PipeData;
import com.hbm.blockentity.Synced;
import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blocks.network.FluidDuctBlock;
import com.hbm.entity.missile.BobmazonEntity;
import com.hbm.entity.train.RailCarBaseEntity;
import com.hbm.extprop.PlayerState;
import com.hbm.handler.BobmazonOffer;
import com.hbm.handler.BobmazonOffers;
import com.hbm.handler.CraneKey;
import com.hbm.handler.KeybindType;
import com.hbm.handler.ToolKey;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.handler.radiation.RadVisServer;
import com.hbm.inventory.container.AnvilMenu;
import com.hbm.inventory.container.HeldItemMenu;
import com.hbm.inventory.container.ModMenus;
import com.hbm.inventory.container.PneumoStorageAccessMenu;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilConstructionRecipes;
import com.hbm.items.KeybindHandler;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ArmorFlight;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.tool.DesignatorManualItem;
import com.hbm.items.tool.RTTYPagerItem;
import com.hbm.items.tool.SatelliteInterfaceItem;
import com.hbm.items.tool.ToolAbilityItem;
import com.hbm.packet.ChunkTrackerIndex;
import com.hbm.packet.ControlAuthority;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.PlayerInformLayout;
import com.hbm.packet.layout.RBMKDiagnosticLayout;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.util.ChunkUtil;
import com.hbm.util.DropHeight;
import com.hbm.util.InventoryUtil;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

import static com.hbm.lib.internal.UnsafeHolder.U;

public final class ServerActions {

    private static final int ID_HUD = 7;
    private static final int ID_MAGNET = 6;
    private static final int ID_JETPACK = 5;
    private static final Map<ServerPlayer, SentDiagnostics> SENT_DIAGNOSTICS = new WeakHashMap<>();

    private ServerActions() {}

    public static void armorFlight(
            ServerPlayer player, ArmorFlight.Mode mode, boolean euphemiumFallCapped) {
        PlayerState props = PlayerState.getOrCreate(player);
        props.flightIntent = mode.ordinal();
        props.fallCapped = euphemiumFallCapped;
    }

    public static void anvilCraft(
            ServerPlayer player, int containerId, String recipeName, int mode) {
        if (player.isSpectator()) return;
        if (mode < 0 || mode > 1) return;
        AnvilConstructionRecipe recipe = AnvilConstructionRecipes.INSTANCE.getRecipe(recipeName);
        if (recipe == null
                || !(ControlAuthority.openMenu(player, containerId) instanceof AnvilMenu anvil)
                || !recipe.isTierValid(anvil.tier)) return;

        List<AnvilOutput> outputs = recipe.outputs();
        int count =
                mode != 1
                        ? 1
                        : outputs.size() > 1
                                ? 64
                                : outputs.getFirst().stack().getMaxStackSize()
                                        / outputs.getFirst().stack().getCount();
        for (int i = 0; i < count; i++) {
            if (!InventoryUtil.consumeIngredients(player, recipe.input())) break;
            InventoryUtil.giveChanceOutputs(player, outputs);
            HbmCriteria.itemCrafted(player, outputs.getFirst().stack());
        }
        anvil.broadcastFullState();
    }

    public static void bobmazonPurchase(ServerPlayer player, int containerId, int offerIndex) {
        HeldItemMenu menu = HeldItemMenu.current(player, containerId, ModMenus.CATALOG);
        if (menu == null) return;
        ServerLevel level = player.level();
        Component refusal = Environment.deliveryRefusal(level);
        if (refusal != null) {
            player.sendSystemMessage(refusal);
            return;
        }

        List<BobmazonOffer> catalog = BobmazonOffers.forStack(menu.stack());
        if (offerIndex < 0 || offerIndex >= catalog.size()) return;

        BobmazonOffer offer = catalog.get(offerIndex);
        boolean creative = player.getAbilities().instabuild;
        if (!offer.requirement().admits(player) && !creative) {
            player.sendSystemMessage(
                    Component.translatable("chat.bobmazonPurchase.achievementRequirementNot"));
            return;
        }
        if (countCaps(player) < offer.cost() && !creative) {
            player.sendSystemMessage(Component.translatable("chat.bobmazonPurchase.notEnoughCaps"));
            return;
        }

        payCaps(player, offer.cost());
        player.inventoryMenu.broadcastChanges();
        RandomSource random = level.getRandom();
        BobmazonEntity bob = new BobmazonEntity(level);
        double dropX = player.getX() + random.nextGaussian() * 10;
        double dropZ = player.getZ() + random.nextGaussian() * 10;
        bob.setPos(dropX, DropHeight.clearOfTerrain(level, 300, dropX, dropZ), dropZ);
        bob.payload = offer.offer().copy();
        level.addFreshEntity(bob);
    }

    public static void containedStorage(
            ServerPlayer player, int rootContainer, int rootSlot, UUID target, boolean backpack) {}

    public static void crateOpenHeld(ServerPlayer player, boolean openHeld) {
        PlayerState.getOrCreate(player).crateOpenHeld = openHeld;
    }

    public static void craneKey(ServerPlayer player, CraneKey key, boolean pressed) {
        PlayerState.getOrCreate(player).setHeld(key, pressed);
    }

    public static void designatorCoordinates(
            ServerPlayer player, int containerId, int operator, int value, int reference) {
        if (operator < DesignatorManualItem.ADD
                || operator > DesignatorManualItem.SET_HERE
                || reference < DesignatorManualItem.REFERENCE_X
                || reference > DesignatorManualItem.REFERENCE_Z) return;
        HeldItemMenu menu = HeldItemMenu.current(player, containerId, ModMenus.DESIGNATOR_MANUAL);
        if (menu == null) return;
        ItemStack stack = menu.stack();
        long target = stack.getOrDefault(ModDataComponents.TARGET_DESIGNATOR.get(), 0L);
        stack.set(
                ModDataComponents.TARGET_DESIGNATOR.get(),
                DesignatorManualItem.apply(
                        target, operator, value, reference, player.getX(), player.getZ()));
    }

    public static void farView(ServerPlayer player, int blocks) {
        ((com.hbm.interfaces.injected.FarViewer) (player.connection))
                .hbm$farView(Math.max(blocks, 0));
    }

    public static void fluidIdentifier(
            ServerPlayer player, int containerId, boolean primary, Identifier fluidId) {
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(fluidId).orElse(null);
        HeldItemMenu menu = HeldItemMenu.current(player, containerId, ModMenus.FLUID_IDENTIFIER);
        if (fluid == null || menu == null) return;
        ItemStack stack = menu.stack();
        FluidIdentifierData data =
                stack.getOrDefault(
                        ModDataComponents.FLUID_IDENTIFIER.get(), FluidIdentifierData.EMPTY);
        stack.set(
                ModDataComponents.FLUID_IDENTIFIER.get(),
                primary ? data.withPrimary(fluid) : data.withSecondary(fluid));
    }

    public static void fluidPipeTintRequest(
            ServerPlayer player, @Nullable Object storage, long address, int count) {
        ServerLevel level = player.level();
        Reference2ObjectOpenHashMap<Fluid, LongArrayList> byFluid =
                new Reference2ObjectOpenHashMap<>();
        ChunkTrackingView view = player.getChunkTrackingView();
        for (int index = 0; index < count; index++) {
            long key = U.getLong(storage, address + 8L * index);
            BlockPos pos = BlockPos.of(key);
            if (!view.contains(ChunkPos.containing(pos))) continue;
            BlockState state = ChunkUtil.blockStateIfLoaded(level, pos);
            if (state == null || !(state.getBlock() instanceof FluidDuctBlock)) continue;
            PipeData data = FluidPipeGraph.dataAt(level, key);
            if (data == null) continue;
            byFluid.computeIfAbsent(data.fluid(), fluid -> new LongArrayList()).add(key);
        }
        for (Reference2ObjectMap.Entry<Fluid, LongArrayList> entry :
                byFluid.reference2ObjectEntrySet()) {
            FluidPipeTintData.syncTo(player, entry.getValue(), entry.getKey());
        }
    }

    public static void satelliteCoordinates(
            ServerPlayer player, int containerId, int x, int z, boolean yPresent, int y) {
        HeldItemMenu menu = HeldItemMenu.current(player, containerId, ModMenus.SATELLITE_INTERFACE);
        if (menu == null) return;
        ItemStack stack = menu.stack();
        if (stack.getItem() instanceof SatelliteInterfaceItem receiver) {
            receiver.setCoordinates(player, stack, x, yPresent ? y : Satellite.SURFACE, z);
        }
    }

    public static void keybind(ServerPlayer player, KeybindType key, boolean pressed) {
        PlayerState props = PlayerState.getOrCreate(player);
        if (pressed && !props.held(key) && key == KeybindType.TOGGLE_HUD) {
            props.enableHUD = !props.enableHUD;
            PacketWire.sendTo(
                    PlayerInformLayout.encode(
                            toggled(props.enableHUD, "chat.toggle.hudOn", "chat.toggle.hudOff"),
                            ID_HUD,
                            1_000),
                    player);
        }
        if (pressed && !props.held(key) && key == KeybindType.TOGGLE_JETPACK) {
            props.enableBackpack = !props.enableBackpack;
            PacketWire.sendTo(
                    PlayerInformLayout.encode(
                            toggled(
                                    props.enableBackpack,
                                    "chat.toggle.jetpackOn",
                                    "chat.toggle.jetpackOff"),
                            ID_JETPACK,
                            1_000),
                    player);
        }
        if (pressed && !props.held(key) && key == KeybindType.TOGGLE_MAGNET) {
            props.enableMagnet = !props.enableMagnet;
            PacketWire.sendTo(
                    PlayerInformLayout.encode(
                            toggled(
                                    props.enableMagnet,
                                    "chat.toggle.magnetOn",
                                    "chat.toggle.magnetOff"),
                            ID_MAGNET,
                            1_000),
                    player);
        }
        if (pressed
                && !props.held(key)
                && key == KeybindType.TRAIN_INVENTORY
                && player.getVehicle() instanceof RailCarBaseEntity car
                && car instanceof MenuProvider menu) player.openMenu(menu);
        props.setHeld(key, pressed && !player.isSpectator());
        if (player.isSpectator()) return;

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()
                && held.getItem() instanceof KeybindHandler receiver
                && receiver.canHandleKeybind(player, held, key)) {
            receiver.onKeyChanged(player, held, key, pressed);
        }
    }

    public static void pneumoAccessClick(
            ServerPlayer player, int containerId, PneumoStorageAccessMenu.ClickOp op, int entryId) {
        if (player.isSpectator()
                || !(ControlAuthority.openMenu(player, containerId)
                        instanceof PneumoStorageAccessMenu menu)) return;
        menu.handleClick(op, entryId);
    }

    public static void radiationWatch(ServerPlayer player, int radius) {
        if (radius < Byte.MIN_VALUE || radius > Byte.MAX_VALUE) return;
        RadVisServer.watch(player, radius);
    }

    public static void rbmkDiagnostics(ServerPlayer player, BlockPos pos) {
        if (!ChunkTrackerIndex.canRepair(player, ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4)))
            return;
        if (!(ChunkUtil.blockEntityIfLoaded(player.level(), pos)
                instanceof RBMKBaseBlockEntity column)) return;
        CompoundTag diagnostics = new CompoundTag();
        column.writeDiagnostics(diagnostics);
        SentDiagnostics sent = new SentDiagnostics(player.level().dimension(), pos, diagnostics);
        if (sent.equals(SENT_DIAGNOSTICS.put(player, sent))) return;
        PacketWire.sendTo(RBMKDiagnosticLayout.encode(pos, diagnostics), player);
    }

    public static void rttyPagerChannel(ServerPlayer player, int containerId, String channel) {
        HeldItemMenu menu = HeldItemMenu.current(player, containerId, ModMenus.RTTY_PAGER);
        if (menu == null || channel.length() > RTTYPagerItem.MAX_CHANNEL_LENGTH) return;
        RTTYPagerItem.setChannel(menu.stack(), channel);
    }

    public static void syncRequest(ServerPlayer player, BlockPos pos) {
        if (!ChunkTrackerIndex.canRepair(player, ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4)))
            return;
        if (ChunkUtil.blockEntityIfLoaded(player.level(), pos) instanceof Synced synced) {
            synced.syncTo(player);
        }
    }

    public static void toolKey(ServerPlayer player, ToolKey key, boolean pressed) {
        PlayerState.getOrCreate(player).setHeld(key, pressed);
    }

    public static void toolPresets(
            ServerPlayer player, int containerId, List<ToolPreset> presets, int current) {
        HeldItemMenu menu = HeldItemMenu.current(player, containerId, ModMenus.TOOL_PRESETS);
        if (menu == null) return;
        ItemStack held = menu.stack();
        if (held.getItem() instanceof ToolAbilityItem tool) {
            tool.setPresets(held, presets, current);
        }
    }

    private static int countCaps(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int count = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (isCap(stack.getItem())) count += stack.getCount();
        }
        return count;
    }

    private static void payCaps(ServerPlayer player, int price) {
        if (price == 0) return;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.INVENTORY_SIZE && price > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!isCap(stack.getItem())) continue;
            int taken = Math.min(price, stack.getCount());
            inventory.removeItem(i, taken);
            price -= taken;
        }
    }

    private static boolean isCap(Item item) {
        return item == ModItems.CAP_FRITZ.get()
                || item == ModItems.CAP_KORL.get()
                || item == ModItems.CAP_NUKA.get()
                || item == ModItems.CAP_QUANTUM.get()
                || item == ModItems.CAP_RAD.get()
                || item == ModItems.CAP_SPARKLE.get();
    }

    private static Component toggled(boolean on, String onKey, String offKey) {
        return Component.translatable(on ? onKey : offKey)
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private record SentDiagnostics(
            ResourceKey<Level> dimension, BlockPos pos, CompoundTag diagnostics) {}
}
