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

package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.EnergyHandler;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blockentity.UpgradeInfoProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ExposureChamberMenu;
import com.hbm.inventory.recipes.ExposureChamberRecipe;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.loader.Outputs;
import com.hbm.items.machine.upgrade.UpgradeInfo;
import com.hbm.items.machine.upgrade.UpgradeLimits;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncUnitSchema;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class ExposureChamberBlockEntity extends MachineBlockEntity
        implements EnergyHandler, MenuProvider, UpgradeInfoProvider, SyncUnitSchema {

    public static final int CAPSULE_SLOT = 0;
    public static final int MARKER_SLOT = 1;
    public static final int CONTAINER_SLOT = 2;
    public static final int MATERIAL_SLOT = 3;
    public static final int PRODUCT_SLOT = 4;
    public static final int BATTERY_SLOT = 5;
    public static final int FIRST_UPGRADE_SLOT = 6;
    public static final int LAST_UPGRADE_SLOT = 7;
    public static final int INVENTORY_SIZE = 8;
    public static final int CHARGES_PER_CAPSULE = 8;
    public static final long ENERGY_CAPACITY = 1_000_000L;
    public static final int BASE_DURATION = 200;
    public static final int BASE_TICK_COST = 10_000;

    private static final int[] AUTOMATION_SLOTS = {
        CAPSULE_SLOT, CONTAINER_SLOT, MATERIAL_SLOT, PRODUCT_SLOT
    };
    private static final UpgradeLimits UPGRADES =
            UpgradeLimits.of(UpgradeType.SPEED, 3)
                    .and(UpgradeType.POWER, 3)
                    .and(UpgradeType.OVERDRIVE, 3);

    private final UpgradeManager upgrades = new UpgradeManager(this);
    private @Nullable ItemStackTemplate remainderDefinition;
    private @Nullable DataComponentMap remainderBase;
    private @Nullable DataComponentMap remainderState;

    public boolean running;
    public int work;
    public int duration;

    {
        hbm$syncSet$ExposureChamberBlockEntity$duration(BASE_DURATION);
    }

    public int tickCost;

    {
        hbm$syncSet$ExposureChamberBlockEntity$tickCost(BASE_TICK_COST);
    }

    public long energy;
    public int charges;
    public float angle;
    public float previousAngle;

    public ExposureChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPOSURE_CHAMBER.get(), pos, state, INVENTORY_SIZE);
    }

    @Override
    public void tickServer() {
        hbm$syncSet$ExposureChamberBlockEntity$running(false);
        long received =
                ItemEnergyTransfer.extract(this, BATTERY_SLOT, ENERGY_CAPACITY - energy, false);
        if (received != 0L) {
            hbm$syncSet$ExposureChamberBlockEntity$energy((long) (energy + (received)));
            setChanged();
        }
        upgrades.scan(this, FIRST_UPGRADE_SLOT, LAST_UPGRADE_SLOT);
        int speed = upgrades.getLevel(UpgradeType.SPEED);
        int efficiency = upgrades.getLevel(UpgradeType.POWER);
        int overdrive = upgrades.getLevel(UpgradeType.OVERDRIVE);
        hbm$syncSet$ExposureChamberBlockEntity$duration(
                (BASE_DURATION - 50 * speed) * (efficiency / 2 + 1) / (overdrive + 1));
        hbm$syncSet$ExposureChamberBlockEntity$tickCost(
                BASE_TICK_COST * (speed / 2 + 1) / (efficiency + 1) * (2 * overdrive + 1));

        ExposureChamberRecipe recipe;
        if (inventory.get(MARKER_SLOT).isEmpty()) {
            recipe =
                    ExposureChamberRecipes.getRecipe(
                            inventory.get(CAPSULE_SLOT), inventory.get(MATERIAL_SLOT));
            if (charges > 0 || recipe == null || !loadCapsule()) recipe = null;
        } else {
            recipe = loadedRecipe();
        }

        if (recipe != null && charges > 0 && energy >= tickCost && acceptsProduct(recipe)) {
            hbm$syncSet$ExposureChamberBlockEntity$energy((long) (energy - (tickCost)));
            hbm$syncSet$ExposureChamberBlockEntity$work((int) (work + 1));
            hbm$syncSet$ExposureChamberBlockEntity$running(true);
            if (work >= duration) {
                hbm$syncSet$ExposureChamberBlockEntity$work(0);
                hbm$syncSet$ExposureChamberBlockEntity$charges((int) (charges - 1));
                removeItem(MATERIAL_SLOT, 1);
                var result = recipe.output();
                var product = inventory.get(PRODUCT_SLOT);
                if (product.isEmpty()) inventory.set(PRODUCT_SLOT, result.copy());
                else product.grow(result.getCount());
            }
            setChanged();
        } else if (work != 0) {
            hbm$syncSet$ExposureChamberBlockEntity$work(0);
            setChanged();
        }
        if (charges <= 0 && !inventory.get(MARKER_SLOT).isEmpty()) {
            inventory.set(MARKER_SLOT, ItemStack.EMPTY);
            setChanged();
        }
        networkPackNT(50);
    }

    private boolean loadCapsule() {
        var capsule = inventory.get(CAPSULE_SLOT);
        var remainder = capsule.getCraftingRemainder();
        var returned = inventory.get(CONTAINER_SLOT);
        if (remainder != null) {
            if (returned.isEmpty()) {
                inventory.set(CONTAINER_SLOT, remainder.create());
            } else {
                if (returned.getCount() >= returned.getMaxStackSize()
                        || !sameRemainder(returned, remainder)) {
                    return false;
                }
                returned.grow(1);
            }
        }
        inventory.set(MARKER_SLOT, capsule.copyWithCount(1));
        removeItem(CAPSULE_SLOT, 1);
        hbm$syncSet$ExposureChamberBlockEntity$charges(CHARGES_PER_CAPSULE);
        setChanged();
        return true;
    }

    private boolean sameRemainder(ItemStack returned, ItemStackTemplate template) {
        if (!returned.is(template.item())) return false;
        var base = template.item().value().components();
        if (template != remainderDefinition || base != remainderBase) {
            remainderDefinition = template;
            remainderBase = base;
            remainderState = PatchedDataComponentMap.fromPatch(base, template.components());
        }
        return returned.getComponents().equals(remainderState);
    }

    private @Nullable ExposureChamberRecipe loadedRecipe() {
        return ExposureChamberRecipes.getRecipe(
                inventory.get(MARKER_SLOT), inventory.get(MATERIAL_SLOT));
    }

    private boolean acceptsProduct(ExposureChamberRecipe recipe) {
        return Outputs.fits(inventory.get(PRODUCT_SLOT), recipe.outputItems(), 0);
    }

    @Override
    public void tickClient() {
        previousAngle = angle;
        if (running) {
            angle += 10F;
            if (angle >= 720F) {
                angle -= 720F;
                previousAngle -= 720F;
            }
        }
    }

    @Override
    public boolean dropsSlot(int slot) {
        return slot != MARKER_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == CONTAINER_SLOT || slot == PRODUCT_SLOT;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != CAPSULE_SLOT && slot != MATERIAL_SLOT) return false;
        if (!inventory.get(slot).isEmpty()) return true;
        var loaded = inventory.get(MARKER_SLOT);
        var external = inventory.get(CAPSULE_SLOT);
        var particle = loaded.isEmpty() ? external : loaded;
        if (slot == CAPSULE_SLOT) {
            if (!particle.isEmpty()) return false;
            var material = inventory.get(MATERIAL_SLOT);
            return material.isEmpty()
                    ? ExposureChamberRecipes.anyRowNames(stack, true)
                    : ExposureChamberRecipes.getRecipe(stack, material) != null;
        }
        return particle.isEmpty()
                ? ExposureChamberRecipes.anyRowNames(stack, false)
                : ExposureChamberRecipes.getRecipe(particle, stack) != null;
    }

    @Override
    public long getPower() {
        return energy;
    }

    @Override
    public long getMaxPower() {
        return ENERGY_CAPACITY;
    }

    @Override
    public void setPower(long stored) {
        long next = Math.clamp(stored, 0L, ENERGY_CAPACITY);
        if (energy != next) {
            hbm$syncSet$ExposureChamberBlockEntity$energy(next);
            setChanged();
        }
    }

    @Override
    public UpgradeLimits upgradeCaps() {
        return UPGRADES;
    }

    @Override
    public void appendUpgradeInfo(UpgradeType type, int level, List<Component> tooltip) {
        if (type == UpgradeType.SPEED)
            UpgradeInfo.speedAndPower(tooltip, -25 * level, 100 * (level / 2));
        else if (type == UpgradeType.POWER) {
            UpgradeInfo.powerAndDelay(tooltip, UpgradeInfo.reduction(level + 1), 100 * (level / 2));
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.exposureChamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ExposureChamberMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        hbm$syncSet$ExposureChamberBlockEntity$energy(input.getLongOr("power", 0L));
        hbm$syncSet$ExposureChamberBlockEntity$work(input.getIntOr("progress", 0));
        hbm$syncSet$ExposureChamberBlockEntity$charges(input.getIntOr("savedParticles", 0));
        requireChargeCount(charges);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", energy);
        output.putInt("progress", work);
        output.putInt("savedParticles", charges);
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(running);
            case 1 -> output.writeInt(work);
            case 2 -> output.writeInt(duration);
            case 3 -> output.writeInt(tickCost);
            case 4 -> output.writeLong(energy);
            case 5 -> output.writeByte(charges);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> hbm$syncSet$ExposureChamberBlockEntity$running(input.readBoolean());
            case 1 -> hbm$syncSet$ExposureChamberBlockEntity$work(input.readInt());
            case 2 -> hbm$syncSet$ExposureChamberBlockEntity$duration(input.readInt());
            case 3 -> hbm$syncSet$ExposureChamberBlockEntity$tickCost(input.readInt());
            case 4 -> hbm$syncSet$ExposureChamberBlockEntity$energy(input.readLong());
            case 5 -> {
                hbm$syncSet$ExposureChamberBlockEntity$charges(input.readByte());
                requireChargeCount(charges);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    private static void requireChargeCount(int count) {
        if (count < 0 || count > CHARGES_PER_CAPSULE)
            throw new IllegalArgumentException("Particle charge count " + count);
    }

    public final void hbm$syncSet$ExposureChamberBlockEntity$running(boolean next) {
        if (this.running != next) {
            this.running = next;
            if ((1L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 1L);
        }
    }

    public final void hbm$syncSet$ExposureChamberBlockEntity$work(int next) {
        if (this.work != next) {
            this.work = next;
            if ((2L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2L);
        }
    }

    public final void hbm$syncSet$ExposureChamberBlockEntity$duration(int next) {
        if (this.duration != next) {
            this.duration = next;
            if ((4L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4L);
        }
    }

    public final void hbm$syncSet$ExposureChamberBlockEntity$tickCost(int next) {
        if (this.tickCost != next) {
            this.tickCost = next;
            if ((8L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8L);
        }
    }

    public final void hbm$syncSet$ExposureChamberBlockEntity$energy(long next) {
        if (this.energy != next) {
            this.energy = next;
            if ((16L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16L);
        }
    }

    public final void hbm$syncSet$ExposureChamberBlockEntity$charges(int next) {
        if (this.charges != next) {
            this.charges = next;
            if ((32L) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 32L);
        }
    }
}
