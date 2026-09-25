// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.MenuMachineWoodBurner;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumAshType;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.BlockEntityMachineBase;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineWoodBurner extends BlockEntityMachineBase
        implements IEnergyHandlerMK2,
                IFluidHandlerMK2,
                MenuProvider,
                IControlReceiver,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_FUEL = 0;
    public static final int SLOT_ASH = 1;
    public static final int SLOT_FLUID_ID = 2;
    public static final int SLOT_FLUID_IN = 3;
    public static final int SLOT_FLUID_OUT = 4;
    public static final int SLOT_BATTERY = 5;
    public static final int SLOT_COUNT = 6;

    public static final long MAX_POWER = 100_000L;
    public static final int TANK_CAPACITY = 16_000;
    public static final int ASH_THRESHOLD = 2_000;

    public static final ModuleBurnTime BURN_MODULE =
            new ModuleBurnTime().setLogTimeMod(4).setWoodTimeMod(2);
    private static final int[] ACCESSIBLE_SLOTS = {SLOT_FUEL, SLOT_ASH};

    @SyncField(units = 1L << 6)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.WOODOIL, TANK_CAPACITY);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public int burnTime;

    @SyncField(units = 1L << 2)
    public int maxBurnTime;

    @SyncField(units = 1L << 4)
    public boolean isOn = false;

    @SyncField(units = 1L << 5)
    public boolean liquidBurn = false;

    @SyncField(units = 1L << 3)
    public int powerGen;

    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;

    public BlockEntityMachineWoodBurner(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_BURNER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public void tickServer() {
        long prevPower = power;
        powerGen = 0;

        boolean changed = tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, inventory);
        if (changed) setChanged();
        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);

        if (!liquidBurn) {
            if (burnTime <= 0) {
                ItemStack fuel = inventory.get(SLOT_FUEL);
                int burn = BURN_MODULE.getBurnTime(level, fuel);
                if (burn > 0) {
                    EnumAshType type = BlockEntityFireboxBase.getAshFromFuel(fuel);
                    if (type == EnumAshType.WOOD) ashLevelWood += burn;
                    else if (type == EnumAshType.COAL) ashLevelCoal += burn;
                    else ashLevelMisc += burn;
                    while (ashLevelWood >= ASH_THRESHOLD && processAsh(EnumAshType.WOOD))
                        ashLevelWood -= ASH_THRESHOLD;
                    while (ashLevelCoal >= ASH_THRESHOLD && processAsh(EnumAshType.COAL))
                        ashLevelCoal -= ASH_THRESHOLD;
                    while (ashLevelMisc >= ASH_THRESHOLD && processAsh(EnumAshType.MISC))
                        ashLevelMisc -= ASH_THRESHOLD;
                    maxBurnTime = burnTime = burn;
                    ItemStackTemplate remainder = fuel.getCraftingRemainder();
                    fuel.shrink(1);
                    if (fuel.isEmpty())
                        inventory.set(
                                SLOT_FUEL,
                                remainder != null ? remainder.create() : ItemStack.EMPTY);
                    setChanged();
                }
            } else if (power < MAX_POWER && isOn) {
                burnTime--;
                powerGen += 100;
                if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                    PollutionHandler.incrementPollution(
                            level,
                            worldPosition,
                            PollutionType.SOOT,
                            PollutionHandler.SOOT_PER_SECOND);
                }
            }
        } else if (power < MAX_POWER && tank.getFill() > 0 && isOn) {
            FT_Flammable trait =
                    NTMFluidProperties.getTrait(tank.getTankType(), FT_Flammable.class);
            if (trait != null) {
                int toBurn = Math.min(tank.getFill(), 2);
                if (toBurn > 0) {
                    powerGen += (int) (trait.getHeatEnergy() * toBurn / 2_000L);
                    tank.setFill(tank.getFill() - toBurn);
                    if (TickPhase.every(this, SharedConstants.TICKS_PER_SECOND)) {
                        PollutionHandler.incrementPollution(
                                level,
                                worldPosition,
                                PollutionType.SOOT,
                                PollutionHandler.SOOT_PER_SECOND * toBurn / 2F);
                    }
                }
            }
        }

        power = Math.min(MAX_POWER, power + powerGen);
        if (power != prevPower) setChanged();
        networkPackNT(25);
    }

    @Override
    public void tickClient() {
        if (powerGen <= 0) return;
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();

        level.addParticle(
                ParticleTypes.SMOKE,
                worldPosition.getX() + 0.5 - dir.getStepX() + rot.getStepX(),
                worldPosition.getY() + 4,
                worldPosition.getZ() + 0.5 - dir.getStepZ() + rot.getStepZ(),
                0,
                0.05,
                0);
    }

    private Direction coreFacing() {
        return BlockMultiblockCore.coreFacing(getBlockState());
    }

    private boolean processAsh(EnumAshType type) {
        ItemStack out = inventory.get(SLOT_ASH);
        if (out.isEmpty()) {
            inventory.set(SLOT_ASH, ModItems.POWDER_ASH.stack(type));
            return true;
        }
        if (ModItems.POWDER_ASH.is(out, type) && out.getCount() < out.getMaxStackSize()) {
            out.grow(1);
            return true;
        }
        return false;
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
    public long getDemand(Fluid type, int pressure) {
        if (pressure != tank.getPressure()) return 0L;
        if (!tank.accepts(type)) return 0L;
        return (long) tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != tank.getPressure()) return amount;
        int accepted = tank.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) setChanged();
        return amount - accepted;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            isOn = !isOn;
            setChanged();
        }
        if (data.contains("switch")) {
            liquidBurn = !liquidBurn;
            setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FUEL -> level != null && BURN_MODULE.getBurnTime(level, stack) > 0;
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_FLUID_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == SLOT_FUEL && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == SLOT_ASH;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineWoodBurner");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineWoodBurner(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        input.getLong("power").ifPresent(v -> power = v);
        input.getInt("burnTime").ifPresent(v -> burnTime = v);
        input.getInt("maxBurnTime").ifPresent(v -> maxBurnTime = v);
        input.getInt("ashWood").ifPresent(v -> ashLevelWood = v);
        input.getInt("ashCoal").ifPresent(v -> ashLevelCoal = v);
        input.getInt("ashMisc").ifPresent(v -> ashLevelMisc = v);

        isOn = input.getBooleanOr("isOn", isOn);
        liquidBurn = input.getBooleanOr("liquidBurn", liquidBurn);
        input.child("tank").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("burnTime", burnTime);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putInt("ashWood", ashLevelWood);
        output.putInt("ashCoal", ashLevelCoal);
        output.putInt("ashMisc", ashLevelMisc);
        output.putBoolean("isOn", isOn);
        output.putBoolean("liquidBurn", liquidBurn);
        tank.serialize(output.child("tank"));
    }

    @Override
    public long syncUnitMask() {
        return 0x7fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> output.writeInt(this.burnTime);
            case 2 -> output.writeInt(this.maxBurnTime);
            case 3 -> output.writeInt(this.powerGen);
            case 4 -> output.writeBoolean(this.isOn);
            case 5 -> output.writeBoolean(this.liquidBurn);
            case 6 -> this.tank.packetSerialize(output);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> this.burnTime = input.readInt();
            case 2 -> this.maxBurnTime = input.readInt();
            case 3 -> this.powerGen = input.readInt();
            case 4 -> this.isOn = input.readBoolean();
            case 5 -> this.liquidBurn = input.readBoolean();
            case 6 -> this.tank.packetDeserialize(input);
            default -> throw new IllegalArgumentException();
        }
    }
}
