// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.hazard;

import com.hbm.config.RadiationConfig;
import com.hbm.data.RadiationData;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.transformer.IHazardTransformer;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.hazard.type.IHazardType;
import com.hbm.items.ModDataComponents;
import com.hbm.platform.Services;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public final class HazardSystem {

    public static volatile Map<TagKey<Item>, HazardData> tagMap = new HashMap<>();
    public static volatile Map<Item, HazardData> itemMap = new HashMap<>();
    public static final HashSet<Item> itemBlacklist = new HashSet<>();
    public static final HashSet<TagKey<Item>> tagBlacklist = new HashSet<>();

    private static IHazardTransformer[] transformers = new IHazardTransformer[0];
    private static boolean liveStorageTransformers;
    private static ExternalEquipmentApplicator[] externalEquipment =
            new ExternalEquipmentApplicator[0];

    @FunctionalInterface
    public interface ExternalEquipmentApplicator {

        float apply(Player player);
    }

    private static final double MIN_RAD_RATE = 0.000005D;

    private static final ConcurrentHashMap<Item, HazardData[]> chronologyCache =
            new ConcurrentHashMap<>();

    private static final HazardData[] NO_HAZARD_DATA = new HazardData[0];

    public static final int ACTIVATION_WRITE_PERIOD = 20;

    private static final int TRACKED_SLOTS = Inventory.SLOT_OFFHAND + 1;

    private static final int OFFLINE_SWEEP_PERIOD = 100;

    private static final HazardEntry[] NO_ENTRIES = new HazardEntry[0];

    private static final Applicator[] EMPTY_MEMO = new Applicator[0];

    private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();

    private static final ConcurrentHashMap<UUID, PlayerHazardData> playerData =
            new ConcurrentHashMap<>();
    private static final Queue<InventoryDelta> inventoryDeltas = new ConcurrentLinkedQueue<>();
    private static volatile boolean cachesDirty;

    private static Map<Item, HazardData> codeItems = Map.of();
    private static Map<TagKey<Item>, HazardData> codeTags = Map.of();
    private static final Executor EXECUTOR = ForkJoinPool.commonPool();
    private static volatile CompletableFuture<Void> scanFuture =
            CompletableFuture.completedFuture(null);

    private HazardSystem() {}

    public static void init() {
        Services.SERVER.onServerTickPost(HazardSystem::onServerTick);
    }

    public static void registerContent() {
        HazardRegistry.registerTrafos();
        HazardRegistry.registerItems();
        codeItems = Map.copyOf(itemMap);
        codeTags = Map.copyOf(tagMap);
    }

    public static void applyDataPack(RegistryAccess registries) {
        Map<Item, HazardData> items = new HashMap<>(codeItems);
        Map<TagKey<Item>, HazardData> tags = new HashMap<>(codeTags);

        registries
                .lookup(HazardAssignment.REGISTRY)
                .ifPresent(
                        registry -> {
                            Map<Item, Identifier> itemBindings = new HashMap<>();
                            Map<TagKey<Item>, Identifier> tagBindings = new HashMap<>();
                            for (var holder : registry.listElements().toList()) {
                                HazardAssignment assignment = holder.value();
                                Identifier id = holder.key().identifier();
                                HazardData data = assignment.toData();

                                assignment
                                        .targets()
                                        .unwrap()
                                        .ifLeft(
                                                tag -> {
                                                    claimBinding(tagBindings, tag, id);
                                                    tags.put(tag, data);
                                                })
                                        .ifRight(
                                                members -> {
                                                    for (Holder<Item> member : members) {
                                                        claimBinding(
                                                                itemBindings, member.value(), id);
                                                        items.put(member.value(), data);
                                                    }
                                                });
                            }
                        });

        tagMap = tags;
        itemMap = items;
        clearCaches();
    }

    private static <T> void claimBinding(Map<T, Identifier> bindings, T target, Identifier id) {
        Identifier prior = bindings.putIfAbsent(target, id);
        if (prior != null) {
            throw new IllegalStateException(
                    "Duplicate hazard binding for " + target + ": " + prior + " and " + id);
        }
    }

    public static void addTransformer(IHazardTransformer transformer) {
        transformers = Arrays.copyOf(transformers, transformers.length + 1);
        transformers[transformers.length - 1] = transformer;
        if (transformer.readsLiveStorage()) liveStorageTransformers = true;
    }

    public static void addExternalEquipmentApplicator(ExternalEquipmentApplicator applicator) {
        externalEquipment = Arrays.copyOf(externalEquipment, externalEquipment.length + 1);
        externalEquipment[externalEquipment.length - 1] = applicator;
    }

    public static float applyExternalEquipment(ItemStack stack, Player player) {
        if (stack.isEmpty()) return 0F;
        List<HazardEntry> hazards = getHazardsFromStack(stack);
        for (HazardEntry entry : hazards) entry.applyHazard(stack, player);
        return activationOf(stack, hazards);
    }

    public static void register(TagKey<Item> tag, HazardData data) {
        tagMap.put(tag, data);
    }

    public static void register(Item item, HazardData data) {
        itemMap.put(item, data);
    }

    public static void register(Block block, HazardData data) {
        itemMap.put(block.asItem(), data);
    }

    public static void register(ItemStack stack, HazardData data) {
        if (!stack.isEmpty()) itemMap.put(stack.getItem(), data);
    }

    public static void blacklist(Item item) {
        itemBlacklist.add(item);
    }

    public static void blacklist(TagKey<Item> tag) {
        tagBlacklist.add(tag);
    }

    public static boolean isItemBlacklisted(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!itemBlacklist.isEmpty() && itemBlacklist.contains(stack.getItem())) return true;
        if (!tagBlacklist.isEmpty()) {
            Iterator<TagKey<Item>> it =
                    BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).tags().iterator();
            while (it.hasNext()) {
                if (tagBlacklist.contains(it.next())) return true;
            }
        }
        return false;
    }

    public static List<HazardEntry> getHazardsFromStack(ItemStack stack) {
        if (stack.isEmpty() || isItemBlacklisted(stack)) return Collections.emptyList();
        return computeHazards(stack, stack.getItem());
    }

    private static List<HazardEntry> computeHazards(ItemStack stack, Item item) {
        HazardData[] chronological = chronologyCache.get(item);
        if (chronological == null)
            chronological = chronologyCache.computeIfAbsent(item, HazardSystem::chronologyFor);
        List<HazardEntry> entries = null;

        if (chronological.length > 0) {
            entries = new ArrayList<>();
            int mutex = 0;
            for (HazardData data : chronological) {
                if ((data.getMutex() & mutex) == 0) {
                    entries.addAll(data.entries);
                    mutex |= data.getMutex();
                }
            }
        }

        for (IHazardTransformer t : transformers) {
            if (!t.appliesTo(stack)) continue;
            if (entries == null) entries = new ArrayList<>(2);
            t.transform(stack, entries);
        }
        return entries == null ? Collections.emptyList() : entries;
    }

    private static HazardData[] chronologyFor(Item item) {
        List<Sourced> data = new ArrayList<>();
        Iterator<TagKey<Item>> it = BuiltInRegistries.ITEM.wrapAsHolder(item).tags().iterator();
        while (it.hasNext()) {
            TagKey<Item> tag = it.next();
            HazardData d = tagMap.get(tag);
            if (d != null) data.add(new Sourced(d, false, tag.location().toString()));
        }
        HazardData itemData = itemMap.get(item);
        if (itemData != null) {
            data.add(new Sourced(itemData, true, BuiltInRegistries.ITEM.getKey(item).toString()));
        }
        if (data.size() > 1) data.sort(Sourced.ORDER);
        HazardData[] out = new HazardData[data.size()];
        for (int i = 0; i < out.length; i++) out[i] = data.get(i).data();
        return out;
    }

    private record Sourced(HazardData data, boolean direct, String name) {

        static final Comparator<Sourced> ORDER =
                Comparator.comparingInt((Sourced s) -> s.data().priority)
                        .thenComparing(Sourced::direct)
                        .thenComparing(Sourced::name);
    }

    public static boolean isStackHazardous(ItemStack stack) {
        return !stack.isEmpty() && !getHazardsFromStack(stack).isEmpty();
    }

    public static double getHazardLevelFromStack(ItemStack stack, IHazardType hazard) {
        for (HazardEntry e : getHazardsFromStack(stack)) {
            if (e.type == hazard)
                return IHazardModifier.evalAllModifiers(stack, null, e.baseLevel, e.getMods());
        }
        return 0D;
    }

    public static double getRawRadsFromStack(ItemStack stack) {
        double total = 0D;
        for (HazardEntry e : getHazardsFromStack(stack)) {
            if (e.type == HazardRegistry.RADIATION) {
                total += IHazardModifier.evalAllModifiers(stack, null, e.baseLevel, e.getMods());
            }
        }
        return total;
    }

    public static void addHazardInfo(
            ItemStack stack, @Nullable Player player, List<Component> list, TooltipFlag flag) {
        for (HazardEntry hazard : getHazardsFromStack(stack)) {
            hazard.type.addHazardInformation(
                    player, list, hazard.baseLevel, stack, hazard.getMods());
        }
        float activation = ContaminationUtil.getNeutronRads(stack);
        if (activation > 0F) {
            HazardTypeRadiation.appendRadiationLines(
                    list, activation / stack.getCount(), stack.getCount());
        }
    }

    public static double getRawRadsFromBlock(Block block) {
        return getHazardLevelFromStack(new ItemStack(block.asItem()), HazardRegistry.RADIATION);
    }

    public static void applyHazards(ItemStack stack, LivingEntity entity) {
        if (stack.isEmpty()) return;
        for (HazardEntry e : getHazardsFromStack(stack)) e.applyHazard(stack, entity);
    }

    public static void applyHazards(Block block, LivingEntity entity) {
        applyHazards(new ItemStack(block.asItem()), entity);
    }

    public static void updateDroppedItem(ItemEntity entity) {
        if (entity.level().isClientSide() || entity.isRemoved()) return;
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) return;
        for (HazardEntry e : getHazardsFromStack(stack)) {
            e.type.updateEntity(
                    entity,
                    IHazardModifier.evalAllModifiers(stack, null, e.baseLevel, e.getMods()));
        }
    }

    public static void clearCaches() {
        if (Services.SERVER.getCurrentServer() == null) {
            chronologyCache.clear();
            return;
        }
        cachesDirty = true;
    }

    public static void onSlotChanged(ServerPlayer player, Slot slot, ItemStack stack) {

        if (slot.container != player.getInventory()) return;
        enqueue(player, slot.getContainerSlot(), stack);
    }

    private static void enqueue(ServerPlayer player, int slot, ItemStack stack) {
        if (slot < 0 || slot >= TRACKED_SLOTS) return;
        UUID id = player.getUUID();

        PlayerHazardData phd = playerData.get(id);
        if (phd == null) phd = playerData.computeIfAbsent(id, u -> new PlayerHazardData(player));
        phd.player = player;
        inventoryDeltas.add(new InventoryDelta(id, slot, stack, phd));
    }

    private static void onServerTick(MinecraftServer server) {
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        if (playerData.size() > players.size()
                || server.getTickCount() % OFFLINE_SWEEP_PERIOD == 0) {
            forgetOfflinePlayers(server);
        }

        for (ServerPlayer player : players) {
            if (!TickPhase.every(player, RadiationConfig.hazardRate) || player.isRemoved())
                continue;
            PlayerHazardData phd = playerData.get(player.getUUID());
            if (phd == null) {
                if (externalEquipment.length == 0) continue;
                phd =
                        playerData.computeIfAbsent(
                                player.getUUID(), u -> new PlayerHazardData(player));
            }
            phd.player = player;
            phd.applyActiveHazards();
        }

        if (!scanFuture.isDone()) return;
        if (!cachesDirty && inventoryDeltas.isEmpty()) return;
        dispatchScan(server);
    }

    private static void forgetOfflinePlayers(MinecraftServer server) {
        Iterator<UUID> it = playerData.keySet().iterator();
        while (it.hasNext()) {
            if (server.getPlayerList().getPlayer(it.next()) == null) it.remove();
        }
    }

    public static void tickDroppedItem(ItemEntity entity) {
        if (!TickPhase.every(entity, Services.CONFIG.runtime().itemHazardDropTickrate())) return;
        updateDroppedItem(entity);
    }

    public static void tickMobEquipment(LivingEntity entity) {
        if (entity.level().isClientSide() || entity instanceof Player) return;
        if (!TickPhase.every(entity, RadiationConfig.hazardRate)) return;
        for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
            applyHazards(entity.getItemBySlot(slot), entity);
        }
    }

    private static void dispatchScan(MinecraftServer server) {
        if (cachesDirty) {
            cachesDirty = false;
            chronologyCache.clear();
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                requeueWholeInventory(player);
        }
        List<InventoryDelta> deltas = new ArrayList<>();
        InventoryDelta delta;
        while ((delta = inventoryDeltas.poll()) != null) deltas.add(delta);
        if (deltas.isEmpty()) return;

        scanFuture = CompletableFuture.runAsync(() -> resolve(deltas), EXECUTOR);
    }

    private static void requeueWholeInventory(ServerPlayer player) {
        PlayerHazardData phd = playerData.get(player.getUUID());
        if (phd == null) return;
        Inventory inv = player.getInventory();
        for (int i = 0; i <= Inventory.SLOT_OFFHAND; i++) {
            inventoryDeltas.add(
                    new InventoryDelta(player.getUUID(), i, inv.getItem(i).copy(), phd));
        }
    }

    private static void resolve(List<InventoryDelta> deltas) {
        for (InventoryDelta d : deltas) {
            PlayerHazardData phd = playerData.get(d.uuid());
            if (phd == null || phd != d.phd()) continue;
            ItemStack ns = d.newStack();
            boolean liveStorage = readsLiveStorage(ns);
            List<HazardEntry> h = liveStorage ? Collections.emptyList() : getHazardsFromStack(ns);
            float activation = activationOf(ns, h);
            if (h.isEmpty() && activation == 0F && !liveStorage) phd.removeApplicator(d.slot());
            else
                phd.setApplicator(
                        d.slot(),
                        new Applicator(
                                ns.getItem(), h.toArray(NO_ENTRIES), activation, liveStorage));
        }
    }

    private static boolean readsLiveStorage(ItemStack stack) {
        if (!liveStorageTransformers || stack.isEmpty() || isItemBlacklisted(stack)) return false;
        for (IHazardTransformer transformer : transformers) {
            if (transformer.readsLiveStorage() && transformer.appliesTo(stack)) return true;
        }
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return false;
        for (ItemStackTemplate template : contents.nonEmptyItems()) {
            if (readsLiveStorage(template.create())) return true;
        }
        return false;
    }

    private static float activationOf(ItemStack stack, List<HazardEntry> hazards) {
        Float stored = stack.get(ModDataComponents.NEUTRON_ACTIVATION.get());
        if (stored == null || stored <= 0F) return 0F;
        for (HazardEntry e : hazards) {
            if (e.type == HazardRegistry.RADIATION
                    && IHazardModifier.evalAllModifiers(stack, null, e.baseLevel, e.getMods())
                            > 0D) {
                return 0F;
            }
        }
        return stored * stack.getCount();
    }

    private record Applicator(
            Item item, HazardEntry[] entries, float activation, boolean liveStorage) {}

    private record InventoryDelta(
            UUID uuid, int slot, ItemStack newStack, @Nullable PlayerHazardData phd) {}

    public record MenuWatcher(ServerPlayer player) implements ContainerListener {

        @Override
        public void slotChanged(AbstractContainerMenu menu, int slotIndex, ItemStack stack) {
            onSlotChanged(player, menu.getSlot(slotIndex), stack);
        }

        @Override
        public void dataChanged(AbstractContainerMenu menu, int id, int value) {}
    }

    private static final class PlayerHazardData {
        Player player;

        private volatile @Nullable Applicator[] applicators = EMPTY_MEMO;
        private volatile float carriedActivation;
        private double owedActivation;

        PlayerHazardData(Player player) {
            this.player = player;
        }

        void setApplicator(int slot, Applicator app) {
            Applicator[] next = Arrays.copyOf(applicators, TRACKED_SLOTS);
            next[slot] = app;
            publish(next);
        }

        void removeApplicator(int slot) {
            Applicator[] current = applicators;
            if (slot >= current.length || current[slot] == null) return;
            Applicator[] next = Arrays.copyOf(current, TRACKED_SLOTS);
            next[slot] = null;
            publish(next);
        }

        private void publish(Applicator[] next) {
            float carried = 0F;
            for (Applicator a : next) if (a != null) carried += a.activation();
            applicators = next;
            carriedActivation = carried;
        }

        void applyActiveHazards() {
            if (player.isRemoved()) return;
            pollWhileForeignMenuOpen();
            Applicator[] memo = applicators;
            Inventory inv = player.getInventory();
            float carried = carriedActivation;
            for (int slot = 0; slot < memo.length; slot++) {
                Applicator app = memo[slot];
                if (app == null) continue;
                ItemStack live = inv.getItem(slot);
                if (live.isEmpty() || live.getItem() != app.item()) continue;
                if (app.liveStorage()) {
                    List<HazardEntry> hazards = getHazardsFromStack(live);
                    carried += activationOf(live, hazards) - app.activation();
                    for (HazardEntry entry : hazards) entry.applyHazard(live, player);
                } else {
                    for (HazardEntry entry : app.entries()) entry.applyHazard(live, player);
                }
            }
            for (ExternalEquipmentApplicator applicator : externalEquipment)
                carried += applicator.apply(player);
            applyNeutronActivation(carried);
        }

        private void pollWhileForeignMenuOpen() {
            if (!(player instanceof ServerPlayer sp) || sp.containerMenu == sp.inventoryMenu)
                return;
            sp.inventoryMenu.suppressRemoteUpdates();
            try {
                sp.inventoryMenu.broadcastChanges();
            } finally {
                sp.inventoryMenu.resumeRemoteUpdates();
            }
        }

        private void applyNeutronActivation(float carried) {
            if (!RadiationData.NEUTRON_ACTIVATION.get()) return;
            if (carried > 0F) {
                ContaminationUtil.contaminate(
                        player,
                        ContaminationUtil.HazardType.NEUTRON,
                        ContaminationUtil.ContaminationType.CREATIVE,
                        carried * 0.05D * RadiationConfig.hazardRate);
            } else {
                HbmLivingProps.setNeutron(player, 0D);
            }

            double rate =
                    ContaminationUtil.getNoNeutronPlayerRads(player) * 0.00004D
                            - 0.00004D * RadiationData.NEUTRON_ACTIVATION_THRESHOLD.get();
            if (rate > MIN_RAD_RATE) owedActivation += rate * RadiationConfig.hazardRate;
            if (owedActivation <= 0D) return;

            long time = player.level().getGameTime();
            if (Math.floorMod(time + player.getId(), ACTIVATION_WRITE_PERIOD)
                    >= RadiationConfig.hazardRate) return;

            float owed = (float) owedActivation;

            if (owed < ContaminationUtil.ACTIVATION_FLOOR) return;
            owedActivation = 0D;
            ContaminationUtil.neutronActivateInventory(player, owed, 1.0F);
        }
    }
}
