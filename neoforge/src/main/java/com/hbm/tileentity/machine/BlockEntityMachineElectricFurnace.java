// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.machine.MachineElectricFurnace;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineElectricFurnace;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntitySmeltingFurnace;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineElectricFurnace extends BlockEntitySmeltingFurnace
        implements IEnergyHandlerMK2, MenuProvider, IUpgradeInfoProvider, SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_UPGRADE = 3;
    public static final int SLOT_COUNT = 4;

    public static final long MAX_POWER = 100_000L;
    public static final int BASE_PROCESS_TIME = 100;
    public static final long BASE_CONSUMPTION = 50L;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_BATTERY, SLOT_INPUT, SLOT_OUTPUT};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.SPEED, 3, UpgradeType.POWER, 3);
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);
    private final UpgradeManager upgradeManager = new UpgradeManager(this);
    @ContainerSync public long power;

    @SyncField(units = 1L << 0)
    @ContainerSync
    public int progress;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public int maxProgress = BASE_PROCESS_TIME;

    private int cooldown;

    public BlockEntityMachineElectricFurnace(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE.get(), pos, state, SLOT_COUNT, SLOT_OUTPUT);
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
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) return hasSmeltingResult(stack);
        return slot != SLOT_OUTPUT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
        return slot == SLOT_INPUT && hasSmeltingResult(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == SLOT_OUTPUT) return true;

        if (slot == SLOT_BATTERY)
            return stack.getItem() instanceof IBatteryItem battery
                    && battery.getCharge(stack) == 0L;
        return false;
    }

    @Override
    public void tickServer() {
        long prevPower = power;
        int prevProgress = progress;

        if (cooldown > 0) cooldown--;

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        upgradeManager.scan(this, SLOT_UPGRADE, SLOT_UPGRADE);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);

        long consumption = BASE_CONSUMPTION + speedLevel * 50L - powerLevel * 15L;
        maxProgress = Math.max(1, BASE_PROCESS_TIME - speedLevel * 25 + powerLevel * 10);

        if (power < consumption) cooldown = 20;

        boolean working = power >= consumption && canProcess();
        if (working) {
            progress++;
            power = Math.max(0L, power - consumption);
            if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                PollutionHandler.incrementPollution(
                        level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
            }
            if (progress >= maxProgress) {
                progress = 0;
                processItem();
            }
        } else {
            progress = 0;
        }

        boolean shouldBeLit = progress > 0;
        BlockState curState = level.getBlockState(worldPosition);
        if (curState.hasProperty(MachineElectricFurnace.LIT)
                && curState.getValue(MachineElectricFurnace.LIT) != shouldBeLit) {
            level.setBlock(
                    worldPosition, curState.setValue(MachineElectricFurnace.LIT, shouldBeLit), 2);
        }

        if (power != prevPower || progress != prevProgress) setChanged();
        networkPackNT(50);
    }

    private @Nullable RecipeHolder<SmeltingRecipe> smeltRecipe() {
        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.isEmpty()) return null;
        if (cooldown > 0) return null;
        return quickCheck.getRecipeFor(new SingleRecipeInput(in), (ServerLevel) level).orElse(null);
    }

    private @Nullable ItemStack smeltResult() {
        RecipeHolder<SmeltingRecipe> recipe = smeltRecipe();
        if (recipe == null) return null;
        return recipe.value().assemble(new SingleRecipeInput(inventory.get(SLOT_INPUT)));
    }

    private boolean canProcess() {
        ItemStack result = smeltResult();
        if (result == null || result.isEmpty()) return false;
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(out, result)) return false;

        return out.getCount() < out.getMaxStackSize();
    }

    private void processItem() {
        RecipeHolder<SmeltingRecipe> recipe = smeltRecipe();
        if (recipe == null) return;
        ItemStack result =
                recipe.value().assemble(new SingleRecipeInput(inventory.get(SLOT_INPUT)));
        if (result.isEmpty()) return;
        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) inventory.set(SLOT_OUTPUT, result.copy());
        else out.grow(result.getCount());
        inventory.get(SLOT_INPUT).shrink(1);
        recipesUsed.record(0, recipe);
        setChanged();
    }

    private boolean hasSmeltingResult(ItemStack stack) {
        if (stack.isEmpty() || !(level instanceof ServerLevel server)) return false;
        return quickCheck.getRecipeFor(new SingleRecipeInput(stack), server).isPresent();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.electricFurnace");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineElectricFurnace(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
    }

    @Override
    public long syncUnitMask() {
        return 0x3L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.progress);
            case 1 -> output.writeInt(this.maxProgress);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.progress = input.readInt();
            case 1 -> this.maxProgress = input.readInt();
            default -> throw new IllegalArgumentException();
        }
    }
}
