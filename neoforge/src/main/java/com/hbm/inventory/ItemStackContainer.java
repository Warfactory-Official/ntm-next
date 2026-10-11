// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import com.hbm.handler.UnstableFuses;
import com.hbm.items.tool.ItemLeadBox;
import com.hbm.items.tool.ItemPlasticBag;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

public class ItemStackContainer extends SimpleContainer {

    private final @Nullable Player owner;
    private final ItemStack target;
    private final Store store;
    private boolean writing;

    public ItemStackContainer(@Nullable Player owner, ItemStack target, int size) {
        this(owner, target, size, Store.CONTAINER);
    }

    public ItemStackContainer(@Nullable Player owner, ItemStack target, int size, Store store) {
        super(size);
        this.owner = owner;
        this.target = target;
        this.store = store;

        store.read(target).copyInto(getItems());
        if (owner != null) {
            Inventory inventory = owner.getInventory();
            int slot;
            if (owner.getMainHandItem() == target) slot = inventory.getSelectedSlot();
            else if (owner.getOffhandItem() == target) slot = Inventory.SLOT_OFFHAND;
            else
                throw new IllegalArgumentException(
                        "Item-backed menu target must be held by its owner");
            InventorySlotBinding.bind(target, inventory, slot);
        }
    }

    public ItemStack target() {
        return target;
    }

    public Player inventoryHolder() {
        return owner;
    }

    @Override
    public int getMaxStackSize() {
        return target.getItem() instanceof ItemLeadBox || target.getItem() instanceof ItemPlasticBag
                ? 1
                : super.getMaxStackSize();
    }

    @Override
    public void setChanged() {
        writing = true;
        try {
            store.write(target, ItemContainerContents.fromItems(getItems()));
        } finally {
            writing = false;
        }
        if (owner != null && !InventorySlotBinding.isBound(target)) UnstableFuses.changed(owner);
    }

    public void refreshFromTarget() {
        if (writing) return;
        store.read(target).copyInto(getItems());
    }

    @Override
    public boolean stillValid(Player player) {
        return !target.isEmpty()
                && player == owner
                && (player.getMainHandItem() == target || player.getOffhandItem() == target);
    }

    @Override
    public void startOpen(ContainerUser user) {
        cue(ModSounds.CRATE_OPEN.get());
    }

    @Override
    public void stopOpen(ContainerUser user) {
        setChanged();
        cue(ModSounds.CRATE_CLOSE.get());
    }

    public interface Store {
        Store CONTAINER =
                new Store() {
                    @Override
                    public ItemContainerContents read(ItemStack stack) {
                        return stack.getOrDefault(
                                DataComponents.CONTAINER, ItemContainerContents.EMPTY);
                    }

                    @Override
                    public void write(ItemStack stack, ItemContainerContents contents) {
                        if (contents == ItemContainerContents.EMPTY)
                            stack.remove(DataComponents.CONTAINER);
                        else stack.set(DataComponents.CONTAINER, contents);
                    }
                };

        ItemContainerContents read(ItemStack stack);

        void write(ItemStack stack, ItemContainerContents contents);
    }

    private void cue(SoundEvent sound) {
        if (owner == null || target.getItem() instanceof ItemPlasticBag) return;
        EntitySounds.play(
                owner,
                owner.getX(),
                owner.getY(),
                owner.getZ(),
                sound,
                SoundSource.PLAYERS,
                1.0F,
                0.8F);
    }
}
