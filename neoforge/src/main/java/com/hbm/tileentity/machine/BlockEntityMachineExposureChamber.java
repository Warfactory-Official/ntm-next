// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineExposureChamber;
import com.hbm.inventory.recipes.ExposureChamberRecipe;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IUpgradeInfoProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class BlockEntityMachineExposureChamber extends BlockEntityMachineBase
        implements IEnergyHandlerMK2, MenuProvider, IUpgradeInfoProvider, SyncUnitSchema {

    public static final int SLOT_PARTICLE = 0;

    public static final int SLOT_PARTICLE_INTERNAL = 1;
    public static final int SLOT_CONTAINER = 2;
    public static final int SLOT_INGREDIENT = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOT_BATTERY = 5;
    public static final int SLOT_UPGRADE_START = 6;
    public static final int SLOT_UPGRADE_END = 7;
    public static final int SLOT_COUNT = 8;

    public static final long MAX_POWER = 1_000_000L;
    public static final int PROCESS_TIME_BASE = 200;
    public static final int CONSUMPTION_BASE = 10_000;
    public static final int MAX_PARTICLES = 8;

    private static final int[] ACCESSIBLE_SLOTS = {
        SLOT_PARTICLE, SLOT_CONTAINER, SLOT_INGREDIENT, SLOT_OUTPUT
    };
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);

    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 4)
    public long power;

    @SyncField(units = 1L << 1)
    public int progress;

    @SyncField(units = 1L << 2)
    public int processTime = PROCESS_TIME_BASE;

    @SyncField(units = 1L << 3)
    public int consumption = CONSUMPTION_BASE;

    @SyncField(units = 1L << 5)
    public int savedParticles;

    @SyncField(units = 1L << 0)
    public boolean isOn;

    public float rotation;
    public float prevRotation;

    public BlockEntityMachineExposureChamber(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXPOSURE_CHAMBER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.exposureChamber");
    }

    @Override
    public void tickServer() {
        isOn = false;
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overdriveLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        consumption = CONSUMPTION_BASE;
        processTime = PROCESS_TIME_BASE - PROCESS_TIME_BASE / 4 * speedLevel;
        consumption *= speedLevel / 2 + 1;
        processTime *= powerLevel / 2 + 1;
        consumption /= powerLevel + 1;
        processTime /= overdriveLevel + 1;
        consumption *= overdriveLevel * 2 + 1;

        boolean changed = drawParticle();
        changed |= expose();

        if (savedParticles <= 0) inventory.set(SLOT_PARTICLE_INTERNAL, ItemStack.EMPTY);

        if (changed) setChanged();
        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;

        if (isOn) {
            rotation += 10F;

            if (rotation >= 720F) {
                rotation -= 720F;
                prevRotation -= 720F;
            }
        }
    }

    private boolean drawParticle() {
        if (!inventory.get(SLOT_PARTICLE_INTERNAL).isEmpty()) return false;
        ItemStack particle = inventory.get(SLOT_PARTICLE);
        if (particle.isEmpty() || inventory.get(SLOT_INGREDIENT).isEmpty() || savedParticles > 0)
            return false;
        if (ExposureChamberRecipes.getRecipe(particle, inventory.get(SLOT_INGREDIENT)) == null)
            return false;

        ItemStackTemplate remainder = particle.getCraftingRemainder();
        ItemStack container = remainder == null ? ItemStack.EMPTY : remainder.create();
        ItemStack stored = inventory.get(SLOT_CONTAINER);

        if (!container.isEmpty()) {
            if (stored.isEmpty()) {
                inventory.set(SLOT_CONTAINER, container.copy());
            } else if (ItemStack.isSameItemSameComponents(stored, container)
                    && stored.getCount() < stored.getMaxStackSize()) {
                stored.grow(1);
            } else {
                return false;
            }
        }

        inventory.set(SLOT_PARTICLE_INTERNAL, particle.copyWithCount(1));
        removeItem(SLOT_PARTICLE, 1);
        savedParticles = MAX_PARTICLES;
        return true;
    }

    private boolean expose() {
        ExposureChamberRecipe recipe = currentRecipe();

        if (recipe == null || savedParticles <= 0 || power < consumption || !outputFits(recipe)) {
            progress = 0;
            return false;
        }

        progress++;
        power -= consumption;
        isOn = true;

        if (progress < processTime) return false;

        progress = 0;
        savedParticles--;
        removeItem(SLOT_INGREDIENT, 1);

        ItemStack out = recipe.output();
        ItemStack current = inventory.get(SLOT_OUTPUT);
        if (current.isEmpty()) inventory.set(SLOT_OUTPUT, out.copy());
        else current.grow(out.getCount());
        return true;
    }

    private @Nullable ExposureChamberRecipe currentRecipe() {
        ItemStack loaded = inventory.get(SLOT_PARTICLE_INTERNAL);
        if (loaded.isEmpty()) return null;
        return ExposureChamberRecipes.getRecipe(loaded, inventory.get(SLOT_INGREDIENT));
    }

    private boolean outputFits(ExposureChamberRecipe recipe) {
        ItemStack out = recipe.output();
        ItemStack current = inventory.get(SLOT_OUTPUT);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(current, out)) return false;
        return current.getCount() + out.getCount() <= current.getMaxStackSize();
    }

    @Override
    public boolean dropsSlot(int slot) {
        return slot != SLOT_PARTICLE_INTERNAL;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        ItemStack ingredient = inventory.get(SLOT_INGREDIENT);
        if (slot == SLOT_PARTICLE && !inventory.get(SLOT_PARTICLE).isEmpty()) return true;
        if (slot == SLOT_INGREDIENT && !ingredient.isEmpty()) return true;

        ItemStack loaded = inventory.get(SLOT_PARTICLE_INTERNAL);
        ItemStack particle = loaded.isEmpty() ? inventory.get(SLOT_PARTICLE) : loaded;

        if (slot == SLOT_PARTICLE && particle.isEmpty() && !ingredient.isEmpty()) {
            return ExposureChamberRecipes.getRecipe(stack, ingredient) != null;
        }
        if (slot == SLOT_INGREDIENT && !particle.isEmpty() && ingredient.isEmpty()) {
            return ExposureChamberRecipes.getRecipe(inventory.get(SLOT_PARTICLE), stack) != null;
        }
        if (particle.isEmpty() && ingredient.isEmpty()) {
            if (slot == SLOT_PARTICLE) return ExposureChamberRecipes.anyRowNames(stack, true);
            if (slot == SLOT_INGREDIENT) return ExposureChamberRecipes.anyRowNames(stack, false);
        }
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_CONTAINER || slot == SLOT_OUTPUT;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineExposureChamber(containerId, playerInventory, this);
    }

    private void writeSavedParticles(ByteBuf output) {
        output.writeByte(savedParticles);
    }

    private void readSavedParticles(ByteBuf input) {
        savedParticles = input.readByte();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getInt("progress").ifPresent(v -> progress = v);
        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("savedParticles").ifPresent(v -> savedParticles = v);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", progress);
        output.putLong("power", power);
        output.putInt("savedParticles", savedParticles);
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeBoolean(this.isOn);
            case 1 -> output.writeInt(this.progress);
            case 2 -> output.writeInt(this.processTime);
            case 3 -> output.writeInt(this.consumption);
            case 4 -> output.writeLong(this.power);
            case 5 -> writeSavedParticles(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.isOn = input.readBoolean();
            case 1 -> this.progress = input.readInt();
            case 2 -> this.processTime = input.readInt();
            case 3 -> this.consumption = input.readInt();
            case 4 -> this.power = input.readLong();
            case 5 -> readSavedParticles(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
