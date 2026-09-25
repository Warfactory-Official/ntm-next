// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.ContainerSync;
import com.hbm.inventory.container.MenuMachineCrystallizer;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncSlots;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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

@SyncSlots(
        value = {0},
        units = 1L << 0)
public class BlockEntityMachineCrystallizer extends BlockEntityMachineBase
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                IFluidCopiable,
                MenuProvider,
                IUpgradeInfoProvider,
                SyncUnitSchema {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SLOT_FLUID_IN = 3;
    public static final int SLOT_FLUID_OUT = 4;
    public static final int SLOT_UPGRADE_START = 5;
    public static final int SLOT_UPGRADE_END = 6;
    public static final int SLOT_FLUID_ID = 7;
    public static final int SLOT_COUNT = 8;

    public static final long MAX_POWER = 1_000_000L;
    public static final int DEMAND = 1000;
    public static final int TANK_CAPACITY = 8000;

    private static final int[] ACCESSIBLE_SLOTS = {SLOT_INPUT, SLOT_OUTPUT};
    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.EFFECT, 3, UpgradeType.OVERDRIVE, 3);

    public static Consumer<BlockEntityMachineCrystallizer> CLIENT_SOUND = be -> {};

    @SyncField(units = 1L << 3)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.PEROXIDE, TANK_CAPACITY);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] allTanks = {tank};

    @SyncField(units = 1L << 0)
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 1)
    @ContainerSync
    public long power;

    @SyncField(units = 1L << 0)
    public short progress;

    public short duration = 600;

    @SyncField(units = 1L << 2)
    public boolean isOn;

    public float angle;
    public float prevAngle;

    public BlockEntityMachineCrystallizer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ACIDOMATIC.get(), pos, state, SLOT_COUNT);
        receiving = new FluidTankNTM[] {tank};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.crystallizer");
    }

    @Override
    public void tickServer() {
        isOn = false;

        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        boolean changed = tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, inventory);

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);

        for (int i = 0; i < getCycleCount(); i++) {
            if (canProcess()) {
                progress++;
                power -= getPowerRequired();
                isOn = true;
                if (progress > getDuration()) {
                    progress = 0;
                    processItem();
                    changed = true;
                }
            } else {
                progress = 0;
            }
        }

        if (changed) setChanged();
        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        CLIENT_SOUND.accept(this);
        prevAngle = angle;
        if (isOn) {
            angle += 5F * getCycleCount();
            if (angle >= 360) {
                angle -= 360;
                prevAngle -= 360;
            }
            if (level.getRandom().nextInt(20) == 0) {
                level.addParticle(
                        ParticleTypes.CLOUD,
                        worldPosition.getX() + level.getRandom().nextDouble(),
                        worldPosition.getY() + 6.5D,
                        worldPosition.getZ() + level.getRandom().nextDouble(),
                        0.0,
                        0.1,
                        0.0);
            }
        }
    }

    private boolean canProcess() {
        ItemStack in = inventory.get(SLOT_INPUT);
        if (in.isEmpty()) return false;
        if (power < getPowerRequired()) return false;

        CrystallizerRecipe result = CrystallizerRecipes.INSTANCE.getOutput(in, tank.getTankType());
        if (result == null) return false;
        if (in.getCount() < result.itemAmount()) return false;
        if (tank.getFill() < getRequiredAcid(result.acidAmount())) return false;

        ItemStack out = inventory.get(SLOT_OUTPUT);
        if (!out.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(out, result.output())) return false;
            return out.getCount() + result.output().getCount() <= out.getMaxStackSize();
        }
        return true;
    }

    private void processItem() {
        ItemStack in = inventory.get(SLOT_INPUT);
        CrystallizerRecipe result = CrystallizerRecipes.INSTANCE.getOutput(in, tank.getTankType());
        if (result == null) return;

        ItemStack out = result.output().copy();
        ItemStack current = inventory.get(SLOT_OUTPUT);
        if (current.isEmpty()) inventory.set(SLOT_OUTPUT, out);
        else if (current.getCount() + out.getCount() <= current.getMaxStackSize())
            current.grow(out.getCount());

        tank.setFill(tank.getFill() - getRequiredAcid(result.acidAmount()));

        float freeChance = getFreeChance(result);
        if (freeChance == 0 || freeChance < level.getRandom().nextFloat()) {
            in.shrink(result.itemAmount());
            if (in.isEmpty()) inventory.set(SLOT_INPUT, ItemStack.EMPTY);
        }
    }

    public int getRequiredAcid(int base) {
        return base;
    }

    public float getFreeChance(CrystallizerRecipe recipe) {
        int effect = upgradeManager.getLevel(UpgradeType.EFFECT);
        return effect > 0 ? Math.min(effect * recipe.productivity, 0.99F) : 0F;
    }

    public short getDuration() {
        CrystallizerRecipe result =
                CrystallizerRecipes.INSTANCE.getOutput(
                        inventory.get(SLOT_INPUT), tank.getTankType());
        int base = result != null ? result.duration : 600;
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        return speed > 0
                ? (short) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.25F))
                : (short) base;
    }

    public int getPowerRequired() {
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int effect = upgradeManager.getLevel(UpgradeType.EFFECT);
        return DEMAND + speed * DEMAND + effect * DEMAND * 2;
    }

    public int getCycleCount() {
        int overdrive = upgradeManager.getLevel(UpgradeType.OVERDRIVE);
        return Math.min(1 + overdrive * 2, 7);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.CHEMICAL_PLANT_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1F,
                15F,
                0.75F,
                15);
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
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return allTanks;
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT ->
                    CrystallizerRecipes.INSTANCE.getOutput(stack, tank.getTankType()) != null;
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> ItemMachineUpgrade.isUpgrade(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> true;
        };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_INPUT
                && CrystallizerRecipes.INSTANCE.getOutput(stack, tank.getTankType()) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_OUTPUT;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineCrystallizer(containerId, playerInventory, this);
    }

    private void writeProgress(ByteBuf output) {
        output.writeShort(progress);
        output.writeShort(getDuration());
    }

    private void readProgress(ByteBuf input) {
        progress = input.readShort();
        duration = input.readShort();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0xfL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeProgress(output);
            case 1 -> output.writeLong(this.power);
            case 2 -> output.writeBoolean(this.isOn);
            case 3 -> this.tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readProgress(input);
            case 1 -> this.power = input.readLong();
            case 2 -> this.isOn = input.readBoolean();
            case 3 -> this.tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
