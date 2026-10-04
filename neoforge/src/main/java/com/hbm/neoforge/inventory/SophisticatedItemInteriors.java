// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import com.hbm.handler.UnstableFuses;
import com.hbm.inventory.InteriorSlotSource;
import com.hbm.inventory.InventoryAlias;
import com.hbm.inventory.ItemInteriorAdapter;
import com.hbm.inventory.ItemInteriorBackend;
import com.hbm.inventory.ItemInteriors;
import com.hbm.inventory.SlotSource;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackStorage;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.inventory.StorageWrapperRepository;
import net.p3pp3rf1y.sophisticatedcore.upgrades.infinity.InfinityInventoryPart;
import net.p3pp3rf1y.sophisticatedstorage.block.ItemContentsStorage;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageBlockItem;
import org.jspecify.annotations.Nullable;

public final class SophisticatedItemInteriors {
    private SophisticatedItemInteriors() {}

    public static void registerBackpacks() {
        ItemInteriors.register(Backpacks.ADAPTER);
    }

    public static void registerStorage() {
        ItemInteriors.register(Storage.ADAPTER);
    }

    public static void backpackRecordChanged(UUID id) {
        recordChanged(Backpacks.ADAPTER, id);
    }

    public static void storageRecordChanged(UUID id) {
        recordChanged(Storage.ADAPTER, id);
    }

    private static void recordChanged(ItemInteriorAdapter adapter, UUID id) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER)
            ItemInteriors.backendChanged(ServerLifecycleHooks.getCurrentServer(), adapter, id);
    }

    private abstract static class Adapter implements ItemInteriorAdapter {
        private final Class<?> type;
        private final boolean endpointExclusion;

        Adapter(Class<?> type, boolean endpointExclusion) {
            this.type = type;
            this.endpointExclusion = endpointExclusion;
        }

        @Override
        public boolean handles(Class<?> type) {
            return this.type.isAssignableFrom(type);
        }

        @Override
        public @Nullable UUID reference(ItemInstance stack) {
            if (endpointExclusion && stack.has(ModCoreDataComponents.LINKED_STORAGE_ENDPOINT.get()))
                return null;
            return stack.get(ModCoreDataComponents.STORAGE_UUID.get());
        }
    }

    private abstract static class Backend extends ItemInteriorBackend {
        private static final SlotSource[] EMPTY = new SlotSource[0];
        protected final UUID id;
        protected final ItemStackTemplate prototype;
        private @Nullable ContainerContents contents;
        private int[] partitionBases;
        private List<String> partitionNames;
        private SlotSource[] sources = EMPTY;
        private @Nullable IStorageWrapper wrapper;

        Backend(MinecraftServer server, UUID id, ItemStackTemplate prototype) {
            super(server);
            this.id = id;
            this.prototype = prototype;
            changed();
        }

        protected abstract @Nullable ContainerContents readContents();

        protected abstract IStorageWrapper createWrapper();

        protected abstract void persist(IStorageWrapper current);

        protected final ContainerContents contents() {
            return contents;
        }

        @Override
        public SlotSource[] inventorySources() {
            return sources;
        }

        @Override
        public void changed() {
            ContainerContents current = readContents();
            if (contents != current) {
                contents = current;
                sources =
                        current == null
                                ? EMPTY
                                : new SlotSource[] {
                                    (InteriorSlotSource) current.inventory(),
                                    (InteriorSlotSource) current.upgrades()
                                };
                if (current != null) {
                    partitionBases = current.partitioner().baseIndexes();
                    partitionNames = current.partitioner().partNames();
                }
            }
            wrapper = null;
        }

        @Override
        public void slotChanged(SlotSource source, int slot) {
            if (sources.length == 0 || source != sources[1]) return;
            wrapper = null;
            var partition = contents.partitioner();
            if (partitionBases != partition.baseIndexes()
                    || partitionNames != partition.partNames()) {
                partitionBases = partition.baseIndexes();
                partitionNames = partition.partNames();
                UnstableFuses.reindexSource(server, sources[0]);
            }
        }

        private IStorageWrapper wrapper() {
            if (wrapper == null) wrapper = createWrapper();
            return wrapper;
        }

        @Override
        public boolean physicalSlot(SlotSource source, int slot) {
            if (sources.length == 0 || slot < 0 || slot >= source.storedSlotCount()) return false;
            if (source == sources[1]) return true;
            if (source != sources[0]) return false;
            InventoryHandler inventory = wrapper().getInventoryHandler();
            return ((InventoryAlias) inventory).inventoryIdentity() == contents.inventory()
                    && slot < inventory.size()
                    && !(inventory.getInventoryPartitioner().getPartBySlot(slot)
                            instanceof InfinityInventoryPart);
        }

        @Override
        public boolean replaceStoredItem(
                SlotSource source, int slot, ItemStack expected, ItemStack replacement) {
            if (inventoryCarrier() == null
                    || Transaction.getLifecycle() != Transaction.Lifecycle.NONE
                    || slot < 0
                    || slot >= source.storedSlotCount()
                    || source.storedItem(slot) != expected) return false;
            wrapper = null;
            if (!physicalSlot(source, slot)) return false;
            IStorageWrapper current = wrapper();
            if (source == sources[0])
                current.getInventoryHandler().setStackInSlotInternal(slot, replacement);
            else {
                var upgrades = current.getUpgradeHandler();
                if (((InventoryAlias) upgrades).inventoryIdentity() != contents.upgrades())
                    return false;
                upgrades.setStackInSlot(slot, replacement);
            }
            return true;
        }

        @Override
        public void publishStoredChanges(SlotSource source, int slot) {
            persist(wrapper());
        }
    }

    private static final class Backpacks {
        private static final ItemInteriorAdapter ADAPTER =
                new Adapter(BackpackItem.class, true) {
                    @Override
                    public ItemInteriorBackend backend(
                            MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                        return new BackpackBackend(server, id, prototype);
                    }
                };

        private static final class BackpackBackend extends Backend {
            BackpackBackend(MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                super(server, id, prototype);
            }

            @Override
            protected @Nullable ContainerContents readContents() {
                return ((SophisticatedContentsStorage) BackpackStorage.get()).storedContents(id);
            }

            @Override
            protected IStorageWrapper createWrapper() {
                ItemStack item = prototype.create();
                item.remove(
                        net.p3pp3rf1y.sophisticatedbackpacks.init.ModDataComponents.COLUMNS_TAKEN
                                .get());
                var current = BackpackWrapper.fromStack(item);
                current.setSlotNumbers(
                        contents().inventory().stacks().size(),
                        contents().upgrades().stacks().size());
                return current;
            }

            @Override
            protected void persist(IStorageWrapper current) {
                BackpackStorage.get().setDirty();
                StorageWrapperRepository.invalidateStorageWrapperContents(id, current);
            }
        }
    }

    private static final class Storage {
        private static final ItemInteriorAdapter ADAPTER =
                new Adapter(StorageBlockItem.class, false) {
                    @Override
                    public ItemInteriorBackend backend(
                            MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                        return new StorageBackend(server, id, prototype);
                    }
                };

        private static final class StorageBackend extends Backend {
            StorageBackend(MinecraftServer server, UUID id, ItemStackTemplate prototype) {
                super(server, id, prototype);
            }

            @Override
            protected @Nullable ContainerContents readContents() {
                return ((SophisticatedContentsStorage) ItemContentsStorage.get())
                        .storedContents(id);
            }

            @Override
            protected IStorageWrapper createWrapper() {
                return new SavedStorageWrapper(prototype.create(), id, contents());
            }

            @Override
            protected void persist(IStorageWrapper current) {
                ItemContentsStorage.get().setDirty();
                StorageWrapperRepository.invalidateStorageWrapperContents(id, current);
            }
        }

        private static final class SavedStorageWrapper extends StackStorageWrapper {
            private final ContainerContents saved;

            SavedStorageWrapper(ItemStack item, UUID id, ContainerContents saved) {
                super(item);
                this.saved = saved;
                contentsUuid = id;
                numberOfInventorySlots = saved.inventory().stacks().size();
                numberOfUpgradeSlots = saved.upgrades().stacks().size();
                getSettingsHandler().reloadFrom(saved.settings());
            }

            @Override
            public ContainerContents getContents() {
                return saved;
            }

            @Override
            public int getDefaultNumberOfInventorySlots() {
                return saved.inventory().stacks().size();
            }

            @Override
            public int getDefaultNumberOfUpgradeSlots() {
                return saved.upgrades().stacks().size();
            }
        }
    }
}
