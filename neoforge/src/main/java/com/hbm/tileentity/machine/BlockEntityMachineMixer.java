// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineMixer;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.MixerRecipe;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.FluidIdentifierData;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineMixer extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_ITEM_INPUT = 1;
    public static final int SLOT_FLUID_ID = 2;
    public static final int SLOT_UPGRADE_START = 3;
    public static final int SLOT_UPGRADE_END = 4;
    public static final int SLOT_COUNT = 5;

    public static final int TANK_IN1 = 0;
    public static final int TANK_IN2 = 1;
    public static final int TANK_OUT = 2;
    public static final int TANK_COUNT = 3;

    public static final long MAX_POWER = 10_000L;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_ITEM_INPUT};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 6);

    @SyncField(units = 1L << 5)
    public final FluidTankNTM[] tanks = new FluidTankNTM[TANK_COUNT];

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 2)
    public int progress;

    @SyncField(units = 1L << 1)
    public int processTime;

    @SyncField(units = 1L << 3)
    public int recipeIndex;

    @SyncField(units = 1L << 4)
    public boolean wasOn;

    public float rotation;
    public float prevRotation;
    private int consumption = 50;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineMixer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIXER.get(), pos, state, SLOT_COUNT);
        tanks[TANK_IN1] = new FluidTankNTM(16_000);
        tanks[TANK_IN2] = new FluidTankNTM(16_000);
        tanks[TANK_OUT] = new FluidTankNTM(24_000);
        receiving = new FluidTankNTM[] {tanks[0], tanks[1]};
        sending = new FluidTankNTM[] {tanks[2]};
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);

        ItemStack idStack = inventory.get(SLOT_FLUID_ID);
        if (idStack.getItem() instanceof FluidIdentifierItem) {
            Fluid identified =
                    idStack.getOrDefault(
                                    ModDataComponents.FLUID_IDENTIFIER.get(),
                                    FluidIdentifierData.EMPTY)
                            .primary();
            tanks[TANK_OUT].setTankTypeByIdentifier(identified);
        }

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        consumption = 50;
        consumption += speedLevel * 150;
        consumption -= consumption * powerLevel * 0.25;
        consumption *= (overLevel * 3 + 1);

        wasOn = canProcess();

        if (wasOn) {
            progress++;
            power -= consumption;

            processTime -= processTime * speedLevel / 4;
            processTime /= (overLevel + 1);
            if (processTime <= 0) processTime = 1;

            if (progress >= processTime) {
                process();
                progress = 0;
            }
        } else {
            progress = 0;
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(50);
    }

    @Override
    public void tickClient() {
        prevRotation = rotation;
        if (wasOn) rotation += 20F;
        if (rotation >= 360) {
            rotation -= 360;
            prevRotation -= 360;
        }
    }

    public boolean canProcess() {
        List<MixerRecipe> recipeSet =
                MixerRecipes.INSTANCE.getOutput(tanks[TANK_OUT].getTankType());
        if (recipeSet.isEmpty()) {
            recipeIndex = 0;
            return false;
        }

        recipeIndex = recipeIndex % recipeSet.size();
        MixerRecipe recipe = recipeSet.get(recipeIndex);

        tanks[TANK_IN1].setTankType(recipe.input1() != null ? recipe.input1().type() : null);
        tanks[TANK_IN2].setTankType(recipe.input2() != null ? recipe.input2().type() : null);

        if (recipe.input1() != null && tanks[TANK_IN1].getFill() < recipe.input1().amount())
            return false;
        if (recipe.input2() != null && tanks[TANK_IN2].getFill() < recipe.input2().amount())
            return false;

        if (power < consumption) return false;

        if (recipe.output() + tanks[TANK_OUT].getFill() > tanks[TANK_OUT].getMaxFill())
            return false;

        if (recipe.solidInput() != null
                && !recipe.solidInput().test(inventory.get(SLOT_ITEM_INPUT))) return false;

        processTime = recipe.duration;
        return true;
    }

    private void process() {
        List<MixerRecipe> recipeSet =
                MixerRecipes.INSTANCE.getOutput(tanks[TANK_OUT].getTankType());
        MixerRecipe recipe = recipeSet.get(recipeIndex % recipeSet.size());

        if (recipe.input1() != null)
            tanks[TANK_IN1].setFill(tanks[TANK_IN1].getFill() - (int) recipe.input1().amount());
        if (recipe.input2() != null)
            tanks[TANK_IN2].setFill(tanks[TANK_IN2].getFill() - (int) recipe.input2().amount());
        if (recipe.solidInput() != null) {
            ItemStack solid = inventory.get(SLOT_ITEM_INPUT);
            solid.shrink(recipe.solidInput().count());
            if (solid.isEmpty()) inventory.set(SLOT_ITEM_INPUT, ItemStack.EMPTY);
        }
        tanks[TANK_OUT].setFill(tanks[TANK_OUT].getFill() + recipe.output());
        setChanged();
    }

    public int getConsumption() {
        return consumption;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tanks[TANK_OUT];
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> ItemMachineUpgrade.isUpgrade(stack);
            case SLOT_ITEM_INPUT -> matchesSolidInput(stack);
            default -> true;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_ITEM_INPUT && matchesSolidInput(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    private boolean matchesSolidInput(ItemStack stack) {
        List<MixerRecipe> recipeSet =
                MixerRecipes.INSTANCE.getOutput(tanks[TANK_OUT].getTankType());
        if (recipeSet.isEmpty()) return false;
        MixerRecipe recipe = recipeSet.get(recipeIndex % recipeSet.size());
        return recipe.solidInput() != null && recipe.solidInput().matchesItem(stack);
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX() + 0.5D,
                                worldPosition.getY() + 0.5D,
                                worldPosition.getZ() + 0.5D)
                <= 16 * 16;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            recipeIndex++;
            setChanged();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineMixer");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineMixer(containerId, playerInventory, this);
    }

    private void writeTanks(ByteBuf output) {
        for (FluidTankNTM tank : tanks) tank.packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (FluidTankNTM tank : tanks) tank.packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("progress").ifPresent(v -> progress = v);
        input.getInt("processTime").ifPresent(v -> processTime = v);
        input.getInt("recipe").ifPresent(v -> recipeIndex = v);
        for (int i = 0; i < TANK_COUNT; i++) {
            input.child("t" + i).ifPresent(tanks[i]::deserialize);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putInt("processTime", processTime);
        output.putInt("recipe", recipeIndex);
        for (int i = 0; i < TANK_COUNT; i++) tanks[i].serialize(output.child("t" + i));
    }

    @Override
    public long syncUnitMask() {
        return 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.processTime);
            case 2 -> output.writeInt(this.progress);
            case 3 -> output.writeInt(this.recipeIndex);
            case 4 -> output.writeBoolean(this.wasOn);
            case 5 -> writeTanks(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.processTime = input.readInt();
            case 2 -> this.progress = input.readInt();
            case 3 -> this.recipeIndex = input.readInt();
            case 4 -> this.wasOn = input.readBoolean();
            case 5 -> readTanks(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
