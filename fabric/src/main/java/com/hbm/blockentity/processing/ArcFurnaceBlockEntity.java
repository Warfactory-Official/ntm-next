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
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.MachineSoundProvider;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.ArcFurnaceMenu;
import com.hbm.inventory.material.MaterialRegistry;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.recipes.ArcFurnaceRecipe;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.loader.RecipeSource;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ArcElectrodeBurntItem;
import com.hbm.items.machine.ArcElectrodeItem;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.SoundHandle;
import com.hbm.util.CrucibleUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ArcFurnaceBlockEntity extends MachineBlockEntity
        implements EnergyHandler,
                UpgradeInfoProvider,
                MenuProvider,
                MachineSoundProvider,
                SyncUnitSchema {
    public static final int SLOT_COUNT = 30;
    public static final int SLOT_GRID_START = 5;
    public static final int SLOT_GRID_END = 24;
    public static final long MAX_POWER = 2_500_000L;
    public static final int MAX_LIQUID = MaterialShape.BLOCK.amount(128);
    public static final byte ELECTRODE_NONE = 0;
    public static final byte ELECTRODE_FRESH = 1;
    public static final byte ELECTRODE_USED = 2;
    public static final byte ELECTRODE_DEPLETED = 3;

    private static final int QUEUE_START = 25;
    private static final int[] INPUT_COUNTS = {1, 4, 8, 16};
    private static final int[] WORK_DIVISORS = {400, 133, 80, 57};
    private static final long[] WORK_COSTS = {1_000L, 5_000L, 25_000L, 125_000L};
    private static final int[] LID_DIVISORS = {60, 40, 30, 24};
    private static final int[] BATCH_DELAYS = {120, 80, 60, 48};
    private static final int[] AUTOMATION_SLOTS = {
        0, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
        27, 28, 29
    };
    private static final UpgradeLimits UPGRADE_CAPS = UpgradeLimits.of(UpgradeType.SPEED, 3);
    public static Consumer<ArcFurnaceBlockEntity> CLIENT_SOUND = _ -> {};

    public long power;
    public float progress;
    public float lid;
    public float prevLid;
    public boolean isProgressing;
    public boolean liquidMode;
    public boolean hasMaterial;
    public int upgrade;
    public final byte[] electrodes = new byte[3];
    public final List<MaterialStack> liquids = new SyncList<>();
    public final List<PourStream> streams = new ArrayList<>();
    public int delay;
    public @Nullable SoundHandle audioLid;
    public @Nullable SoundHandle audioProgress;

    private final UpgradeManager upgrades = new UpgradeManager(this);
    private final ItemStack[] recipeKeys = new ItemStack[SLOT_COUNT];
    private final ArcFurnaceRecipe[] recipes = new ArcFurnaceRecipe[SLOT_COUNT];
    private final boolean[] recipeValid = new boolean[SLOT_COUNT];
    private final int[] slotLimits = new int[SLOT_COUNT];
    private final int[] moltenPerItem = new int[SLOT_COUNT];
    private RecipeSource.@Nullable Token recipeToken;
    private @Nullable List<ArcFurnaceRecipe> recipeGeneration;
    private boolean cachedLiquidMode;
    private int cachedUpgrade = -1;
    private boolean cachedWorking;
    private boolean processableGrid;
    private int pourColor;
    private float pourLength;

    public record PourStream(int color, float len, long birth) {}

    public ArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_FURNACE_LARGE.get(), pos, state, SLOT_COUNT);
        Arrays.fill(recipeKeys, ItemStack.EMPTY);
    }

    @Override
    public void tickServer() {
        long previousPower = power;
        float previousProgress = progress;
        float previousLift = lid;
        int previousDelay = delay;
        hbm$syncSet$ArcFurnaceBlockEntity$prevLid(lid);
        hbm$syncSet$ArcFurnaceBlockEntity$power(
                (long) (power + (ItemEnergyTransfer.extract(this, 3, MAX_POWER - power, false))));
        upgrades.scan(this, 4, 4);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        if (upgrade != speed) hbm$syncSet$ArcFurnaceBlockEntity$upgrade(speed);
        hbm$syncSet$ArcFurnaceBlockEntity$isProgressing(false);
        refreshRecipes(true);
        if (lid > 0F && feedGrid()) refreshRecipes(true);
        if (power > 0L) {
            boolean canWork =
                    processableGrid && liveElectrodes() && delay <= 0 && liquids.isEmpty();
            float travel = 1F / LID_DIVISORS[upgrade];
            if (canWork) {
                if (lid > 0F) {
                    hbm$syncSet$ArcFurnaceBlockEntity$lid(Math.max(0F, lid - travel));
                    hbm$syncSet$ArcFurnaceBlockEntity$progress(0F);
                } else if (power >= WORK_COSTS[upgrade]) {
                    hbm$syncSet$ArcFurnaceBlockEntity$power((long) (power - (WORK_COSTS[upgrade])));
                    hbm$syncSet$ArcFurnaceBlockEntity$progress(
                            (float) (progress + (1F / WORK_DIVISORS[upgrade])));
                    hbm$syncSet$ArcFurnaceBlockEntity$isProgressing(true);
                    if (progress >= 1F) finishBatch();
                }
            } else {
                hbm$syncSet$ArcFurnaceBlockEntity$progress(0F);
                if (delay > 0) delay--;
                hbm$syncSet$ArcFurnaceBlockEntity$lid(Math.min(1F, lid + travel));
            }
        }
        refreshElectrodes();
        if (lid > 0F && !liquids.isEmpty()) pourMaterials();
        if (power != previousPower
                || progress != previousProgress
                || lid != previousLift
                || delay != previousDelay) setChanged();
        networkPackNT(150);
    }

    private void refreshRecipes(boolean updateVisible) {
        prepareRecipeState();
        boolean present = false;
        boolean ready = false;
        for (int slot = SLOT_GRID_START; slot < SLOT_COUNT; slot++) {
            refreshRecipe(slot);
            if (slot <= SLOT_GRID_END) {
                present |= !inventory.get(slot).isEmpty();
                ready |= recipes[slot] != null && slotLimits[slot] > 0;
            }
        }
        if (updateVisible && hasMaterial != present)
            hbm$syncSet$ArcFurnaceBlockEntity$hasMaterial(present);
        processableGrid = ready;
    }

    private void prepareRecipeState() {
        var token = RecipeSource.token();
        var generation = ArcFurnaceRecipes.INSTANCE.recipes();
        boolean invalidate =
                recipeToken != token
                        || recipeGeneration != generation
                        || cachedLiquidMode != liquidMode
                        || cachedUpgrade != upgrade;
        recipeToken = token;
        recipeGeneration = generation;
        cachedLiquidMode = liquidMode;
        cachedUpgrade = upgrade;
        if (invalidate) Arrays.fill(recipeValid, false);
    }

    private void refreshRecipe(int slot) {
        var stack = inventory.get(slot);
        var key = recipeKeys[slot];
        boolean inputChanged =
                key.getCount() != stack.getCount()
                        || !ItemStack.isSameItemSameComponents(key, stack);
        if (recipeValid[slot] && !inputChanged) return;
        if (inputChanged) recipeKeys[slot] = stack.copy();
        var row = stack.isEmpty() ? null : ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode);
        recipes[slot] = row;
        slotLimits[slot] = row == null ? 0 : inputLimit(stack, row);
        int amount = 0;
        if (row != null && liquidMode) {
            for (var material : row.fluidOutput()) amount += material.amount;
        }
        moltenPerItem[slot] = amount;
        recipeValid[slot] = true;
    }

    private int inputLimit(ItemStack stack, ArcFurnaceRecipe row) {
        int limit = Math.min(getMaxInputSize(), stack.getMaxStackSize());
        if (!liquidMode) {
            var output = row.solidOutput();
            limit = Math.min(limit, output.getMaxStackSize() / output.getCount());
        }
        return limit;
    }

    private boolean feedGrid() {
        boolean changed = false;
        for (int queue = QUEUE_START; queue < SLOT_COUNT; queue++) {
            var input = inventory.get(queue);
            if (recipes[queue] == null || slotLimits[queue] == 0) continue;
            for (int pass = 0; pass < 2 && !input.isEmpty(); pass++) {
                for (int slot = SLOT_GRID_START;
                        slot <= SLOT_GRID_END && !input.isEmpty();
                        slot++) {
                    var stored = inventory.get(slot);
                    if (pass == 0
                            ? stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, input)
                            : !stored.isEmpty()) continue;
                    int amount = Math.min(input.getCount(), slotLimits[queue] - stored.getCount());
                    if (amount <= 0) continue;
                    if (stored.isEmpty()) inventory.set(slot, input.copyWithCount(amount));
                    else stored.grow(amount);
                    input.shrink(amount);
                    changed = true;
                }
            }
            if (input.isEmpty()) inventory.set(queue, ItemStack.EMPTY);
        }
        if (changed) setChanged();
        return changed;
    }

    private boolean liveElectrodes() {
        return inventory.get(0).getItem() instanceof ArcElectrodeItem
                && inventory.get(1).getItem() instanceof ArcElectrodeItem
                && inventory.get(2).getItem() instanceof ArcElectrodeItem;
    }

    private void finishBatch() {
        int moltenTotal = getStackAmount(liquids);
        for (int slot = SLOT_GRID_START; slot <= SLOT_GRID_END; slot++) {
            var row = recipes[slot];
            if (row == null || slotLimits[slot] == 0) continue;
            var input = inventory.get(slot);
            if (liquidMode) {
                int consumed =
                        Math.min(
                                input.getCount(), (MAX_LIQUID - moltenTotal) / moltenPerItem[slot]);
                if (consumed == 0) continue;
                for (var addition : row.fluidOutput()) addMaterial(addition, consumed);
                moltenTotal += moltenPerItem[slot] * consumed;
                input.shrink(consumed);
                if (input.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
            } else {
                var output = row.solidOutput();
                inventory.set(slot, output.copyWithCount(output.getCount() * input.getCount()));
            }
        }
        for (int slot = 0; slot < 3; slot++) {
            var electrode = inventory.get(slot);
            if (ArcElectrodeItem.damage(electrode)) {
                var type = ((ArcElectrodeItem) electrode.getItem()).type;
                inventory.set(slot, new ItemStack(ModItems.ARC_ELECTRODE_BURNT.get(type)));
            }
        }
        hbm$syncSet$ArcFurnaceBlockEntity$progress(0F);
        delay = BATCH_DELAYS[upgrade];
        PollutionHandler.add(level, worldPosition, PollutionType.SOOT, 10F);
        updateMaterialPresence();
        setChanged();
    }

    private void addMaterial(MaterialStack addition, int count) {
        for (int index = 0; index < liquids.size(); index++) {
            var stored = liquids.get(index);
            if (stored.material == addition.material) {
                stored.hbm$syncSet$MaterialStack$amount(
                        (int) (stored.amount + (addition.amount * count)));
                return;
            }
        }
        liquids.add(new MaterialStack(addition.material, addition.amount * count));
    }

    private void updateMaterialPresence() {
        boolean present = false;
        for (int slot = SLOT_GRID_START; slot <= SLOT_GRID_END; slot++)
            present |= !inventory.get(slot).isEmpty();
        if (hasMaterial != present) hbm$syncSet$ArcFurnaceBlockEntity$hasMaterial(present);
    }

    private void refreshElectrodes() {
        boolean workingChanged = cachedWorking != isProgressing;
        cachedWorking = isProgressing;
        for (int slot = 0; slot < 3; slot++) {
            var stack = inventory.get(slot);
            var key = recipeKeys[slot];
            boolean inputChanged =
                    key.getCount() != stack.getCount()
                            || !ItemStack.isSameItemSameComponents(key, stack);
            if (!workingChanged && !inputChanged) continue;
            if (inputChanged) recipeKeys[slot] = stack.copy();
            byte code = ELECTRODE_NONE;
            if (stack.getItem() instanceof ArcElectrodeItem)
                code =
                        isProgressing || ArcElectrodeItem.getDurability(stack) > 0
                                ? ELECTRODE_USED
                                : ELECTRODE_FRESH;
            else if (stack.getItem() instanceof ArcElectrodeBurntItem) code = ELECTRODE_DEPLETED;
            if (electrodes[slot] != code)
                hbm$syncArraySet$ArcFurnaceBlockEntity$electrodes(electrodes, slot, code);
        }
    }

    private void pourMaterials() {
        var facing = getBlockState().getValue(MultiblockCoreBlock.FACING);
        var pour =
                CrucibleUtil.pourFirst(
                        level,
                        worldPosition.getX() + 0.5D + facing.getStepX() * 2.875D,
                        worldPosition.getY() + 1.25D,
                        worldPosition.getZ() + 0.5D + facing.getStepZ() * 2.875D,
                        6D,
                        liquids,
                        MaterialShape.INGOT.quanta());
        if (pour == null) return;
        for (int index = liquids.size() - 1; index >= 0; index--)
            if (liquids.get(index).amount <= 0) liquids.remove(index);
        hbm$syncSet$ArcFurnaceBlockEntity$pourColor(pour.material().moltenColor());
        hbm$syncSet$ArcFurnaceBlockEntity$pourLength(
                Math.max(1F, (float) (worldPosition.getY() + 1D - pour.impact().y)));
        syncUnitsChanged(1, 0x200L);
        markSyncEvent();
        setChanged();
    }

    public void spillLiquids(@Nullable Player player) {
        if (liquids.isEmpty()) return;
        var recovered = new ItemStack[liquids.size()];
        for (int index = 0; index < recovered.length; index++)
            recovered[index] = ScrapsItem.create(liquids.get(index));
        liquids.clear();
        setChanged();
        for (var stack : recovered) {
            if (player == null)
                Containers.dropItemStack(
                        level,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.5D,
                        worldPosition.getZ() + 0.5D,
                        stack);
            else player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
        }
    }

    public void toggleLiquidMode() {
        hbm$syncSet$ArcFurnaceBlockEntity$liquidMode(!liquidMode);
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < 3) return stack.getItem() instanceof ArcElectrodeItem;
        return slot >= QUEUE_START
                && ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot < 3) return lid == 1F && !(stack.getItem() instanceof ArcElectrodeItem);
        if (slot < SLOT_GRID_START) return false;
        return (slot >= QUEUE_START || lid > 0F)
                && ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode) == null;
    }

    public long distributeInput(ItemStack stack, long amount, boolean apply) {
        long remaining = amount;
        if (stack.getItem() instanceof ArcElectrodeItem) {
            for (int slot = 0; slot < 3 && remaining > 0L; slot++) {
                if (!inventory.get(slot).isEmpty()) continue;
                if (apply) inventory.set(slot, stack.copyWithCount(1));
                remaining--;
            }
        } else {
            var row = ArcFurnaceRecipes.INSTANCE.getOutput(stack, liquidMode);
            if (row == null) return 0L;
            int limit = inputLimit(stack, row);
            for (int pass = 0; pass < 2 && remaining > 0L; pass++) {
                for (int slot = SLOT_GRID_START; slot <= SLOT_GRID_END && remaining > 0L; slot++) {
                    var stored = inventory.get(slot);
                    if (pass == 0
                            ? stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, stack)
                            : !stored.isEmpty()) continue;
                    int accepted =
                            (int) Math.min(remaining, Math.max(0, limit - stored.getCount()));
                    if (apply && accepted > 0) {
                        if (stored.isEmpty()) inventory.set(slot, stack.copyWithCount(accepted));
                        else stored.grow(accepted);
                    }
                    remaining -= accepted;
                }
            }
        }
        long accepted = amount - remaining;
        if (apply && accepted > 0L) setChanged();
        return accepted;
    }

    public long collectRequested(ItemStack requested, long amount, boolean apply) {
        prepareRecipeState();
        long remaining = amount;
        for (int slot = 0; slot < SLOT_COUNT && remaining > 0L; slot++) {
            var available = availableItem(slot);
            if (available.isEmpty() || !ItemStack.isSameItemSameComponents(available, requested))
                continue;
            int collected = (int) Math.min(remaining, available.getCount());
            if (apply) {
                available.shrink(collected);
                if (available.isEmpty()) inventory.set(slot, ItemStack.EMPTY);
            }
            remaining -= collected;
        }
        long collected = amount - remaining;
        if (apply && collected > 0L) setChanged();
        return collected;
    }

    public ItemStack getAvailableItemFromSlot(int slot) {
        prepareRecipeState();
        return availableItem(slot);
    }

    private ItemStack availableItem(int slot) {
        var stack = inventory.get(slot);
        if (slot < 3) return stack.getItem() instanceof ArcElectrodeItem ? ItemStack.EMPTY : stack;
        if (slot < SLOT_GRID_START || slot > SLOT_GRID_END) return ItemStack.EMPTY;
        refreshRecipe(slot);
        return recipes[slot] != null ? ItemStack.EMPTY : stack;
    }

    public int getMaxInputSize() {
        return INPUT_COUNTS[upgrade];
    }

    public static int getStackAmount(List<MaterialStack> materials) {
        int total = 0;
        for (int index = 0; index < materials.size(); index++) total += materials.get(index).amount;
        return total;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long stored) {
        hbm$syncSet$ArcFurnaceBlockEntity$power(stored);
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    public RandomSource getRandom() {
        return level.getRandom();
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADE_CAPS;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED)
            UpgradeInfo.speedAndPower(
                    tooltip,
                    WORK_DIVISORS[level] * 100 / WORK_DIVISORS[0] - 100,
                    (int) (WORK_COSTS[level] * 100 / WORK_COSTS[0] - 100));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineArcFurnaceLarge");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ArcFurnaceMenu(id, playerInventory, this);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
        long now = level.getGameTime();
        for (int index = streams.size() - 1; index >= 0; index--)
            if (now - streams.get(index).birth() >= 20L) streams.remove(index);
    }

    @Override
    public void setRemoved() {
        if (audioLid != null) audioLid.stop();
        if (audioProgress != null) audioProgress.stop();
        super.setRemoved();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ArcFurnaceBlockEntity$power(input.getLongOr("power", 0L));
        hbm$syncSet$ArcFurnaceBlockEntity$liquidMode(input.getBooleanOr("liquidMode", false));
        hbm$syncSet$ArcFurnaceBlockEntity$progress(input.getFloatOr("progress", 0F));
        hbm$syncSet$ArcFurnaceBlockEntity$lid(input.getFloatOr("lid", 0F));
        hbm$syncSet$ArcFurnaceBlockEntity$prevLid(lid);
        delay = input.getIntOr("delay", 0);
        int[] ids = input.getIntArray("liquidMats").orElseGet(() -> new int[0]);
        int[] amounts = input.getIntArray("liquidAmounts").orElseGet(() -> new int[0]);
        if (ids.length != amounts.length)
            throw new IllegalArgumentException("Unpaired molten material arrays");
        liquids.clear();
        for (int index = 0; index < ids.length; index++) {
            liquids.add(new MaterialStack(MaterialRegistry.byId(ids[index]), amounts[index]));
        }
        recipeGeneration = null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("liquidMode", liquidMode);
        output.putFloat("progress", progress);
        output.putFloat("lid", lid);
        output.putInt("delay", delay);
        int[] ids = new int[liquids.size()];
        int[] amounts = new int[liquids.size()];
        for (int index = 0; index < ids.length; index++) {
            var material = liquids.get(index);
            ids[index] = material.material.id();
            amounts[index] = material.amount;
        }
        output.putIntArray("liquidMats", ids);
        output.putIntArray("liquidAmounts", amounts);
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3FFL;
    }

    @Override
    public long syncEventUnits() {
        return 0x200L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> output.writeFloat(progress);
            case 2 -> {
                output.writeFloat(lid);
                output.writeFloat(prevLid);
            }
            case 3 -> output.writeBoolean(isProgressing);
            case 4 -> output.writeBoolean(liquidMode);
            case 5 -> output.writeBoolean(hasMaterial);
            case 6 -> output.writeByte(upgrade);
            case 7 -> output.writeBytes(electrodes);
            case 8 -> {
                output.writeShort(liquids.size());
                for (int index = 0; index < liquids.size(); index++) {
                    var material = liquids.get(index);
                    output.writeInt(material.material.id());
                    output.writeInt(material.amount);
                }
            }
            case 9 -> {
                output.writeInt(pourColor);
                output.writeFloat(pourLength);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ArcFurnaceBlockEntity$power(input.readLong());
            case 1 -> hbm$syncSet$ArcFurnaceBlockEntity$progress(input.readFloat());
            case 2 -> {
                hbm$syncSet$ArcFurnaceBlockEntity$lid(input.readFloat());
                hbm$syncSet$ArcFurnaceBlockEntity$prevLid(input.readFloat());
            }
            case 3 -> hbm$syncSet$ArcFurnaceBlockEntity$isProgressing(input.readBoolean());
            case 4 -> hbm$syncSet$ArcFurnaceBlockEntity$liquidMode(input.readBoolean());
            case 5 -> hbm$syncSet$ArcFurnaceBlockEntity$hasMaterial(input.readBoolean());
            case 6 -> hbm$syncSet$ArcFurnaceBlockEntity$upgrade(input.readByte());
            case 7 -> input.readBytes(electrodes);
            case 8 -> {
                int count = input.readShort();
                if (count < 0) throw new DecoderException("Negative molten material count");
                liquids.clear();
                for (int index = 0; index < count; index++) {
                    var material = MaterialRegistry.byId(input.readInt());
                    int amount = input.readInt();
                    liquids.add(new MaterialStack(material, amount));
                }
            }
            case 9 -> {
                hbm$syncSet$ArcFurnaceBlockEntity$pourColor(input.readInt());
                hbm$syncSet$ArcFurnaceBlockEntity$pourLength(input.readFloat());
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    public void afterSyncUnits(long units) {
        if ((units & 0x200L) != 0L)
            streams.add(new PourStream(pourColor, pourLength, level.getGameTime()));
    }

    @Override
    public void afterInitialSyncUnits() {}

    public final void hbm$syncSet$ArcFurnaceBlockEntity$power(long next) {
        if (this.power != next) {
            this.power = next;
            if ((1) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1);
        }
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$progress(float next) {
        if (Float.floatToRawIntBits(this.progress) != Float.floatToRawIntBits(next)) {
            this.progress = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$lid(float next) {
        if (Float.floatToRawIntBits(this.lid) != Float.floatToRawIntBits(next)) {
            this.lid = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$prevLid(float next) {
        if (Float.floatToRawIntBits(this.prevLid) != Float.floatToRawIntBits(next)) {
            this.prevLid = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$isProgressing(boolean next) {
        if (this.isProgressing != next) {
            this.isProgressing = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$liquidMode(boolean next) {
        if (this.liquidMode != next) {
            this.liquidMode = next;
            if ((16) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16);
        }
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$hasMaterial(boolean next) {
        if (this.hasMaterial != next) {
            this.hasMaterial = next;
            if ((32) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32);
        }
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$upgrade(int next) {
        if (this.upgrade != next) {
            this.upgrade = next;
            if ((64) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 64);
        }
    }

    public final byte hbm$syncArraySet$ArcFurnaceBlockEntity$electrodes(
            byte[] array, int index, byte next) {
        if (array[index] != next) {
            array[index] = next;
            if (array == this.electrodes) {
                syncArrayChanged(array, index, 3, 128);
            }
        }
        return next;
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$pourColor(int next) {
        if (this.pourColor != next) {
            this.pourColor = next;
            if ((512) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 512);
        }
    }

    public final void hbm$syncSet$ArcFurnaceBlockEntity$pourLength(float next) {
        if (Float.floatToRawIntBits(this.pourLength) != Float.floatToRawIntBits(next)) {
            this.pourLength = next;
            if ((512) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 512);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(ArcFurnaceBlockEntity.class, "electrodes", 3, 128);
        com.hbm.packet.SyncBindings.field(ArcFurnaceBlockEntity.class, "liquids", 3, 256);
        com.hbm.packet.SyncBindings.field(ArcFurnaceBlockEntity.class, "upgrades", 3, 0L);
        com.hbm.packet.SyncInventory.slots(
                ArcFurnaceBlockEntity.class, new int[] {}, 0L, true, true);
    }
}
