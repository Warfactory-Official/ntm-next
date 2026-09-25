// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.MenuCrate;
import com.hbm.items.ModDataComponents;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.PersistentDrop;
import com.hbm.tileentity.machine.BlockEntityLockableBase;
import com.hbm.tileentity.machine.LockStateData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityCrate extends BlockEntityLockableBase
        implements MenuProvider, PersistentDrop {

    private static final String[] PERSISTENT_KEYS = {
        "inventory", "lock", "cheesable", "isLocked", "lockMod", "spiders"
    };

    public final CrateType crateType;
    private boolean hasSpiders;

    public BlockEntityCrate(CrateType crateType, BlockPos pos, BlockState state) {
        super(crateType.blockEntityType(), pos, state, crateType.slots);
        this.crateType = crateType;
    }

    @Override
    protected double interactionRangeSq() {
        return 64;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[getContainerSize()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Services.PLATFORM.canFitInsideContainerItems(stack);
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isLocked() && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isLocked();
    }

    @Override
    public int getComparatorPower() {
        return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
    }

    @Override
    public void startOpen(ContainerUser user) {
        level.playSound(
                null, worldPosition, ModSounds.CRATE_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        level.playSound(
                null, worldPosition, ModSounds.CRATE_CLOSE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {}

    public boolean hasSpiders() {
        return hasSpiders;
    }

    public void fillWithSpiders() {
        hasSpiders = true;
        setChanged();
    }

    public void releaseSpiders(Player player) {
        if (!hasSpiders) return;
        spawnSpiders(
                (ServerLevel) level,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                player);
        hasSpiders = false;
        setChanged();
    }

    public static void spawnSpiders(
            ServerLevel level, double x, double y, double z, Player target) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            CaveSpider spider = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.TRIGGERED);
            if (spider == null) continue;
            spider.snapTo(
                    x + random.nextGaussian() * 2,
                    y + 1,
                    z + random.nextGaussian() * 2,
                    random.nextFloat(),
                    0);
            spider.setTarget(target);
            level.addFreshEntity(spider);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hasSpiders = input.getBooleanOr("spiders", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("spiders", hasSpiders);
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        LockedContents locked = components.get(ModDataComponents.LOCKED_CONTENTS.get());

        ItemContainerContents contents =
                locked == null
                        ? components.getOrDefault(
                                DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                        : level instanceof ServerLevel server ? locked.open(server) : null;
        if (contents != null) contents.copyInto(inventory);
        LockStateData lockState = components.get(ModDataComponents.LOCK_STATE.get());
        if (lockState != null) applyLockState(lockState);
        hasSpiders = components.get(ModDataComponents.SPIDERS.get()) != null;
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {

        unpackLootTable(null);
        ItemContainerContents contents = ItemContainerContents.fromItems(inventory);

        if (contents != ItemContainerContents.EMPTY) {

            if (isLocked() || hasSpiders) {
                components.set(
                        ModDataComponents.LOCKED_CONTENTS.get(),
                        LockedContents.seal((ServerLevel) level, contents));
            } else {
                components.set(DataComponents.CONTAINER, contents);
            }
        }
        LockStateData lockState = getLockState();
        if (!lockState.isDefault()) components.set(ModDataComponents.LOCK_STATE.get(), lockState);
        if (hasSpiders) components.set(ModDataComponents.SPIDERS.get(), Unit.INSTANCE);

        boolean tagged = contents != ItemContainerContents.EMPTY || isLocked() || hasSpiders;
        components.set(
                ModDataComponents.STACKLOCK.get(), tagged ? level.getRandom().nextLong() : null);
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(crateType.containerKey);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {

        if (getLootTable() != null && player.isSpectator()) return null;
        unpackLootTable(player);
        return new MenuCrate(containerId, playerInventory, this);
    }
}
