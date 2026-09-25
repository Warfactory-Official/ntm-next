// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidFlushSender;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.inventory.container.MenuMachineCompressor;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CompressorRecipe;
import com.hbm.inventory.recipes.CompressorRecipes;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.Audible;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineCompressor extends BlockEntityMachineBase
        implements Audible,
                IEnergyHandlerMK2,
                FluidFlushSender,
                MenuProvider,
                IUpgradeInfoProvider,
                IControlReceiver,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_START = 2;
    public static final int SLOT_UPGRADE_END = 3;
    public static final int SLOT_COUNT = 4;

    public static final long maxPower = 100_000L;
    public static final int processTimeBase = 100;
    public static final int powerRequirementBase = 2_500;

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(
                    UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 9);

    @SyncField(units = 1L << 4)
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];

    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 3)
    public long power;

    @SyncField(units = 1L << 5)
    public boolean isOn;

    @SyncField(units = 1L << 0)
    public int progress;

    @SyncField(units = 1L << 1)
    public int processTime = processTimeBase;

    @SyncField(units = 1L << 2)
    public int powerRequirement;

    public float fanSpin, prevFanSpin;
    public float piston, prevPiston;
    public boolean pistonDir;
    private float randSpeed = 0.1F;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();
    private final FluidTankNTM[] sending;

    public BlockEntityMachineCompressor(BlockPos pos, BlockState state) {
        this(ModBlockEntities.COMPRESSOR.get(), pos, state);
    }

    protected BlockEntityMachineCompressor(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
        tanks[0] = new FluidTankNTM(16_000);
        tanks[1] = new FluidTankNTM(16_000).withPressure(1);
        sending = new FluidTankNTM[] {tanks[1]};
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCompressor");
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, maxPower - power, false);
        tanks[0].setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        setupTanks();

        upgradeManager.scan(this, SLOT_UPGRADE_START, SLOT_UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        CompressorRecipe rec =
                CompressorRecipes.INSTANCE.get(tanks[0].getTankType(), tanks[0].getPressure());
        int timeBase = rec != null ? rec.duration : processTimeBase;

        if (rec == null)
            this.processTime =
                    speedLevel == 3 ? 10 : speedLevel == 2 ? 20 : speedLevel == 1 ? 60 : timeBase;
        else this.processTime = timeBase / (speedLevel + 1);
        this.powerRequirement = powerRequirementBase / (powerLevel + 1);
        this.processTime = this.processTime / (overLevel + 1);
        this.powerRequirement = this.powerRequirement * ((overLevel * 2) + 1);

        if (processTime <= 0) processTime = 1;

        if (canProcess()) {
            progress++;
            isOn = true;
            power -= powerRequirement;

            if (progress >= processTime) {
                progress = 0;
                process();
                setChanged();
            }
        } else {
            progress = 0;
            isOn = false;
        }

        flush.provide((ServerLevel) level, this);

        networkPackNT(100);
    }

    @Override
    public void tickClient() {
        prevFanSpin = fanSpin;
        prevPiston = piston;

        if (!isOn) return;

        fanSpin += 15;
        if (fanSpin >= 360) {
            prevFanSpin -= 360;
            fanSpin -= 360;
        }

        if (pistonDir) {
            piston -= randSpeed;
            if (piston <= 0) {

                level.playSound(
                        null,
                        worldPosition,
                        ModSounds.COMPRESSOR_PISTON.get(),
                        SoundSource.BLOCKS,
                        getVolume(0.5F),
                        0.75F);
                pistonDir = false;
            }
        } else {
            piston += 0.05F;
            if (piston >= 1) {
                randSpeed = 0.085F + level.getRandom().nextFloat() * 0.03F;
                pistonDir = true;
            }
        }
        piston = Mth.clamp(piston, 0F, 1F);
    }

    public boolean canProcess() {
        if (power <= powerRequirement) return false;

        CompressorRecipe recipe =
                CompressorRecipes.INSTANCE.get(tanks[0].getTankType(), tanks[0].getPressure());
        if (recipe == null) {
            return tanks[0].getFill() >= 1_000
                    && tanks[1].getFill() + 1_000 <= tanks[1].getMaxFill()
                    && tanks[1].accepts(tanks[0].getFluid());
        }
        return tanks[0].getFill() >= recipe.inputFluid[0].amount()
                && tanks[1].getFill() + recipe.outputFluid[0].amount() <= tanks[1].getMaxFill();
    }

    private void process() {
        CompressorRecipe recipe =
                CompressorRecipes.INSTANCE.get(tanks[0].getTankType(), tanks[0].getPressure());
        if (recipe == null) {
            Fluid content = tanks[0].getFluid();
            tanks[0].setFill(tanks[0].getFill() - 1_000);
            tanks[1].receive(content, 1_000);
        } else {
            tanks[0].setFill(tanks[0].getFill() - (int) recipe.inputFluid[0].amount());
            tanks[1].setFill(tanks[1].getFill() + (int) recipe.outputFluid[0].amount());
        }
    }

    private void setupTanks() {
        CompressorRecipe recipe =
                CompressorRecipes.INSTANCE.get(tanks[0].getTankType(), tanks[0].getPressure());
        if (recipe == null) {
            tanks[1].withPressure(tanks[0].getPressure() + 1).setTankType(tanks[0].getTankType());
        } else {
            tanks[1].withPressure(recipe.outputFluid[0].pressure())
                    .setTankType(recipe.outputFluid[0].type());
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (!data.contains("compression")) return;
        setCompression(data.getIntOr("compression", 0));
    }

    private void setCompression(int compression) {
        if (compression == tanks[0].getPressure()) return;
        tanks[0].withPressure(compression);
        setupTanks();
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public @Nullable FluidTankNTM getTankToPaste() {
        return tanks[0];
    }

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag tag = IFluidCopiable.super.getSettings(level, pos);
        tag.putInt("compression", tanks[0].getPressure());
        return tag;
    }

    @Override
    public void pasteSettings(
            CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        if (nbt.contains("compression")) setCompression(nbt.getIntOr("compression", 0));
        IFluidCopiable.super.pasteSettings(nbt, index, level, player, pos);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, maxPower));
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    public int[] getReceivingPressureRange(Fluid type) {
        int p = tanks[0].getPressure();
        return new int[] {p, p};
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != tanks[0].getPressure()) return 0L;
        if (!tanks[0].accepts(type)) return 0L;
        return (long) tanks[0].getMaxFill() - tanks[0].getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tanks[0].getPressure()) return amount;
        int accepted = tanks[0].fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public int[] getProvidingPressureRange(Fluid type) {
        int p = tanks[1].getPressure();
        return new int[] {p, p};
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public long getFluidAvailable(Fluid type, int pressure) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return 0L;
        return tanks[1].getFill();
    }

    @Override
    public void useUpFluid(Fluid type, int pressure, long amount) {
        if (!tanks[1].provides(type) || tanks[1].getPressure() != pressure) return;
        if (tanks[1].drain((int) Math.min(amount, Integer.MAX_VALUE), true) > 0) setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_UPGRADE_START, SLOT_UPGRADE_END -> ItemMachineUpgrade.isUpgrade(stack);
            default -> false;
        };
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
        return new MenuMachineCompressor(containerId, playerInventory, this);
    }

    private void writeTanks(ByteBuf output) {
        tanks[0].packetSerialize(output);
        tanks[1].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        tanks[0].packetDeserialize(input);
        tanks[1].packetDeserialize(input);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("progress").ifPresent(v -> progress = v);
        input.child("0").ifPresent(tanks[0]::deserialize);
        input.child("1").ifPresent(tanks[1]::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        tanks[0].serialize(output.child("0"));
        tanks[1].serialize(output.child("1"));
    }

    @Override
    protected boolean syncMuffled() {
        return true;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeInt(this.progress);
            case 1 -> output.writeInt(this.processTime);
            case 2 -> output.writeInt(this.powerRequirement);
            case 3 -> output.writeLong(this.power);
            case 4 -> writeTanks(output);
            case 5 -> output.writeBoolean(this.isOn);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.progress = input.readInt();
            case 1 -> this.processTime = input.readInt();
            case 2 -> this.powerRequirement = input.readInt();
            case 3 -> this.power = input.readLong();
            case 4 -> readTanks(input);
            case 5 -> this.isOn = input.readBoolean();
            default -> super.readSyncUnit(unit, input);
        }
    }
}
