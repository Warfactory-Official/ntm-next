/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) Contributors to Hbm's Nuclear Tech Mod. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.machine.storage;

import com.hbm.blockentity.PersistentDrop;
import com.hbm.blockentity.machine.LockStateData;
import com.hbm.blockentity.security.LockableBlockEntity;
import com.hbm.data.ItemData;
import com.hbm.inventory.container.CrateMenu;
import com.hbm.items.ModDataComponents;
import com.hbm.platform.Platform;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
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

public class CrateBlockEntity extends LockableBlockEntity implements PersistentDrop, MenuProvider {

    private static final int SPIDERS = 3;
    private static final String[] PERSISTENT_KEYS = {
        "inventory", "lock", "cheesable", "isLocked", "lockMod", "spiders", "customName"
    };

    public final CrateType crateType;
    private final int[] faceSlots;
    private boolean spiders;

    public CrateBlockEntity(CrateType crateType, BlockPos pos, BlockState state) {
        super(crateType.blockEntityType(), pos, state, crateType.slots);
        this.crateType = crateType;
        faceSlots = new int[crateType.slots];
        for (int slot = 0; slot < faceSlots.length; slot++) faceSlots[slot] = slot;
    }

    public boolean hasSpiders() {
        return spiders;
    }

    public void fillWithSpiders() {
        spiders = true;
        setChanged();
    }

    public void releaseSpiders(Player opener) {
        if (!spiders || opener.isSpectator() || !(level instanceof ServerLevel server)) return;
        spawnSpiders(
                server, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), opener);
        spiders = false;
        setChanged();
    }

    public static void spawnSpiders(
            ServerLevel level, double x, double y, double z, Player target) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < SPIDERS; i++) {
            CaveSpider spider =
                    EntityTypes.CAVE_SPIDER.create(
                            level, new EntitySpawnRequest(EntitySpawnReason.EVENT, true));
            double sx = x + random.nextGaussian() * 2D;
            double sz = z + random.nextGaussian() * 2D;
            spider.snapTo(sx, y + 1D, sz, random.nextFloat(), 0F);
            spider.setTarget(target);
            level.addFreshEntity(spider);
        }
    }

    @Override
    protected boolean syncName() {
        return true;
    }

    @Override
    protected double interactionRangeSq() {
        return 64D;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return Platform.canFitInsideContainerItems(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return faceSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
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
        cue(ModSounds.CRATE_OPEN.get());
    }

    @Override
    public void stopOpen(ContainerUser user) {
        cue(ModSounds.CRATE_CLOSE.get());
    }

    private void cue(SoundEvent sound) {
        if (level == null || level.isClientSide()) return;
        WorldSounds.play(
                level,
                null,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D,
                sound,
                SoundSource.BLOCKS,
                1F,
                1F);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId, Inventory inventory, Player player) {
        if (getLootTable() != null && player.isSpectator()) return null;
        unpackLootTable(player);
        return new CrateMenu(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(crateType.containerKey);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (ItemData.CRATE_KEEP_CONTENTS.get() || isLocked()) return;
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void writePersistent(DataComponentMap.Builder components) {
        if (!ItemData.CRATE_KEEP_CONTENTS.get()) return;
        unpackLootTable(null);
        boolean filled = !isEmpty();
        if (filled) {
            ItemContainerContents contents = ItemContainerContents.fromItems(inventory);
            if ((isLocked() || spiders) && level instanceof ServerLevel server) {
                components.set(
                        ModDataComponents.LOCKED_CONTENTS.get(),
                        LockedContents.seal(server, contents));
            } else {
                components.set(DataComponents.CONTAINER, contents);
            }
        }
        LockStateData lock = getLockState();
        if (!lock.isDefault()) components.set(ModDataComponents.LOCK_STATE.get(), lock);
        if (spiders) components.set(ModDataComponents.SPIDERS.get(), Unit.INSTANCE);
        components.set(DataComponents.CUSTOM_NAME, getCustomName());
        if (level != null && (filled || isLocked() || spiders)) {
            components.set(ModDataComponents.STACKLOCK.get(), level.getRandom().nextLong());
        }
    }

    @Override
    public void readPersistent(DataComponentGetter components) {
        ItemContainerContents contents = components.get(DataComponents.CONTAINER);
        LockedContents sealed = components.get(ModDataComponents.LOCKED_CONTENTS.get());
        if (contents == null && sealed != null && level instanceof ServerLevel server)
            contents = sealed.open(server);
        if (contents != null) contents.copyInto(inventory);
        LockStateData lock = components.get(ModDataComponents.LOCK_STATE.get());
        if (lock != null) applyLockState(lock);
        spiders = components.get(ModDataComponents.SPIDERS.get()) != null;
        setCustomName(components.get(DataComponents.CUSTOM_NAME));
    }

    @Override
    public String[] persistentKeys() {
        return PERSISTENT_KEYS;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        spiders = input.getBooleanOr("spiders", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (spiders) output.putBoolean("spiders", true);
    }

    public void stripPersistent(ValueOutput output) {
        for (String key : persistentKeys()) output.discard(key);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        writePersistent(components);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        readPersistent(components);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        stripPersistent(output);
    }
}
