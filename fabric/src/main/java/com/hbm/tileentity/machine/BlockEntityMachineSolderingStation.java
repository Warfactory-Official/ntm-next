// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineSolderingStation;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SolderingRecipe;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.ingredient.CountIngredient;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineSolderingStation extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_TOPPING_START = 0;
    public static final int SLOT_PCB_START = 3;
    public static final int SLOT_SOLDER = 5;
    public static final int SLOT_OUTPUT = 6;
    public static final int SLOT_BATTERY = 7;
    public static final int SLOT_FLUID_ID = 8;
    public static final int SLOT_UPGRADE_START = 9;
    public static final int SLOT_UPGRADE_END = 10;
    public static final int SLOT_COUNT = 11;

    public static final int TANK_CAPACITY = 8_000;
    private static final long DEFAULT_MAX_POWER = 2_000L;
    private static final long IDLE_CONSUMPTION = 100L;

    private static final int[] TOPPING_SLOTS = {0, 1, 2};
    private static final int[] PCB_SLOTS = {3, 4};
    private static final int[] SOLDER_SLOTS = {SLOT_SOLDER};
    private static final int[] ACCESSIBLE_SLOTS = {0, 1, 2, 3, 4, 5, SLOT_OUTPUT};

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);

    @SyncField(units = 1L << 7)
    public final FluidTankNTM tank;

    private final FluidTankNTM[] receiving;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 1)
    @ContainerSync
    public long maxPower = DEFAULT_MAX_POWER;

    @SyncField(units = 1L << 2)
    @ContainerSync
    public long consumption;

    @SyncField(units = 1L << 5)
    @ContainerSync
    public boolean collisionPrevention;

    @SyncField(units = 1L << 3)
    @ContainerSync
    public int progress;

    @SyncField(units = 1L << 4)
    @ContainerSync
    public int processTime = 1;

    public ItemStack display = ItemStack.EMPTY;

    @SyncField(units = 1L << 6)
    private @Nullable SolderingRecipe recipe;

    public BlockEntityMachineSolderingStation(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLDERING_STATION.get(), pos, state, SLOT_COUNT);
        this.tank = new FluidTankNTM(TANK_CAPACITY);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);
        tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overdriveLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        this.recipe =
                SolderingRecipes.INSTANCE.getRecipe(this, TOPPING_SLOTS, PCB_SLOTS, SLOT_SOLDER);
        long intendedMaxPower;

        if (recipe != null) {
            processTime =
                    recipe.duration()
                            - recipe.duration() * speedLevel / 6
                            + recipe.duration() * powerLevel / 3;
            consumption =
                    recipe.consumption()
                            + recipe.consumption() * speedLevel
                            - recipe.consumption() * powerLevel / 6;
            consumption *= (long) Math.pow(2, overdriveLevel);
            intendedMaxPower = consumption * 20L;

            if (canProcess(recipe)) {
                progress += 1 + overdriveLevel;
                power -= consumption;
                if (progress >= processTime) {
                    progress = 0;
                    consumeAndProduce(recipe);
                }

                if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                    ServerLevel server = (ServerLevel) level;
                    Direction dir = BlockMultiblockCore.coreFacing(getBlockState());
                    Direction rot = dir.getClockWise();
                    double px =
                            worldPosition.getX()
                                    + 0.5
                                    - dir.getStepX() * 0.5
                                    + rot.getStepX() * 0.5;
                    double py = worldPosition.getY() + 1.125;
                    double pz =
                            worldPosition.getZ()
                                    + 0.5
                                    - dir.getStepZ() * 0.5
                                    + rot.getStepZ() * 0.5;
                    ParticleCreators.sparks(server, px, py, pz, 3, false, 25);
                }
            } else {
                progress = 0;
            }
        } else {
            progress = 0;
            consumption = IDLE_CONSUMPTION;
            intendedMaxPower = DEFAULT_MAX_POWER;
        }

        maxPower = Math.max(intendedMaxPower, power);
        networkPackNT(25);
    }

    private boolean canProcess(SolderingRecipe recipe) {
        if (power < consumption) return false;
        if (recipe.fluid() != null && !recipe.fluid().matches(tank)) return false;
        if (collisionPrevention && recipe.fluid() == null && tank.getFill() > 0) return false;

        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (!out.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(out, recipe.output())) return false;
            return out.getCount() + recipe.output().getCount() <= out.getMaxStackSize();
        }
        return true;
    }

    private void consumeAndProduce(SolderingRecipe recipe) {
        consumeGroup(TOPPING_SLOTS, recipe.toppings());
        consumeGroup(PCB_SLOTS, recipe.pcb());
        consumeGroup(SOLDER_SLOTS, recipe.solder());

        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (out.isEmpty()) {
            inventory.set(SLOT_OUTPUT, recipe.output().copy());
        } else {
            out.grow(recipe.output().getCount());
        }

        if (recipe.fluid() != null) recipe.fluid().drainFrom(tank);
        setChanged();
    }

    private void consumeGroup(int[] slots, CountIngredient[] group) {
        for (CountIngredient ing : group) {
            for (int slot : slots) {
                ItemStack stack = inventory.get(slot);
                if (ing.test(stack)) {
                    stack.shrink(ing.count());
                    break;
                }
            }
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_OUTPUT) return false;
        if (slot == SLOT_BATTERY) return IBatteryItem.isBattery(stack);
        if (slot == SLOT_UPGRADE_START || slot == SLOT_UPGRADE_END)
            return ItemMachineUpgrade.isUpgrade(stack);
        if (slot >= SLOT_TOPPING_START && slot < SLOT_PCB_START) {
            return isValidForGroup(TOPPING_SLOTS, slot, stack, SolderingRecipes.toppings());
        }
        if (slot >= SLOT_PCB_START && slot < SLOT_SOLDER) {
            return isValidForGroup(PCB_SLOTS, slot, stack, SolderingRecipes.pcb());
        }
        if (slot == SLOT_SOLDER) {
            return isValidForGroup(SOLDER_SLOTS, slot, stack, SolderingRecipes.solder());
        }
        return false;
    }

    private boolean isValidForGroup(
            int[] groupSlots, int slot, ItemStack stack, List<CountIngredient> known) {
        for (int i : groupSlots) {

            if (i != slot
                    && !inventory.get(i).isEmpty()
                    && ItemStack.isSameItemSameComponents(inventory.get(i), stack)) return false;
        }
        for (CountIngredient ing : known) if (ing.ingredient().test(stack)) return true;
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot != SLOT_OUTPUT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public long getPower() {
        return Math.max(0L, Math.min(power, maxPower));
    }

    @Override
    public void setPower(long p) {
        power = p;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        collisionPrevention = !collisionPrevention;
        setChanged();
    }

    private void writeDisplay(ByteBuf output) {
        boolean hasRecipe = recipe != null;
        output.writeBoolean(hasRecipe);
        if (hasRecipe) output.writeInt(BuiltInRegistries.ITEM.getId(recipe.previewItem()));
    }

    private void readDisplay(ByteBuf input) {
        display =
                input.readBoolean()
                        ? new ItemStack(BuiltInRegistries.ITEM.byId(input.readInt()))
                        : ItemStack.EMPTY;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSolderingStation");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineSolderingStation(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getLong("power").ifPresent(v -> power = v);
        input.getLong("maxPower").ifPresent(v -> maxPower = v);
        input.getLong("consumption").ifPresent(v -> consumption = v);
        input.getInt("progress").ifPresent(v -> progress = v);
        input.getInt("processTime").ifPresent(v -> processTime = v);
        collisionPrevention = input.getBooleanOr("collisionPrevention", collisionPrevention);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        output.putLong("consumption", consumption);
        output.putInt("progress", progress);
        output.putInt("processTime", processTime);
        output.putBoolean("collisionPrevention", collisionPrevention);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0xffL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeLong(this.maxPower);
            case 2 -> output.writeLong(this.consumption);
            case 3 -> output.writeInt(this.progress);
            case 4 -> output.writeInt(this.processTime);
            case 5 -> output.writeBoolean(this.collisionPrevention);
            case 6 -> writeDisplay(output);
            case 7 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.maxPower = input.readLong();
            case 2 -> this.consumption = input.readLong();
            case 3 -> this.progress = input.readInt();
            case 4 -> this.processTime = input.readInt();
            case 5 -> this.collisionPrevention = input.readBoolean();
            case 6 -> readDisplay(input);
            case 7 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
