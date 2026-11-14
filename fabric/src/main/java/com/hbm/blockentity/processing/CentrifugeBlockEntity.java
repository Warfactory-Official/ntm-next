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

package com.hbm.blockentity.processing;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.EnergyItem;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.AudioLoop;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.SoundEmitter;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.data.MachineData;
import com.hbm.inventory.container.CentrifugeMenu;
import com.hbm.inventory.recipes.CentrifugeRecipe;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.ModSounds;
import com.hbm.sound.SoundHandle;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class CentrifugeBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                AudioLoop,
                SoundEmitter,
                MenuProvider,
                UpgradeInfoProvider,
                SyncUnitSchema {
    public static final int SLOT_COUNT = 8;
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_OUTPUT_START = 2;
    public static final int SLOT_OUTPUT_END = 5;
    public static final int SLOT_UPGRADE_START = 6;
    public static final int SLOT_UPGRADE_END = 7;
    public static final UpgradeLimits VALID_UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.OVERDRIVE, 3);
    private static final int[] ITEM_ACCESS = {0, 2, 3, 4, 5};
    public static Consumer<CentrifugeBlockEntity> CLIENT_SOUND = _ -> {};
    private final UpgradeManager upgrades = new UpgradeManager(this);
    public long power;
    public int progress;
    public boolean isProgressing;
    public int audioDuration;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CENTRIFUGE.get(), pos, state, SLOT_COUNT);
    }

    private boolean fits(CentrifugeRecipe recipe) {
        var outputs = recipe.outputItems();
        for (int i = 0; i < 4; i++) {
            if (!Outputs.fits(getItem(SLOT_OUTPUT_START + i), outputs, i)) return false;
        }
        return true;
    }

    public boolean canProcess() {
        if (power <= 0L) return false;
        var recipe = CentrifugeRecipes.INSTANCE.matchFor(getItem(SLOT_INPUT), level);
        return recipe != null && fits(recipe);
    }

    private void complete(CentrifugeRecipe recipe) {
        var outputs = recipe.outputItems();
        for (int i = 0; i < outputs.length; i++) {
            var product = outputs[i].getRandom(GenericRecipes.RNG).orElse(ItemStack.EMPTY);
            if (product.isEmpty()) continue;
            int slot = SLOT_OUTPUT_START + i;
            var stored = getItem(slot);
            if (stored.isEmpty()) setItem(slot, product.copy());
            else stored.grow(product.getCount());
        }
        removeItem(SLOT_INPUT, 1);
        setChanged();
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        int previousProgress = progress;
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, getMaxPower() - power, false);
        upgrades.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        int overdrive = upgrades.getLevel(UpgradeType.OVERDRIVE);
        if (power > 0L && progress > 0) {
            long baseCost = MachineData.CENTRIFUGE_CONSUMPTION.get();
            long cost =
                    (baseCost + speed * baseCost + 50L * overdrive * baseCost)
                            / (1L + upgrades.getLevel(UpgradeType.POWER));
            power = Math.max(0L, power - cost);
        }
        hbm$syncSet$CentrifugeBlockEntity$isProgressing(false);
        if (power > 0L) {
            var recipe = CentrifugeRecipes.INSTANCE.matchFor(getItem(SLOT_INPUT), level);
            if (recipe != null && fits(recipe)) {
                progress += (1 + speed) * (1 + 5 * overdrive);
                hbm$syncSet$CentrifugeBlockEntity$isProgressing(true);
                if (progress >= MachineData.CENTRIFUGE_PROCESS_TIME.get()) {
                    complete(recipe);
                    progress = 0;
                }
            }
        }
        if (!isProgressing) progress = 0;
        if (power != previousPower || progress != previousProgress) setChanged();
        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
    }

    @Override
    public boolean tickSound() {
        audioDuration = Math.clamp(audioDuration + (isProgressing ? 2 : -3), 0, 60);
        return audioDuration > 10;
    }

    @Override
    public SoundHandle createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CENTRIFUGE_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX() + 0.5F,
                worldPosition.getY() + 0.5F,
                worldPosition.getZ() + 0.5F,
                1F,
                10F,
                1F,
                20);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ITEM_ACCESS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> CentrifugeRecipes.INSTANCE.matchFor(stack, level) != null;
            case SLOT_BATTERY -> EnergyItem.matches(stack);
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_INPUT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= SLOT_OUTPUT_START && slot <= SLOT_OUTPUT_END;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return MachineData.CENTRIFUGE_MAX_POWER.get();
    }

    @Override
    public void setPower(long stored) {
        power = Math.clamp(stored, 0L, getMaxPower());
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return VALID_UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int tier, List<Component> tooltip) {
        UpgradeInfo.centrifuge(type, tier, tooltip);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.centrifuge");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new CentrifugeMenu(id, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", power);
        progress = input.getIntOr("progress", progress);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == 0) output.writeBoolean(isProgressing);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == 0) hbm$syncSet$CentrifugeBlockEntity$isProgressing(input.readBoolean());
        else super.readSyncUnit(unit, input);
    }

    public final void hbm$syncSet$CentrifugeBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    static {
        com.hbm.inventory.container.SyncedData.fields(
                CentrifugeBlockEntity.class, "power", "progress");
    }

    private SoundHandle hbm$audio;

    public void audioLoop(boolean running, float volume) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, 0F, false);
    }

    public void audioLoop(boolean running, float volume, float pitch) {
        hbm$audio = AudioLoop.loop(this, hbm$audio, running, volume, pitch, true);
    }

    private void hbm$audioRemove() {
        audioLoop(false, 0F);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        hbm$audioRemove();
    }

    private boolean hbm$soundActive;

    public boolean soundStarted(boolean active) {
        boolean started = active && !hbm$soundActive;
        hbm$soundActive = active;
        return started;
    }
}
